package org.bharatai.ps5.metalinspection

import android.content.Context
import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.ml.SVM
import org.opencv.objdetect.HOGDescriptor
import java.io.File
import java.util.ArrayList

data class MorphologicalMetrics(
    val defectAreaPercent: Float,
    val contourCount: Int,
    val calculatedSeverity: String,
)

data class DefectAnalysisResult(
    val label: String,
    val morphology: MorphologicalMetrics,
)

internal object OpenCvDefectClassifier {
    private val labels = listOf("crazing", "inclusion", "patches", "pitted_surface", "rolled-in_scale", "scratches")
    private var cachedSvm: SVM? = null

    @Synchronized
    fun predict(context: Context, bitmap: Bitmap): String {
        return analyzeRawClass(context, bitmap)
    }

    private fun loadSvm(context: Context): SVM = cachedSvm ?: run {
        // Refresh this cache on first use each process so an APK model update cannot
        // silently keep using the old app-private cached XML.
        val modelFile = File(context.cacheDir, "neu_hog_svm.xml")
        val stagedFile = File(context.cacheDir, "neu_hog_svm.xml.tmp")
        context.assets.open("neu_hog_svm.xml").use { input ->
            stagedFile.outputStream().use(input::copyTo)
        }
        if (modelFile.exists()) check(modelFile.delete()) { "Could not replace cached NEU model" }
        check(stagedFile.renameTo(modelFile)) { "Could not install refreshed NEU model cache" }
        SVM.load(modelFile.absolutePath).also { cachedSvm = it }
    }

