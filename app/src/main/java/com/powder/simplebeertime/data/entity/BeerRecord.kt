package com.powder.simplebeertime.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "beer_records")
data class BeerRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val timestamp: Long,

    val amount: Double = 1.0
)