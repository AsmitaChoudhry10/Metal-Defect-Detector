package org.bharatai.ps5.metalinspection

import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KolektorBinaryDetectorDeviceTest {
    @Test
    fun normalAndDefectExamplesUseTheExpectedOnDeviceThreshold() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appContext = instrumentation.targetContext
        val testAssets = instrumentation.context.assets
        val normal = testAssets.open("ksdd_normal.png").use { BitmapFactory.decodeStream(it) }
            ?: error("Normal test sample failed to decode")
        val defective = testAssets.open("ksdd_defect.png").use { BitmapFactory.decodeStream(it) }
            ?: error("Defect test sample failed to decode")
        try {
            val normalStart = SystemClock.elapsedRealtime()
            val normalScore = KolektorBinaryDetector.defectScore(appContext, normal)
            val normalMs = SystemClock.elapsedRealtime() - normalStart

            val defectStart = SystemClock.elapsedRealtime()
            val defectScore = KolektorBinaryDetector.defectScore(appContext, defective)
            val defectMs = SystemClock.elapsedRealtime() - defectStart

            Log.i("KolektorBinaryDeviceTest", "normal score=$normalScore timeMs=$normalMs")
            Log.i("KolektorBinaryDeviceTest", "defect score=$defectScore timeMs=$defectMs")
            assertTrue("Labeled normal example should score below the dataset-specific threshold", normalScore < KolektorBinaryDetector.defectThreshold)
            assertTrue("Labeled defect example should score above the dataset-specific threshold", defectScore >= KolektorBinaryDetector.defectThreshold)
        } finally {
            normal.recycle()
            defective.recycle()
        }
    }
}
