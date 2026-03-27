package nl.gymlog.data

import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val dao: WorkoutDao) {
    val allSessions: Flow<List<WorkoutSession>> = dao.getAllSessions()
    suspend fun insert(session: WorkoutSession) = dao.insert(session)
    suspend fun update(session: WorkoutSession) = dao.update(session)
    suspend fun delete(session: WorkoutSession) = dao.delete(session)
}
