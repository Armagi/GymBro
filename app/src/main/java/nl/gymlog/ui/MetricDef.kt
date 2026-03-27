package nl.gymlog.ui

import androidx.compose.ui.graphics.Color
import nl.gymlog.data.WorkoutSession
import nl.gymlog.ui.theme.*

data class MetricDef(
    val key: String,
    val label: String,
    val unit: String,
    val color: Color,
    val getValue: (WorkoutSession) -> Float?,
    val formatValue: (Float) -> String = { "%.0f".format(it) }
)

fun durationFormat(secs: Float): String {
    val s = secs.toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

val ALL_METRICS = listOf(
    MetricDef("calories", "Calorieën", "kcal", AccentCalories, { it.calories }),
    MetricDef("duration", "Duur", "", AccentDuration, { it.durationSeconds?.toFloat() }, ::durationFormat),
    MetricDef("distance", "Afstand", "km", AccentDistance, { it.distanceKm }, { "%.2f".format(it) }),
    MetricDef("avgPower", "Gem. vermogen", "watt", AccentPower, { it.avgPower }),
    MetricDef("avgSpeed", "Gem. snelheid", "spm", AccentSpeed, { it.avgSpeed }),
    MetricDef("avgHeartRate", "Gem. hartslag", "spm", AccentAvgHR, { it.avgHeartRate }),
    MetricDef("maxHeartRate", "Max. hartslag", "spm", AccentMaxHR, { it.maxHeartRate }),
    MetricDef("caloriesPerHour", "Kcal/uur", "kcal/h", AccentCalPerHour, { it.caloriesPerHour }),
    MetricDef("conditionPI", "Conditie", "PI", AccentCondition, { it.conditionPI }),
    MetricDef("moves", "MOVEs", "", AccentMoves, { it.moves })
)
