package nl.gymlog.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.gymlog.data.WorkoutDatabase
import nl.gymlog.data.WorkoutRepository
import nl.gymlog.data.WorkoutSession
import nl.gymlog.ocr.OcrParser
import nl.gymlog.ocr.ParsedWorkout
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class BulkImportViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(WorkoutDatabase.getInstance(app).workoutDao())

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _processed = MutableStateFlow(0)
    val processed = _processed.asStateFlow()

    private val _total = MutableStateFlow(0)
    val total = _total.asStateFlow()

    private val _imported = MutableStateFlow(0)
    val imported = _imported.asStateFlow()

    private val _failed = MutableStateFlow(0)
    val failed = _failed.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done = _done.asStateFlow()

    fun reset() {
        _isRunning.value = false
        _processed.value = 0
        _total.value = 0
        _imported.value = 0
        _failed.value = 0
        _done.value = false
    }

    fun importImages(uris: List<Uri>) {
        if (uris.isEmpty() || _isRunning.value) return
        _isRunning.value = true
        _done.value = false
        _processed.value = 0
        _imported.value = 0
        _failed.value = 0
        _total.value = uris.size

        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            for (uri in uris) {
                try {
                    val date = withContext(Dispatchers.IO) {
                        resolver.openInputStream(uri)?.use { ExifInterface(it) }
                            ?.let { exifDateMillis(it) }
                    } ?: System.currentTimeMillis()

                    val bitmap = withContext(Dispatchers.IO) {
                        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    }
                    if (bitmap == null) {
                        _failed.value += 1
                    } else {
                        val parsed = OcrParser.parse(bitmap)
                        bitmap.recycle()
                        repo.insert(parsed.toSession(date))
                        _imported.value += 1
                    }
                } catch (_: Exception) {
                    _failed.value += 1
                }
                _processed.value += 1
            }
            _isRunning.value = false
            _done.value = true
        }
    }

    private fun exifDateMillis(exif: ExifInterface): Long? {
        val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            ?: return null
        val format = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        return runCatching { format.parse(raw)?.time }.getOrNull()
    }

    private fun ParsedWorkout.toSession(date: Long) = WorkoutSession(
        date = date,
        calories = calories,
        durationSeconds = durationSeconds,
        distanceKm = distanceKm,
        avgPower = avgPower,
        avgSpeed = avgSpeed,
        avgHeartRate = avgHeartRate,
        maxHeartRate = maxHeartRate,
        caloriesPerHour = caloriesPerHour,
        conditionPI = conditionPI,
        moves = moves,
        needsReview = hasNulls
    )
}
