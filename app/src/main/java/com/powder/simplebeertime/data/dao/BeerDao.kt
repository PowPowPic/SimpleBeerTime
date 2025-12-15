package com.powder.simplebeertime.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import com.powder.simplebeertime.data.entity.BeerRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BeerDao {

    @Insert
    suspend fun insert(record: BeerRecord)

    @Delete
    suspend fun delete(record: BeerRecord)

    @Query("DELETE FROM beer_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM beer_records ORDER BY timestamp ASC")
    fun getAllRecords(): Flow<List<BeerRecord>>

    @Query("""
        SELECT * FROM beer_records 
        WHERE timestamp BETWEEN :start AND :end 
        ORDER BY timestamp ASC
    """)
    suspend fun getRecordsBetween(start: Long, end: Long): List<BeerRecord>

    @Query("SELECT * FROM beer_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRecord(): BeerRecord?

    @Query("DELETE FROM beer_records")
    suspend fun deleteAll()

    @Query("DELETE FROM beer_records WHERE timestamp = :timestamp")
    suspend fun deleteByTimestamp(timestamp: Long)
}