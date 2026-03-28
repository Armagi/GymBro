package nl.gymlog.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
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
import java.text.SimpleDateFormat
import java.util.Locale

class CaptureViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(WorkoutDatabase.getInstance(app).workoutDao())

    private val _parsed = MutableStateFlow<ParsedWorkout?>(null)
    val parsed = _parsed.asStateFlow()

    private val _photoPath = MutableStateFlow<String?>(null)
    val photoPath = _photoPath.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _pendingDate = MutableStateFlow(System.currentTimeMillis())
    val pendingDate = _pendingDate.asStateFlow()

    fun processPhoto(path: String) {
        _photoPath.value = path
        _pendingDate.value = System.currentTimeMillis()
        viewModelScope.launch {
            _isProcessing.value = true
            val bitmap = BitmapFactory.decodeFile(path)
            _parsed.value = OcrParser.parse(bitmap)
            _isProcessing.value = false
        }
    }

    fun processGalleryUri(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            val context = getApplication<Application>()
            // Copy content URI to cache — ExifInterface requires a real file path
            val file = File(context.cacheDir, "gallery_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            _photoPath.value = file.absolutePath
            // Read EXIF date (prefer DATETIME_ORIGINAL over DATETIME)
            _pendingDate.value = readExifDate(file.absolutePath) ?: System.currentTimeMillis()
            // Decode + auto-rotate before OCR (gallery images may have EXIF rotation)
            val raw = BitmapFactory.decodeFile(file.absolutePath)
            val bitmap = rotateBitmapFromExif(file.absolutePath, raw)
            _parsed.value = OcrParser.parse(bitmap)
            _isProcessing.value = false
        }
    }

    fun saveSession(session: WorkoutSession) = viewModelScope.launch {
        repo.insert(session)
        _parsed.value = null
        _photoPath.value = null
        _pendingDate.value = System.currentTimeMillis()
    }

    fun reset() {
        _parsed.value = null
        _photoPath.value = null
        _pendingDate.value = System.currentTimeMillis()
        _isProcessing.value = false
    }

    private fun readExifDate(path: String): Long? {
        return try {
            val exif = ExifInterface(path)
            val str = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                ?: return null
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(str)?.time
        } catch (e: Exception) { null }
    }

    private fun rotateBitmapFromExif(path: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) { bitmap }
    }
}
