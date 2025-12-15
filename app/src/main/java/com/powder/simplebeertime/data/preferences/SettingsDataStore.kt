package com.powder.simplebeertime.data.preferences

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore by preferencesDataStore(name = "simple_beer_time_settings")