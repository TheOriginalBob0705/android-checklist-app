package com.example.checklist

import android.app.Application
import com.example.checklist.data.AppDatabase

class App : Application() {
    val db by lazy { AppDatabase.get(this) }
}