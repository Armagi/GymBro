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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sessions: List<WorkoutSession>,
    onCapture: () -> Unit,
    onMetricClick: (String) -> Unit,
    onSessionEdit: (WorkoutSession) -> Unit,
    onSessionDelete: (WorkoutSession) -> Unit
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── Header ──────────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "GymBro",
                                color = TextPrimary,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${sessions.size} sessie${if (sessions.size != 1) "s" else ""} gelogd",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // ── Hero cards (latest calories + duration) ─────────────────
                item {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val latest = sessions.first()
                        HeroCard(
                            metric = ALL_METRICS[0], // calories
                            value = ALL_METRICS[0].getValue(latest),
                            modifier = Modifier.weight(1f)
                        )
                        HeroCard(
                            metric = ALL_METRICS[1], // duration
                            value = ALL_METRICS[1].getValue(latest),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Recent sessions ─────────────────────────────────────────
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader("Recente sessies")
                }

                items(sessions.take(5), key = { it.id }) { session ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    onSessionEdit(session)
                                    false // don't dismiss — just trigger edit
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    onSessionDelete(session)
                                    true
                                }
                                else -> false
                            }
                        }
                    )

                    // Reset dismiss state after edit swipe so it snaps back
                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
                            dismissState.reset()
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = { SwipeBackground(dismissState) },
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface)
                        ) {
                            SessionRow(
                                session = session,
                                onClick = { onSessionEdit(session) }
                            )
                        }
                    }
                }

                // ── Metrics ─────────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader("Metrics")
                }

                items(ALL_METRICS) { metric ->
                    MetricCard(
                        metric = metric,
                        sessions = sessions,
                        onClick = { onMetricClick(metric.key) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                item { Spacer(Modifier.height(88.dp)) }
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

// ─── Metric card (with mini sparkline) ───────────────────────────────────────

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
    val sparkValues = sessions.takeLast(8).reversed()
        .mapNotNull { metric.getValue(it) }

    val trend = when {
        latestVal == null || prevVal == null -> 0
        latestVal > prevVal -> 1
        latestVal < prevVal -> -1
        else -> 0
    }
    val delta = if (latestVal != null && prevVal != null) latestVal - prevVal else null

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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    metric.label.uppercase(),
                    color = TextSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (latestVal != null) metric.formatValue(latestVal) else "--",
                        color = if (latestVal != null) TextPrimary else TextSecondary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (metric.unit.isNotEmpty()) {
                        Text(
                            " ${metric.unit}",
                            color = metric.color,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
                if (delta != null) {
                    val deltaColor = if (delta >= 0f) AccentCondition else AccentAvgHR
                    val deltaStr = "${if (delta >= 0f) "+" else ""}${metric.formatValue(delta)}"
                    Text(deltaStr, color = deltaColor, fontSize = 12.sp)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
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
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(8.dp))
                MiniSparkline(
                    values = sparkValues,
                    color = metric.color,
                    modifier = Modifier.size(width = 64.dp, height = 28.dp)
                )
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
fun EmptyState(onCapture: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("GymBro", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Track je Technogym workouts.", color = TextSecondary, fontSize = 16.sp)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onCapture,
            colors = ButtonDefaults.buttonColors(containerColor = AccentCalories)
        ) {
            Text("Maak je eerste foto", color = Color.White)
        }
    }
}