    private fun analyzeRawClass(context: Context, bitmap: Bitmap): String {
        check(OpenCVLoader.initLocal()) { "OpenCV could not be loaded" }
        val svm = loadSvm(context)
        val rgba = Mat(); val gray = Mat(); val resized = Mat(); val descriptors = MatOfFloat()
        try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.resize(gray, resized, Size(200.0, 200.0))
            val hog = HOGDescriptor(Size(200.0, 200.0), Size(40.0, 40.0), Size(20.0, 20.0), Size(20.0, 20.0), 9)
            hog.compute(resized, descriptors)
            val sample = descriptors.reshape(1, 1)
            val id = try { svm.predict(sample).toInt() } finally { sample.release() }
            return labels.getOrElse(id) { "unknown" }
        } finally {
            rgba.release(); gray.release(); resized.release(); descriptors.release()
        }
    }

    @Synchronized
    fun analyze(context: Context, bitmap: Bitmap): DefectAnalysisResult {
        check(OpenCVLoader.initLocal()) { "OpenCV could not be loaded" }
        val svm = loadSvm(context)
        val rgba = Mat(); val rgb = Mat(); val hsv = Mat(); val gray = Mat(); val resized = Mat()
        val resizedColor = Mat(); val descriptors = MatOfFloat()
        val blurred = Mat(); val thresh = Mat(); val hierarchy = Mat()
        val contours = ArrayList<MatOfPoint>()
        val extraMats = ArrayList<Mat>()
        fun track(m: Mat): Mat = m.also { extraMats.add(it) }
        try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.resize(gray, resized, Size(200.0, 200.0))

            val hog = HOGDescriptor(Size(200.0, 200.0), Size(40.0, 40.0), Size(20.0, 20.0), Size(20.0, 20.0), 9)
            hog.compute(resized, descriptors)
            val sample = descriptors.reshape(1, 1)
            val id = try { svm.predict(sample).toInt() } finally { sample.release() }
            val label = labels.getOrElse(id) { "unknown" }

            Imgproc.GaussianBlur(resized, blurred, Size(5.0, 5.0), 0.0)

            // Detect isolated 3D part on a bright/uniform background (e.g. washer, nut, bolt)
            // so silhouette edges, cast shadows, and normal threads are not misread as defects.
            val grayBytes = ByteArray(200 * 200)
            resized.get(0, 0, grayBytes)
            var borderBright = 0
            for (x in 0 until 200) {
                if ((grayBytes[x].toInt() and 0xFF) > 215) borderBright++
                if ((grayBytes[199 * 200 + x].toInt() and 0xFF) > 215) borderBright++
            }
            for (y in 0 until 200) {
                if ((grayBytes[y * 200].toInt() and 0xFF) > 215) borderBright++
                if ((grayBytes[y * 200 + 199].toInt() and 0xFF) > 215) borderBright++
            }
            var overallBright = 0
            for (b in grayBytes) {
                if ((b.toInt() and 0xFF) > 230) overallBright++
            }
            val isIsolatedPart = (borderBright / 800f >= 0.65f) || (overallBright / 40000f >= 0.30f)

            var areaPct = 0f
            var contourCount = 0
            var usedIsolatedMetrology = false

            if (isIsolatedPart) {
                val fg = track(Mat())
                Imgproc.threshold(resized, fg, 224.0, 255.0, Imgproc.THRESH_BINARY_INV)
                val erodeKernel = track(Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(11.0, 11.0)))
                val fgInner = track(Mat())
                Imgproc.erode(fg, fgInner, erodeKernel)
                val fgPixels = Core.countNonZero(fgInner)

                if (fgPixels >= 400) {
                    usedIsolatedMetrology = true

                    // 1) Localized surface scratches / pits on metallic faces (black-hat on bright/mid-tone metal)
                    val morphKernel = track(Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(7.0, 7.0)))
                    val closed = track(Mat())
                    Imgproc.morphologyEx(blurred, closed, Imgproc.MORPH_CLOSE, morphKernel)
                    val blackhat = track(Mat())
                    Core.subtract(closed, blurred, blackhat)

                    val bhStrong = track(Mat())
                    Imgproc.threshold(blackhat, bhStrong, 45.0, 255.0, Imgproc.THRESH_BINARY)
                    val metalFace = track(Mat())
                    Imgproc.threshold(closed, metalFace, 84.0, 255.0, Imgproc.THRESH_BINARY)
                    val surfMask = track(Mat())
                    Core.bitwise_and(bhStrong, metalFace, surfMask)
                    Core.bitwise_and(surfMask, fgInner, surfMask)
                    val surfPixels = Core.countNonZero(surfMask)
                    val surfPct = (surfPixels.toFloat() / fgPixels.toFloat()) * 100f

                    // 2) Oxidation / rust / heavy warm discoloration inside part interior
                    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
                    Imgproc.resize(rgb, resizedColor, Size(200.0, 200.0))
                    Imgproc.cvtColor(resizedColor, hsv, Imgproc.COLOR_RGB2HSV)
                    val hsvChannels = ArrayList<Mat>(3)
                    Core.split(hsv, hsvChannels)
                    hsvChannels.forEach { track(it) }
                    val satMask = track(Mat())
                    Imgproc.threshold(hsvChannels[1], satMask, 70.0, 255.0, Imgproc.THRESH_BINARY)

                    val rgbChannels = ArrayList<Mat>(3)
                    Core.split(resizedColor, rgbChannels)
                    rgbChannels.forEach { track(it) }
                    val r16 = track(Mat())
                    val b16 = track(Mat())
                    rgbChannels[0].convertTo(r16, CvType.CV_16S)
                    rgbChannels[2].convertTo(b16, CvType.CV_16S)
                    val rbDiff = track(Mat())
                    Core.subtract(r16, b16, rbDiff)
                    val rbMask = track(Mat())
                    Core.compare(rbDiff, Scalar(24.0), rbMask, Core.CMP_GT)

                    val rustMask = track(Mat())
                    Core.bitwise_and(satMask, rbMask, rustMask)
                    Core.bitwise_and(rustMask, fgInner, rustMask)
                    val rustPixels = Core.countNonZero(rustMask)
                    val rustPct = (rustPixels.toFloat() / fgPixels.toFloat()) * 100f

                    val combinedMask = track(Mat())
                    Core.bitwise_or(surfMask, rustMask, combinedMask)
                    Imgproc.findContours(combinedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
                    contourCount = contours.size
                    areaPct = maxOf(surfPct, rustPct)
                }
            }

            if (!usedIsolatedMetrology) {
                // Full-frame surface metrology (e.g. NEU-DET steel strip patches)
                Imgproc.adaptiveThreshold(blurred, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 21, 5.0)
                val defectPixels = Core.countNonZero(thresh)
                areaPct = (defectPixels.toFloat() / 40000f) * 100f
                Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
                contourCount = contours.size
            }

            val calcSeverity = when {
                areaPct < 1.0f -> "Nominal (< 1.0% area)"
                areaPct < 2.5f -> "Low (< 2.5% area)"
                areaPct < 8.0f -> "Medium (2.5% - 8.0% area)"
                else -> "High (> 8.0% area)"
            }
            val finalLabel = if (areaPct < 1.0f) "clean_surface" else label
            val morphology = MorphologicalMetrics(
                defectAreaPercent = areaPct,
                contourCount = contourCount,
                calculatedSeverity = calcSeverity,
            )
            return DefectAnalysisResult(label = finalLabel, morphology = morphology)
        } finally {
            for (c in contours) c.release()
            for (m in extraMats) m.release()
            rgba.release(); rgb.release(); hsv.release(); gray.release(); resized.release()
            resizedColor.release(); descriptors.release()
            blurred.release(); thresh.release(); hierarchy.release()
        }
    }
}
