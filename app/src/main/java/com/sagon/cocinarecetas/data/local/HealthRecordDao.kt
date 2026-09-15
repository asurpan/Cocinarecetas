package com.sagon.cocinarecetas.data.local

import androidx.room.*
import com.sagon.cocinarecetas.data.model.HealthRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthRecordDao {
    @Query("SELECT * FROM health_records ORDER BY date ASC")
    fun getAllRecords(): Flow<List<HealthRecord>>

    @Query("SELECT * FROM health_records WHERE date >= :since ORDER BY date ASC")
    fun getRecordsSince(since: Long): Flow<List<HealthRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HealthRecord)

    @Query("SELECT * FROM health_records WHERE date = :date")
    suspend fun getRecordByDate(date: Long): HealthRecord?

    @Query("DELETE FROM health_records")
    suspend fun deleteAllRecords()
}
