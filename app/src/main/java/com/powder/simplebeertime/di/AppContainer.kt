package com.powder.simplebeertime.di

import android.content.Context
import androidx.room.Room
import com.powder.simplebeertime.data.database.AppDatabase
import com.powder.simplebeertime.data.repository.BeerRepository

class AppContainer(context: Context) {

    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "beer_database"
    ).build()

    private val beerDao = database.beerDao()

    val beerRepository = BeerRepository(beerDao)
}