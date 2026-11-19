package org.bharatai.ps5.metalinspection

import android.content.Context
import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import java.io.File

internal data class SyntheticFittingPrediction(
    val label: String,
    val classScore: Float,
    val severity: String?,
    val severityScore: Float?,
)

/** AI-generated synthetic fitting demo. It is not a validated production inspector. */
internal object SyntheticFittingDetector {
    private const val modelAsset = "synthetic_fitting_mobilenet.onnx"
    private const val expectedModelBytes = 6_127_011L
    private val classLabels = listOf(
        "good",
        "crack",
        "scratch",
        "corrosion",
        "deformation",
        "unrelated object",
        "unusable photo",
    )
    private val severityLabels = listOf("Low", "Medium", "High")
    private val mean = doubleArrayOf(0.485, 0.456, 0.406)
    private val inverseStd = doubleArrayOf(1.0 / 0.229, 1.0 / 0.224, 1.0 / 0.225)
    private var cachedNet: Net? = null

    @Synchronized
    fun predict(context: Context, bitmap: Bitmap): SyntheticFittingPrediction {
        check(OpenCVLoader.initLocal()) { "OpenCV could not be loaded" }
        val net = cachedNet ?: loadNet(context).also { cachedNet = it }
        val rgba = Mat()
        val rgb = Mat()
        val resized = Mat()
        val normalized = Mat()
        val channels = ArrayList<Mat>(3)
        try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
            Imgproc.resize(rgb, resized, Size(224.0, 224.0), 0.0, 0.0, Imgproc.INTER_LINEAR)
            resized.convertTo(normalized, CvType.CV_32FC3, 1.0 / 255.0)
            Core.split(normalized, channels)
            for (index in 0..2) {
                Core.subtract(channels[index], Scalar(mean[index]), channels[index])
                Core.multiply(channels[index], Scalar(inverseStd[index]), channels[index])
            }
            Core.merge(channels, normalized)
            val input = Dnn.blobFromImage(
                normalized,
                1.0,
                Size(224.0, 224.0),
                Scalar(0.0, 0.0, 0.0),
                false,
                false,
            )
            try {
                net.setInput(input)
                val output = net.forward()
                try {
                    check(output.total() == 10L) { "Synthetic model returned an unexpected output shape" }
                    val logits = FloatArray(10) { index ->
                        output.get(0, index)?.firstOrNull()?.toFloat()
                            ?: error("Synthetic model returned no score")
                    }
                    val classPick = bestSoftmax(logits.copyOfRange(0, 7))
                    val label = classLabels[classPick.first]
                    val severityPick = if (label in setOf("crack", "scratch", "corrosion", "deformation")) {
                        ordinalSeverity(logits.copyOfRange(7, 10))
                    } else {
                        null
                    }
                    return SyntheticFittingPrediction(
                        label = label,
                        classScore = classPick.second,
                        severity = severityPick?.let { severityLabels[it.first] },
                        severityScore = severityPick?.second,
                    )
                } finally {
                    output.release()
                }
            } finally {
                input.release()
            }
        } finally {
            channels.forEach(Mat::release)
            rgba.release()
            rgb.release()
            resized.release()
            normalized.release()
        }
    }

    private fun loadNet(context: Context): Net {
        val modelFile = File(context.cacheDir, modelAsset)
        val stagedFile = File(context.cacheDir, "$modelAsset.tmp")
        context.assets.open(modelAsset).use { input ->
            stagedFile.outputStream().use(input::copyTo)
        }
        check(stagedFile.length() == expectedModelBytes) { "Synthetic model asset is incomplete" }
        if (modelFile.exists()) check(modelFile.delete()) { "Could not refresh synthetic model cache" }
        check(stagedFile.renameTo(modelFile)) { "Could not install synthetic model cache" }
        return Dnn.readNetFromONNX(modelFile.absolutePath).also { net ->
            check(!net.empty()) { "OpenCV could not load the synthetic fitting model" }
            net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV)
            net.setPreferableTarget(Dnn.DNN_TARGET_CPU)
        }
    }

    /** Decode ordered synthetic bands using the trained adjacent thresholds. */
    private fun ordinalSeverity(logits: FloatArray): Pair<Int, Float> {
        check(logits.size == 3) { "Expected three ordinal severity scores" }
        val lowThreshold = logits[1] - logits[0]
        val highThreshold = logits[2] - logits[1]
        val index = when {
            lowThreshold < 0f -> 0
            highThreshold < 0f -> 1
            else -> 2
        }
        fun sigmoid(x: Float): Float = (1.0 / (1.0 + kotlin.math.exp(-x.toDouble()))).toFloat()
        val aboveLow = sigmoid(lowThreshold)
        val aboveMedium = sigmoid(highThreshold)
        val probabilities = floatArrayOf(1f - aboveLow, aboveLow * (1f - aboveMedium), aboveLow * aboveMedium)
        return index to probabilities[index]
    }

    private fun bestSoftmax(logits: FloatArray): Pair<Int, Float> {
        val max = logits.maxOrNull() ?: error("Model returned an empty output")
        val exp = FloatArray(logits.size) { kotlin.math.exp((logits[it] - max).toDouble()).toFloat() }
        val total = exp.sum().coerceAtLeast(1e-12f)
        val index = exp.indices.maxBy { exp[it] }
        return index to (exp[index] / total)
    }
}
