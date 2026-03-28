package nl.gymlog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.entry.entryModelOf
import nl.gymlog.data.WorkoutSession
import nl.gymlog.ui.ALL_METRICS
import nl.gymlog.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DetailScreen(
    metricKey: String,
    sessions: List<WorkoutSession>,
    onBack: () -> Unit
) {
    val metric = ALL_METRICS.find { it.key == metricKey } ?: return
    val sorted = sessions.sortedBy { it.date }
    val values = sorted.mapNotNull { metric.getValue(it) }
    val sessionsWithValues = sorted.filter { metric.getValue(it) != null }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // ── Header: back + title ────────────────────────────────────────────
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Terug", tint = TextPrimary)
                }
                Column {
                    Text(
                        metric.label.uppercase(),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(metric.label, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Latest value (hero) ─────────────────────────────────────────────
        val latest = sorted.lastOrNull()?.let { metric.getValue(it) }
        if (latest != null) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        metric.formatValue(latest),
                        color = metric.color,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (metric.unit.isNotEmpty()) {
                        Text(
                            " ${metric.unit}",
                            color = TextSecondary,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                Text("Laatste waarde", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Stats row (avg / max / min) ─────────────────────────────────────
        if (values.size >= 2) {
            item {
                StatsRow(
                    values = values,
                    unit = metric.unit,
                    color = metric.color,
                    formatValue = metric.formatValue
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Chart ───────────────────────────────────────────────────────────
        if (values.size >= 2) {
            item {
                SectionHeader("Trend")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    val model = entryModelOf(*values.mapIndexed { i, v -> i.toFloat() to v }.toTypedArray())
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                LineChart.LineSpec(
                                    lineColor = metric.color.hashCode(),
                                    lineBackgroundShader = DynamicShaders.fromBrush(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            listOf(
                                                metric.color.copy(alpha = 0.4f),
                                                metric.color.copy(alpha = 0f)
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        model = model,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = { value, _ ->
                                sorted.getOrNull(value.toInt())?.let {
                                    SimpleDateFormat("dd/MM", Locale("nl")).format(Date(it.date))
                                } ?: ""
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(8.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        } else {
            item {
                Text(
                    "Minimaal 2 sessies nodig voor een grafiek.",
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                )
            }
        }

        // ── Session history ─────────────────────────────────────────────────
        if (sessionsWithValues.isNotEmpty()) {
            item {
                SectionHeader("Sessiegeschiedenis")
            }

            itemsIndexed(sessionsWithValues.reversed()) { idx, session ->
                val value = metric.getValue(session) ?: return@itemsIndexed
                val prevSession = if (idx < sessionsWithValues.reversed().size - 1)
                    sessionsWithValues.reversed()[idx + 1] else null
                val prevValue = prevSession?.let { metric.getValue(it) }

                SessionMetricRow(
                    session = session,
                    value = value,
                    prevValue = prevValue,
                    metric = metric
                )
                if (idx < sessionsWithValues.size - 1) {
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}
