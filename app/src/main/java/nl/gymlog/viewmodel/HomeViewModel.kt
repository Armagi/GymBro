package nl.gymlog.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import nl.gymlog.data.WorkoutDatabase
import nl.gymlog.data.WorkoutRepository
import nl.gymlog.data.WorkoutSession
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(WorkoutDatabase.getInstance(app).workoutDao())

    val sessions = repo.allSessions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun delete(session: WorkoutSession) = viewModelScope.launch { repo.delete(session) }
    fun update(session: WorkoutSession) = viewModelScope.launch { repo.update(session) }
}
