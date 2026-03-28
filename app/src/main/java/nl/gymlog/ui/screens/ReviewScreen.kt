package nl.gymlog.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.gymlog.data.WorkoutSession
import nl.gymlog.ocr.ParsedWorkout
import nl.gymlog.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    parsed: ParsedWorkout?,
    existingSession: WorkoutSession?,
    initialDate: Long,
    onSave: (WorkoutSession) -> Unit,
    onUpdate: (WorkoutSession) -> Unit,
    onRetake: () -> Unit
) {
    val isEditMode = existingSession != null

    var calories by remember { mutableStateOf(existingSession?.calories?.toString() ?: parsed?.calories?.toString() ?: "") }
    var duration by remember { mutableStateOf(
        existingSession?.durationSeconds?.let { "${it/60}:${"%02d".format(it%60)}" }
            ?: parsed?.durationSeconds?.let { "${it/60}:${"%02d".format(it%60)}" }
            ?: ""
    ) }
    var distance by remember { mutableStateOf(existingSession?.distanceKm?.toString() ?: parsed?.distanceKm?.toString() ?: "") }
    var avgPower by remember { mutableStateOf(existingSession?.avgPower?.toString() ?: parsed?.avgPower?.toString() ?: "") }
    var avgSpeed by remember { mutableStateOf(existingSession?.avgSpeed?.toString() ?: parsed?.avgSpeed?.toString() ?: "") }
    var avgHR by remember { mutableStateOf(existingSession?.avgHeartRate?.toString() ?: parsed?.avgHeartRate?.toString() ?: "") }
    var maxHR by remember { mutableStateOf(existingSession?.maxHeartRate?.toString() ?: parsed?.maxHeartRate?.toString() ?: "") }
    var calPerHour by remember { mutableStateOf(existingSession?.caloriesPerHour?.toString() ?: parsed?.caloriesPerHour?.toString() ?: "") }
    var condition by remember { mutableStateOf(existingSession?.conditionPI?.toString() ?: parsed?.conditionPI?.toString() ?: "") }
    var moves by remember { mutableStateOf(existingSession?.moves?.toString() ?: parsed?.moves?.toString() ?: "") }
    var date by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }

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
        Text(
            if (isEditMode) "Sessie bewerken" else "Controleer je sessie",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        // Tappable date row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
                .background(Surface, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(dateStr, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Fields: label, current value, ocr-null flag (only relevant in new mode), setter
        val fields = listOf(
            Triple("Calorieën (kcal)", calories to (parsed?.calories == null && !isEditMode)) { v: String -> calories = v },
            Triple("Duur (MM:SS)", duration to (parsed?.durationSeconds == null && !isEditMode)) { v: String -> duration = v },
            Triple("Afstand (km)", distance to (parsed?.distanceKm == null && !isEditMode)) { v: String -> distance = v },
            Triple("Gem. vermogen (watt)", avgPower to (parsed?.avgPower == null && !isEditMode)) { v: String -> avgPower = v },
            Triple("Gem. snelheid (spm)", avgSpeed to (parsed?.avgSpeed == null && !isEditMode)) { v: String -> avgSpeed = v },
            Triple("Gem. hartslag (spm)", avgHR to (parsed?.avgHeartRate == null && !isEditMode)) { v: String -> avgHR = v },
            Triple("Max. hartslag (spm)", maxHR to (parsed?.maxHeartRate == null && !isEditMode)) { v: String -> maxHR = v },
            Triple("Kcal/uur", calPerHour to (parsed?.caloriesPerHour == null && !isEditMode)) { v: String -> calPerHour = v },
            Triple("Conditie (PI)", condition to (parsed?.conditionPI == null && !isEditMode)) { v: String -> condition = v },
            Triple("MOVEs", moves to (parsed?.moves == null && !isEditMode)) { v: String -> moves = v }
        )

        fields.forEach { (label, valuePair, onValueChange) ->
            val (value, needsReview) = valuePair
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
                val session = WorkoutSession(
                    id = existingSession?.id ?: 0,
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
                    needsReview = if (isEditMode) false else fields.any { it.second.second }
                )
                if (isEditMode) onUpdate(session) else onSave(session)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCalories),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (isEditMode) "Wijzigingen opslaan" else "Opslaan",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Border),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (isEditMode) "Annuleren" else "Opnieuw fotograferen",
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    // DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("OK", color = AccentCalories) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuleren", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Surface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Surface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextSecondary,
                    navigationContentColor = TextPrimary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = AccentCalories,
                    selectedYearContentColor = TextPrimary,
                    selectedYearContainerColor = AccentCalories,
                    dayContentColor = TextPrimary,
                    selectedDayContentColor = TextPrimary,
                    selectedDayContainerColor = AccentCalories,
                    todayContentColor = AccentCalories,
                    todayDateBorderColor = AccentCalories
                )
            )
        }
    }
}
