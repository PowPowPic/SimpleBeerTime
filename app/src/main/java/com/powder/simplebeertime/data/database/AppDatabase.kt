package com.powder.simplebeertime.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.powder.simplebeertime.data.dao.BeerDao
import com.powder.simplebeertime.data.entity.BeerRecord

@Database(
    entities = [BeerRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun beerDao(): BeerDao
}