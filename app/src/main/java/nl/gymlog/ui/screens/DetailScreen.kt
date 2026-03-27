package nl.gymlog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Terug", tint = TextPrimary)
            }
            Text(metric.label, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))

        if (values.size >= 2) {
            val model = entryModelOf(*values.mapIndexed { i, v -> i.toFloat() to v }.toTypedArray())
            Chart(
                chart = lineChart(
                    lines = listOf(
                        LineChart.LineSpec(
                            lineColor = metric.color.hashCode(),
                            lineBackgroundShader = DynamicShaders.fromBrush(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(metric.color.copy(alpha = 0.3f), metric.color.copy(alpha = 0f))
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
                modifier = Modifier.fillMaxWidth().height(300.dp)
            )
        } else {
            Text(
                "Minimaal 2 sessies nodig voor een grafiek.",
                color = TextSecondary,
                modifier = Modifier.padding(top = 32.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        val latest = sorted.lastOrNull()?.let { metric.getValue(it) }
        if (latest != null) {
            Text("Laatste waarde", color = TextSecondary, fontSize = 13.sp)
            Text(
                "${metric.formatValue(latest)} ${metric.unit}".trim(),
                color = metric.color,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
