package nl.gymlog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.gymlog.data.WorkoutSession
import nl.gymlog.ui.MetricDef
import nl.gymlog.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ─── Section header (ALL CAPS, Whoop/Apple Health style) ─────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(horizontal = 0.dp, vertical = 8.dp)
    )
}

// ─── Hero card (large metric display, colored top border) ────────────────────

@Composable
fun HeroCard(
    metric: MetricDef,
    value: Float?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column {
            // Colored top accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(metric.color)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    metric.label.uppercase(),
                    color = TextSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (value != null) metric.formatValue(value) else "--",
                    color = if (value != null) TextPrimary else TextSecondary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                if (metric.unit.isNotEmpty()) {
                    Text(
                        metric.unit,
                        color = metric.color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─── Mini sparkline (Canvas-drawn, no Vico to avoid scroll jank) ─────────────

@Composable
fun MiniSparkline(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (values.size < 2) return
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i / (values.size - 1f) * size.width
            val y = size.height - ((v - min) / range) * size.height * 0.85f - size.height * 0.075f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// ─── Session row (used in HomeScreen recent sessions list) ───────────────────

@Composable
fun SessionRow(
    session: WorkoutSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = SimpleDateFormat("EEE d MMM", Locale("nl")).format(Date(session.date))
    val metricCount = listOf(
        session.calories, session.durationSeconds?.toFloat(), session.distanceKm,
        session.avgPower, session.avgSpeed, session.avgHeartRate, session.maxHeartRate,
        session.caloriesPerHour, session.conditionPI, session.moves
    ).count { it != null }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(AccentCalories)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(dateStr, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("$metricCount metrics vastgelegd", color = TextSecondary, fontSize = 12.sp)
        }
        if (session.calories != null) {
            Text(
                "${session.calories.toInt()} kcal",
                color = AccentCalories,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.ChevronRight, tint = TextSecondary, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

// ─── Swipe background (edit = green right, delete = red left) ─────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeBackground(dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.targetValue
    val isEditSwipe = direction == SwipeToDismissBoxValue.StartToEnd
    val isDeleteSwipe = direction == SwipeToDismissBoxValue.EndToStart
    val bgColor = when {
        isEditSwipe -> Color(0xFF2ECC71)
        isDeleteSwipe -> Color(0xFFE74C3C)
        else -> Color.Transparent
    }
    val icon = if (isEditSwipe) Icons.Default.Edit else Icons.Default.Delete
    val alignment = if (isEditSwipe) Alignment.CenterStart else Alignment.CenterEnd
    val padding = if (isEditSwipe) PaddingValues(start = 20.dp) else PaddingValues(end = 20.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor, RoundedCornerShape(16.dp))
            .padding(padding),
        contentAlignment = alignment
    ) {
        if (isEditSwipe || isDeleteSwipe) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

// ─── Stats row (avg / max / min) — used in DetailScreen ──────────────────────

@Composable
fun StatsRow(
    values: List<Float>,
    unit: String,
    color: Color,
    formatValue: (Float) -> String
) {
    if (values.isEmpty()) return
    val avg = values.average().toFloat()
    val max = values.max()
    val min = values.min()

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Gem." to avg, "Max" to max, "Min" to min).forEach { (label, value) ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        label.uppercase(),
                        color = TextSecondary,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(formatValue(value), color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (unit.isNotEmpty()) {
                        Text(unit, color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ─── Per-session metric row — used in DetailScreen history list ───────────────

@Composable
fun SessionMetricRow(
    session: WorkoutSession,
    value: Float,
    prevValue: Float?,
    metric: MetricDef
) {
    val delta = if (prevValue != null) value - prevValue else null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            SimpleDateFormat("d MMM", Locale("nl")).format(Date(session.date)),
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        if (delta != null) {
            val deltaColor = if (delta >= 0f) AccentCondition else AccentAvgHR
            val deltaStr = "${if (delta >= 0f) "+" else ""}${metric.formatValue(delta)}"
            Text(
                deltaStr,
                color = deltaColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
        Text(
            "${metric.formatValue(value)}${if (metric.unit.isNotEmpty()) " ${metric.unit}" else ""}",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
