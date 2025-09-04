package com.habittracker.data.profiles

import com.habittracker.data.database.dao.timing.TimerAlertProfileDao
import com.habittracker.data.database.entity.timing.TimerAlertProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertProfilesRepository @Inject constructor(
    private val dao: TimerAlertProfileDao
) {
    fun profiles(): Flow<List<TimerAlertProfileEntity>> = dao.getAllProfiles()
    suspend fun upsert(profile: TimerAlertProfileEntity) = dao.upsertProfiles(profile)
    suspend fun delete(id: String) = dao.deleteProfile(id)
    suspend fun get(id: String): TimerAlertProfileEntity? = dao.getProfile(id)
}
