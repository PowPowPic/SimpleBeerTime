package com.powder.simplebeertime.data.repository

import com.powder.simplebeertime.data.dao.BeerDao
import com.powder.simplebeertime.data.entity.BeerRecord
import kotlinx.coroutines.flow.Flow

class BeerRepository(private val beerDao: BeerDao) {

    val allRecords: Flow<List<BeerRecord>> = beerDao.getAllRecords()

    val latestRecord: Flow<BeerRecord?> = beerDao.getLatestRecord()

    suspend fun insert(record: BeerRecord) {
        beerDao.insert(record)
    }

    suspend fun deleteById(id: Long) {
        beerDao.deleteById(id)
    }

    suspend fun deleteByTimestamp(timestamp: Long) {
        beerDao.deleteByTimestamp(timestamp)
    }

    suspend fun deleteAll() {
        beerDao.deleteAll()
    }

    suspend fun getTotalAmountSince(startTime: Long): Double {
        return beerDao.getTotalAmountSince(startTime)
    }

    suspend fun getTotalAmountBetween(startTime: Long, endTime: Long): Double {
        return beerDao.getTotalAmountBetween(startTime, endTime)
    }
    suspend fun deleteByTimestampRange(fromMillis: Long, toMillis: Long) {
        beerDao.deleteByTimestampRange(fromMillis, toMillis)
    }
}