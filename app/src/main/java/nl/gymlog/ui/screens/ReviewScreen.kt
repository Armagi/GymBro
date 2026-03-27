package nl.gymlog.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.gymlog.data.WorkoutSession
import nl.gymlog.ocr.ParsedWorkout
import nl.gymlog.ui.ALL_METRICS
import nl.gymlog.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReviewScreen(
    parsed: ParsedWorkout,
    onSave: (WorkoutSession) -> Unit,
    onRetake: () -> Unit
) {
    var calories by remember { mutableStateOf(parsed.calories?.toString() ?: "") }
    var duration by remember { mutableStateOf(parsed.durationSeconds?.let { "${it/60}:${"%02d".format(it%60)}" } ?: "") }
    var distance by remember { mutableStateOf(parsed.distanceKm?.toString() ?: "") }
    var avgPower by remember { mutableStateOf(parsed.avgPower?.toString() ?: "") }
    var avgSpeed by remember { mutableStateOf(parsed.avgSpeed?.toString() ?: "") }
    var avgHR by remember { mutableStateOf(parsed.avgHeartRate?.toString() ?: "") }
    var maxHR by remember { mutableStateOf(parsed.maxHeartRate?.toString() ?: "") }
    var calPerHour by remember { mutableStateOf(parsed.caloriesPerHour?.toString() ?: "") }
    var condition by remember { mutableStateOf(parsed.conditionPI?.toString() ?: "") }
    var moves by remember { mutableStateOf(parsed.moves?.toString() ?: "") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }

    val dateStr = remember(date) {
        SimpleDateFormat("dd MMM yyyy", Locale("nl")).format(Date(date))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Controleer je sessie", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(dateStr, color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))

        val fields = listOf(
            "Calorieën (kcal)" to Triple(calories, parsed.calories == null) { v: String -> calories = v },
            "Duur (MM:SS)" to Triple(duration, parsed.durationSeconds == null) { v: String -> duration = v },
            "Afstand (km)" to Triple(distance, parsed.distanceKm == null) { v: String -> distance = v },
            "Gem. vermogen (watt)" to Triple(avgPower, parsed.avgPower == null) { v: String -> avgPower = v },
            "Gem. snelheid (spm)" to Triple(avgSpeed, parsed.avgSpeed == null) { v: String -> avgSpeed = v },
            "Gem. hartslag (spm)" to Triple(avgHR, parsed.avgHeartRate == null) { v: String -> avgHR = v },
            "Max. hartslag (spm)" to Triple(maxHR, parsed.maxHeartRate == null) { v: String -> maxHR = v },
            "Kcal/uur" to Triple(calPerHour, parsed.caloriesPerHour == null) { v: String -> calPerHour = v },
            "Conditie (PI)" to Triple(condition, parsed.conditionPI == null) { v: String -> condition = v },
            "MOVEs" to Triple(moves, parsed.moves == null) { v: String -> moves = v }
        )

        fields.forEach { (label, triple) ->
            val (value, needsReview, onValueChange) = triple
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label, color = if (needsReview) Color(0xFFFFA502) else TextSecondary) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (needsReview) Color(0xFFFFA502) else AccentCalories,
                    unfocusedBorderColor = if (needsReview) Color(0xFFFFA502).copy(alpha = 0.5f) else Border,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentCalories
                )
            )
        }

        Spacer(Modifier.height(24.dp))

        fun parseDur(s: String): Int? {
            val parts = s.split(":")
            return if (parts.size == 2) {
                val m = parts[0].toIntOrNull() ?: return null
                val sec = parts[1].toIntOrNull() ?: return null
                m * 60 + sec
            } else s.toIntOrNull()
        }

        Button(
            onClick = {
                onSave(WorkoutSession(
                    date = date,
                    calories = calories.toFloatOrNull(),
                    durationSeconds = parseDur(duration),
                    distanceKm = distance.toFloatOrNull(),
                    avgPower = avgPower.toFloatOrNull(),
                    avgSpeed = avgSpeed.toFloatOrNull(),
                    avgHeartRate = avgHR.toFloatOrNull(),
                    maxHeartRate = maxHR.toFloatOrNull(),
                    caloriesPerHour = calPerHour.toFloatOrNull(),
                    conditionPI = condition.toFloatOrNull(),
                    moves = moves.toFloatOrNull(),
                    needsReview = fields.any { it.second.second }
                ))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCalories),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Opslaan", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Border),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Opnieuw fotograferen", color = TextSecondary)
        }

        Spacer(Modifier.height(32.dp))
    }
}
