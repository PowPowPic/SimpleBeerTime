package com.powder.simplebeertime.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "beer_records")
data class BeerRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long // 飲んだ時刻のミリ秒
)