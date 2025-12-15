package com.powder.simplebeertime.data.repository

import com.powder.simplebeertime.data.dao.BeerDao
import com.powder.simplebeertime.data.entity.BeerRecord
import kotlinx.coroutines.flow.Flow

class BeerRepository(
    private val dao: BeerDao
) {

    fun getAllRecords(): Flow<List<BeerRecord>> =
        dao.getAllRecords()

    suspend fun insertRecord(timestamp: Long = System.currentTimeMillis()) {
        dao.insert(BeerRecord(timestamp = timestamp))
    }

    suspend fun deleteRecord(id: Long) {
        dao.deleteById(id)
    }

    suspend fun getLatestRecord(): BeerRecord? {
        return dao.getLatestRecord()
    }

    suspend fun getRecordsBetween(start: Long, end: Long): List<BeerRecord> {
        return dao.getRecordsBetween(start, end)
    }

    suspend fun deleteAllRecords() {
        dao.deleteAll()
    }

    suspend fun deleteRecordByTimestamp(timestamp: Long) {
        dao.deleteByTimestamp(timestamp)
    }
}