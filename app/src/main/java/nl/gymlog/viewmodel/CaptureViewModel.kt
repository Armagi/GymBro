package nl.gymlog.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.gymlog.data.WorkoutDatabase
import nl.gymlog.data.WorkoutRepository
import nl.gymlog.data.WorkoutSession
import nl.gymlog.ocr.OcrParser
import nl.gymlog.ocr.ParsedWorkout
import java.io.File

class CaptureViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(WorkoutDatabase.getInstance(app).workoutDao())

    private val _parsed = MutableStateFlow<ParsedWorkout?>(null)
    val parsed = _parsed.asStateFlow()

    private val _photoPath = MutableStateFlow<String?>(null)
    val photoPath = _photoPath.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    fun processPhoto(path: String) {
        _photoPath.value = path
        viewModelScope.launch {
            _isProcessing.value = true
            val bitmap = BitmapFactory.decodeFile(path)
            _parsed.value = OcrParser.parse(bitmap)
            _isProcessing.value = false
        }
    }

    fun saveSession(session: WorkoutSession) = viewModelScope.launch {
        repo.insert(session)
        _parsed.value = null
        _photoPath.value = null
    }
}
