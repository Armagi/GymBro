package nl.gymlog.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.gymlog.data.WorkoutSession
import nl.gymlog.ui.ALL_METRICS
import nl.gymlog.ui.MetricDef
import nl.gymlog.ui.theme.*

@Composable
fun HomeScreen(
    sessions: List<WorkoutSession>,
    onCapture: () -> Unit,
    onMetricClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        if (sessions.isEmpty()) {
            EmptyState(onCapture)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "GymLog",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                    )
                }
                item {
                    HighlightRow(sessions)
                }
                item {
                    Text(
                        "${sessions.size} sessies gelogd",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(ALL_METRICS) { metric ->
                    MetricCard(metric, sessions, onClick = { onMetricClick(metric.key) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = onCapture,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = AccentCalories
        ) {
            Icon(Icons.Default.AddAPhoto, contentDescription = "Foto maken", tint = Color.White)
        }
    }
}

@Composable
fun HighlightRow(sessions: List<WorkoutSession>) {
    val latest = sessions.firstOrNull() ?: return
    val highlights = listOf(
        ALL_METRICS[0], // calories
        ALL_METRICS[5], // avgHeartRate
        ALL_METRICS[9]  // moves
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        highlights.forEach { metric ->
            val value = metric.getValue(latest)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(metric.label, color = TextSecondary, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (value != null) metric.formatValue(value) else "--",
                        color = metric.color,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (metric.unit.isNotEmpty()) {
                        Text(metric.unit, color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    metric: MetricDef,
    sessions: List<WorkoutSession>,
    onClick: () -> Unit
) {
    val latest = sessions.firstOrNull()
    val previous = sessions.getOrNull(1)
    val latestVal = latest?.let { metric.getValue(it) }
    val prevVal = previous?.let { metric.getValue(it) }

    val trend = when {
        latestVal == null || prevVal == null -> 0
        latestVal > prevVal -> 1
        latestVal < prevVal -> -1
        else -> 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 40.dp)
                    .background(metric.color, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(metric.label, color = TextSecondary, fontSize = 12.sp)
                Text(
                    if (latestVal != null) "${metric.formatValue(latestVal)} ${metric.unit}".trim()
                    else "--",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                imageVector = when (trend) {
                    1 -> Icons.Default.TrendingUp
                    -1 -> Icons.Default.TrendingDown
                    else -> Icons.Default.TrendingFlat
                },
                contentDescription = null,
                tint = when (trend) {
                    1 -> AccentCondition
                    -1 -> AccentAvgHR
                    else -> TextSecondary
                }
            )
        }
    }
}

@Composable
fun EmptyState(onCapture: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("GymLog", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Nog geen sessies.", color = TextSecondary, fontSize = 16.sp)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onCapture,
            colors = ButtonDefaults.buttonColors(containerColor = AccentCalories)
        ) {
            Text("Maak je eerste foto", color = Color.White)
        }
    }
}
