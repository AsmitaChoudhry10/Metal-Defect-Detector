package org.bharatai.ps5.metalinspection

import android.content.Context
import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.HOGDescriptor
import org.opencv.ml.SVM
import java.io.File

internal object OpenCvDefectClassifier {
    private val labels = listOf("crazing", "inclusion", "patches", "pitted_surface", "rolled-in_scale", "scratches")
    private var cachedSvm: SVM? = null

    @Synchronized
    fun predict(context: Context, bitmap: Bitmap): String {
        check(OpenCVLoader.initLocal()) { "OpenCV could not be loaded" }
        val svm = cachedSvm ?: run {
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
}
