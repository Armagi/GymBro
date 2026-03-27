package nl.gymlog.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import nl.gymlog.data.WorkoutSession
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class ParsedWorkout(
    val calories: Float? = null,
    val durationSeconds: Int? = null,
    val distanceKm: Float? = null,
    val avgPower: Float? = null,
    val avgSpeed: Float? = null,
    val avgHeartRate: Float? = null,
    val maxHeartRate: Float? = null,
    val caloriesPerHour: Float? = null,
    val conditionPI: Float? = null,
    val moves: Float? = null
) {
    val hasNulls get() = listOf(
        calories, durationSeconds, distanceKm, avgPower, avgSpeed,
        avgHeartRate, maxHeartRate, caloriesPerHour, conditionPI, moves
    ).any { it == null }
}

object OcrParser {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun parse(bitmap: Bitmap): ParsedWorkout = suspendCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val lines = visionText.textBlocks
                    .flatMap { it.lines }
                    .sortedBy { it.boundingBox?.top ?: 0 }

                fun findValue(label: String): String? {
                    val labelLine = lines.firstOrNull {
                        it.text.contains(label, ignoreCase = true)
                    } ?: return null
                    val labelTop = labelLine.boundingBox?.top ?: return null
                    val labelBottom = labelLine.boundingBox?.bottom ?: return null
                    val valueLine = lines.firstOrNull { line ->
                        line != labelLine &&
                        (line.boundingBox?.top ?: 0) in (labelTop - 30)..(labelBottom + 30)
                    }
                    return valueLine?.text ?: labelLine.text
                }

                fun extractNumber(text: String?): Float? {
                    if (text == null) return null
                    return Regex("(\\d+[.,]?\\d*)").find(text)
                        ?.value?.replace(",", ".")?.toFloatOrNull()
                }

                fun parseDuration(text: String?): Int? {
                    if (text == null) return null
                    val match = Regex("(\\d{1,2}):(\\d{2})").find(text) ?: return null
                    val mins = match.groupValues[1].toIntOrNull() ?: return null
                    val secs = match.groupValues[2].toIntOrNull() ?: return null
                    return mins * 60 + secs
                }

                val result = ParsedWorkout(
                    calories = extractNumber(findValue("calorie")),
                    durationSeconds = parseDuration(findValue("oefening")),
                    distanceKm = extractNumber(findValue("afstand")),
                    avgPower = extractNumber(findValue("vermogen")),
                    avgSpeed = extractNumber(findValue("snelheid")),
                    avgHeartRate = extractNumber(findValue("Gemiddelde hart")),
                    maxHeartRate = extractNumber(findValue("Maximale hart")),
                    caloriesPerHour = extractNumber(findValue("per uur")),
                    conditionPI = extractNumber(findValue("Conditie")),
                    moves = extractNumber(findValue("MOVEs"))
                )
                cont.resume(result)
            }
            .addOnFailureListener {
                cont.resume(ParsedWorkout())
            }
    }
}
