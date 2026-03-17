package com.sofastream.app

import android.app.Application
import com.sofastream.app.data.preferences.UserPreferences

class SofaStreamApp : Application() {

    lateinit var userPreferences: UserPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        userPreferences = UserPreferences(this)
    }

    companion object {
        lateinit var instance: SofaStreamApp
            private set
    }
}
