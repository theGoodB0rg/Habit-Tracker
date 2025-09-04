package com.habittracker.data.database.dao.timing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.habittracker.data.database.entity.timing.TimerAlertProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for timer alert profiles (Phase UIX-1)
 */
@Dao
interface TimerAlertProfileDao {
    @Query("SELECT * FROM timer_alert_profiles")
    fun getAllProfiles(): Flow<List<TimerAlertProfileEntity>>

    @Query("SELECT * FROM timer_alert_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String): TimerAlertProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfiles(vararg profiles: TimerAlertProfileEntity)

    @Query("DELETE FROM timer_alert_profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)
}
