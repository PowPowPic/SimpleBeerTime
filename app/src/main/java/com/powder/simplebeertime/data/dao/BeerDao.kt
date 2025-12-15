package com.powder.simplebeertime.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.powder.simplebeertime.data.entity.BeerRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BeerDao {

    @Insert
    suspend fun insert(record: BeerRecord)

    @Query("SELECT * FROM beer_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<BeerRecord>>

    @Query("SELECT * FROM beer_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRecord(): Flow<BeerRecord?>

    @Query("DELETE FROM beer_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM beer_records WHERE timestamp = :timestamp")
    suspend fun deleteByTimestamp(timestamp: Long)

    @Query("DELETE FROM beer_records")
    suspend fun deleteAll()

    // SUM(amount) で合計本数を取得
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM beer_records WHERE timestamp >= :startTime")
    suspend fun getTotalAmountSince(startTime: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM beer_records WHERE timestamp >= :startTime AND timestamp < :endTime")
    suspend fun getTotalAmountBetween(startTime: Long, endTime: Long): Double
}