package org.bharatai.ps5.metalinspection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeverityRubricTest {
    @Test
    fun eachNeuClassHasThreeClearOperatorCues() {
        listOf("crazing", "inclusion", "patches", "pitted_surface", "rolled-in_scale", "scratches")
            .forEach { label ->
                val rubric = severityRubricFor(label)
                assertTrue(rubric.low.isNotBlank())
                assertTrue(rubric.medium.isNotBlank())
                assertTrue(rubric.high.isNotBlank())
            }
    }

    @Test
    fun operatorSeverityIsExplicitlyRecordedAsNotAiPredicted() {
        assertEquals("Operator visual tier: High (team-defined rubric)", severityHistoryValue("High"))
    }
}
