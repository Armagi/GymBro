package nl.gymlog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val calories: Float? = null,
    val durationSeconds: Int? = null,
    val distanceKm: Float? = null,
    val avgPower: Float? = null,
    val avgSpeed: Float? = null,
    val avgHeartRate: Float? = null,
    val maxHeartRate: Float? = null,
    val caloriesPerHour: Float? = null,
    val conditionPI: Float? = null,
    val moves: Float? = null,
    val needsReview: Boolean = false
)
