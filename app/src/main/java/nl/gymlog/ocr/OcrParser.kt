package nl.gymlog.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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

    // Each metric maps to a list of label keyword variants (lowercased, accent-stripped).
    // Order matters: the first variant that matches wins; longer/more specific phrases come first.
    private val LABELS_CALORIES = listOf("calorieen", "calorie", "kcal verbrand", "energie", "kcal")
    private val LABELS_DURATION = listOf("oefeningsduur", "trainingsduur", "duur", "tijd", "totale tijd")
    private val LABELS_DISTANCE = listOf("afgelegde afstand", "afstand", "distance", "km")
    private val LABELS_AVG_POWER = listOf("gemiddeld vermogen", "gem vermogen", "gem. vermogen", "vermogen", "watt")
    private val LABELS_AVG_SPEED = listOf("gemiddelde snelheid", "gem snelheid", "gem. snelheid", "slagen per minuut", "snelheid", "spm")
    private val LABELS_AVG_HR = listOf("gemiddelde hartslag", "gem hartslag", "gem. hartslag", "gemiddelde hart", "avg hr")
    private val LABELS_MAX_HR = listOf("maximale hartslag", "max hartslag", "max. hartslag", "maximale hart", "max hr")
    private val LABELS_KCAL_HOUR = listOf("calorieen per uur", "kcal per uur", "kcal/uur", "kcal/h", "per uur")
    private val LABELS_CONDITION = listOf("conditie pi", "condition pi", "conditie-index", "conditie", "pi")
    private val LABELS_MOVES = listOf("moves", "move-punten", "move punten")

    suspend fun parse(bitmap: Bitmap): ParsedWorkout = suspendCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                cont.resume(parseText(visionText))
            }
            .addOnFailureListener {
                cont.resume(ParsedWorkout())
            }
    }

    private fun parseText(visionText: Text): ParsedWorkout {
        val lines = visionText.textBlocks
            .flatMap { it.lines }
            .filter { it.boundingBox != null }
            .sortedBy { it.boundingBox!!.top }

        if (lines.isEmpty()) return ParsedWorkout()

        return ParsedWorkout(
            calories = findNumber(lines, LABELS_CALORIES),
            durationSeconds = findDuration(lines, LABELS_DURATION),
            distanceKm = findNumber(lines, LABELS_DISTANCE),
            avgPower = findNumber(lines, LABELS_AVG_POWER),
            avgSpeed = findNumber(lines, LABELS_AVG_SPEED),
            avgHeartRate = findNumber(lines, LABELS_AVG_HR),
            maxHeartRate = findNumber(lines, LABELS_MAX_HR),
            caloriesPerHour = findNumber(lines, LABELS_KCAL_HOUR),
            conditionPI = findNumber(lines, LABELS_CONDITION),
            moves = findNumber(lines, LABELS_MOVES)
        )
    }

    private fun normalize(s: String): String = s.lowercase()
        .replace('ë', 'e').replace('é', 'e').replace('è', 'e').replace('ê', 'e')
        .replace('ï', 'i').replace('í', 'i').replace('ì', 'i').replace('î', 'i')
        .replace('ö', 'o').replace('ó', 'o').replace('ò', 'o').replace('ô', 'o')
        .replace('ü', 'u').replace('ú', 'u').replace('ù', 'u').replace('û', 'u')
        .replace('ä', 'a').replace('á', 'a').replace('à', 'a').replace('â', 'a')
        .replace(Regex("[^a-z0-9 /.\\-]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun findLabelLine(lines: List<Text.Line>, variants: List<String>): Text.Line? {
        // Try variants in order; the first one that matches any line wins.
        for (variant in variants) {
            val target = normalize(variant)
            val match = lines.firstOrNull { line ->
                normalize(line.text).contains(target)
            }
            if (match != null) return match
        }
        return null
    }

    private fun horizontalOverlap(a: Rect, b: Rect): Float {
        val left = maxOf(a.left, b.left)
        val right = minOf(a.right, b.right)
        val overlap = (right - left).coerceAtLeast(0)
        val minWidth = minOf(a.width(), b.width()).coerceAtLeast(1)
        return overlap.toFloat() / minWidth.toFloat()
    }

    private fun candidateLines(lines: List<Text.Line>, label: Text.Line): List<Text.Line> {
        val labelBox = label.boundingBox ?: return emptyList()
        val labelHeight = labelBox.height().coerceAtLeast(20)
        // Look both above and below the label, prioritising lines aligned horizontally.
        return lines
            .filter { it !== label && it.boundingBox != null }
            .map { it to it.boundingBox!! }
            .filter { (_, box) ->
                horizontalOverlap(labelBox, box) > 0.25f &&
                    kotlin.math.abs(box.centerY() - labelBox.centerY()) < labelHeight * 6
            }
            .sortedBy { (_, box) ->
                // Prefer the closest line below the label, then closest above.
                val verticalDistance = kotlin.math.abs(box.centerY() - labelBox.centerY())
                val belowBonus = if (box.top >= labelBox.bottom - 5) 0 else labelHeight
                verticalDistance + belowBonus
            }
            .map { it.first }
    }

    private fun extractFirstNumber(text: String): Float? {
        // Strip out colons that are part of times so we don't accidentally pull "12" from "12:34".
        val cleaned = text.replace(Regex("\\d{1,2}:\\d{2}"), " ")
        val match = Regex("(-?\\d{1,4}(?:[.,]\\d{1,3})?)").find(cleaned) ?: return null
        return match.value.replace(",", ".").toFloatOrNull()
    }

    private fun findNumber(lines: List<Text.Line>, variants: List<String>): Float? {
        val label = findLabelLine(lines, variants) ?: return null

        // 1) Sometimes the number sits on the same line as the label.
        val sameLine = label.text
            .let { Regex("[A-Za-zÀ-ÿ%/]").replace(it, " ") }
            .let { extractFirstNumber(it) }
        if (sameLine != null) return sameLine

        // 2) Otherwise look at horizontally aligned neighbouring lines.
        for (candidate in candidateLines(lines, label)) {
            val n = extractFirstNumber(candidate.text)
            if (n != null) return n
        }
        return null
    }

    private fun findDuration(lines: List<Text.Line>, variants: List<String>): Int? {
        val label = findLabelLine(lines, variants) ?: return null
        val pattern = Regex("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?")

        fun parseFromText(text: String): Int? {
            val m = pattern.find(text) ?: return null
            val a = m.groupValues[1].toIntOrNull() ?: return null
            val b = m.groupValues[2].toIntOrNull() ?: return null
            val c = m.groupValues.getOrNull(3)?.toIntOrNull()
            return if (c != null) a * 3600 + b * 60 + c else a * 60 + b
        }

        parseFromText(label.text)?.let { return it }
        for (candidate in candidateLines(lines, label)) {
            parseFromText(candidate.text)?.let { return it }
        }
        return null
    }
}
