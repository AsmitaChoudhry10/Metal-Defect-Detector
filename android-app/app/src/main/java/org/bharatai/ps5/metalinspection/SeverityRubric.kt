package org.bharatai.ps5.metalinspection

internal data class SeverityRubric(
    val low: String,
    val medium: String,
    val high: String,
)

/** Team-defined visual triage cues. This is not a trained severity model or an acceptance standard. */
internal fun severityRubricFor(label: String): SeverityRubric = when (
    label.lowercase().replace(' ', '_').replace('-', '_')
) {
    "crazing", "crack", "cracks" -> SeverityRubric(
        low = "Short, faint surface line; no visible opening; away from a critical edge or joint.",
        medium = "Clear continuous line, especially near an edge, transition, thread, or joint.",
        high = "Wide/open line, branching pattern, or line crossing a functional or load-bearing area.",
    )
    "scratches", "scratch" -> SeverityRubric(
        low = "Small isolated mark away from a sealing or contact surface.",
        medium = "Clearly visible groove or mark near a thread, fit, or contact surface.",
        high = "Extensive gouge or mark crossing a sealing, thread, or functional surface.",
    )
    "pitted_surface", "corrosion", "corroded" -> SeverityRubric(
        low = "Small localized discoloration; no obvious flaking or broad rough area.",
        medium = "Several visible rough or pitted spots, or a clearly affected local patch.",
        high = "Widespread flaking, clustered deep-looking pits, or affected functional surfaces.",
    )
    "patches", "rolled_in_scale", "inclusion" -> SeverityRubric(
        low = "Small isolated visible region, away from a functional edge or contact area.",
        medium = "Clearly visible region across part of the surface or near a functional area.",
        high = "Large, multiple, or functionally located regions that need prompt inspector review.",
    )
    "deformation", "deformed" -> SeverityRubric(
        low = "Small local contour change, away from a mating or sealing surface.",
        medium = "Visible contour change near a thread, fit, edge, or contact surface.",
        high = "Major visible shape change at a bore, mating, sealing, or load-bearing area.",
    )
    else -> SeverityRubric(
        low = "Small and localized; no functional area appears involved.",
        medium = "Clearly visible or near a functional area.",
        high = "Widespread, open, or apparently on a critical functional area.",
    )
}

internal fun severityHistoryValue(level: String): String = "Operator visual tier: $level (team-defined rubric)"

internal fun severityTierOf(raw: String): String {
    val upper = raw.uppercase()
    return when {
        upper.contains("HIGH") -> "HIGH"
        upper.contains("MED") -> "MEDIUM"
        upper.contains("LOW") -> "LOW"
        else -> "UNREVIEWED"
    }
}
