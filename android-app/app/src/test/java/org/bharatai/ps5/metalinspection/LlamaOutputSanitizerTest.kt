package org.bharatai.ps5.metalinspection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlamaOutputSanitizerTest {
    @Test
    fun llamaPromptStaysCompactForThe128TokenContextModel() {
        val prompt = buildLlamaPrompt(
            "crack",
            "Retake a sharply focused close-up under diffuse lighting. Compare crack-like patterns with the accepted reference.",
        )
        assertTrue("prompt was ${prompt.length} characters", prompt.length < 500)
        assertTrue(prompt.contains("crack"))
    }

    @Test
    fun removesExecuTorchChatMarkersFromVisibleReply() {
        assertEquals(
            "Inspect the crack-like pattern against the accepted reference.",
            cleanLlamaOutput("<|assistant|> Inspect the crack-like pattern against the accepted reference. <|eot_id|>"),
        )
    }

    @Test
    fun rejectsGenericReplyThatDoesNotUseChecklistDetails() {
        val checklist = "Retake a sharply focused close-up under diffuse lighting. Compare crack-like patterns with the accepted reference."
        assertTrue(isUnsupportedLlamaOutput("Inspectors should perform the checks and verify the results.", checklist))
    }

    @Test
    fun acceptsSpecificReplyGroundedInChecklist() {
        val checklist = "Retake a sharply focused close-up under diffuse lighting. Compare crack-like patterns with the accepted reference."
        assertFalse(isUnsupportedLlamaOutput("Compare the crack-like pattern with the accepted reference.", checklist))
    }
}
