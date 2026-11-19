package org.bharatai.ps5.metalinspection

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NeuClassifierDeviceTest {
    @Test
    fun heldOutNeuImagesRunThroughTheInstalledClassifier() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appContext = instrumentation.targetContext
        val assets = instrumentation.context.assets
        val labels = listOf("crazing", "inclusion", "patches", "pitted_surface", "rolled-in_scale", "scratches")
        val confusion = Array(labels.size) { IntArray(labels.size) }
        val durationsMs = mutableListOf<Long>()
        var correct = 0
        var count = 0

        assets.open("neu_test_manifest.csv").bufferedReader().useLines { lines ->
            lines.drop(1).forEach { line ->
                val (filename, expected) = line.split(',', limit = 2)
                val bitmap = assets.open("neu_test/$filename").use { BitmapFactory.decodeStream(it) }
                    ?: error("Could not decode $filename")
                try {
                    val start = SystemClock.elapsedRealtime()
                    val predicted = OpenCvDefectClassifier.predict(appContext, bitmap)
                    durationsMs += SystemClock.elapsedRealtime() - start
                    val truthId = labels.indexOf(expected)
                    val predictedId = labels.indexOf(predicted)
                    assertTrue("Unexpected classifier label: $predicted", predictedId >= 0)
                    confusion[truthId][predictedId]++
                    if (predicted == expected) correct++
                    count++
                } finally {
                    bitmap.recycle()
                }
            }
        }

        val sorted = durationsMs.sorted()
        val medianMs = sorted[sorted.size / 2]
        val summary = "count=$count correct=$correct accuracy=${correct.toDouble() / count} medianMs=$medianMs confusion=${confusion.joinToString { it.joinToString(prefix = "[", postfix = "]") }}"
        Log.i("NeuClassifierDeviceTest", summary)
        instrumentation.sendStatus(2, Bundle().apply { putString("NEU_DEVICE_RESULT", summary) })
        println("NEU_DEVICE_RESULT: $summary")
        assertEquals("Expected the complete held-out NEU split", 270, count)
        assertEquals("Expected the previously measured held-out NEU result", 226, correct)
    }
}
