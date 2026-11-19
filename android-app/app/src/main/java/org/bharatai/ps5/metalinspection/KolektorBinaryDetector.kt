package org.bharatai.ps5.metalinspection

import android.content.Context
import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import java.io.File
import kotlin.math.exp

/** Experimental binary detector trained only on the KolektorSDD2 product domain. */
internal object KolektorBinaryDetector {
    const val defectThreshold = 0.34295253f // selected on validation using the OpenCV preprocessing path
    private const val modelAsset = "kolektor_binary_mobilenet.onnx"
    private const val expectedModelBytes = 6_090_378L

    fun defectScore(context: Context, bitmap: Bitmap): Float {
        check(OpenCVLoader.initLocal()) { "OpenCV could not be loaded" }
        val modelFile = File(context.cacheDir, modelAsset)
        if (!modelFile.exists() || modelFile.length() != expectedModelBytes) {
            context.assets.open(modelAsset).use { input -> modelFile.outputStream().use(input::copyTo) }
        }
        check(modelFile.length() == expectedModelBytes) { "Binary model asset is incomplete" }

        val net: Net = Dnn.readNetFromONNX(modelFile.absolutePath)
        check(!net.empty()) { "OpenCV could not load the experimental binary model" }
        net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV)
        net.setPreferableTarget(Dnn.DNN_TARGET_CPU)

        val rgba = Mat()
        val gray = Mat()
        val rgb = Mat()
        val resized = Mat()
        try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(gray, rgb, Imgproc.COLOR_GRAY2RGB)
            Imgproc.resize(rgb, resized, Size(512.0, 192.0), 0.0, 0.0, Imgproc.INTER_LINEAR)
            val inputBlob = Dnn.blobFromImage(
                resized,
                1.0 / 255.0,
                Size(512.0, 192.0),
                Scalar(0.0, 0.0, 0.0),
                false,
                false,
            )
            try {
                net.setInput(inputBlob)
                val output = net.forward()
                try {
                    val logit = output.get(0, 0)?.firstOrNull()?.toFloat()
                        ?: error("Binary model returned no score")
                    return (1.0 / (1.0 + exp(-logit.toDouble()))).toFloat()
                } finally {
                    output.release()
                }
            } finally {
                inputBlob.release()
            }
        } finally {
            rgba.release()
            gray.release()
            rgb.release()
            resized.release()
        }
    }
}
