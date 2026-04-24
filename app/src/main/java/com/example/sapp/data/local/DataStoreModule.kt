package com.example.sapp.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// Define DataStore once, globally
val Context.dataStore by preferencesDataStore(name = "settings")
