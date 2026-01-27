package com.banyg

import android.app.Application
import com.banyg.seed.AppSeeder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Banyg Application class.
 *
 * Entry point for the app with Hilt dependency injection.
 * Seeds default data on first launch.
 */
@HiltAndroidApp
class BanygApplication : Application() {

    @Inject
    lateinit var appSeeder: AppSeeder

    override fun onCreate() {
        super.onCreate()

        // Seed default data (categories, etc.) in background
        // Safe to call on every launch - checks for existing data
        appSeeder.seedOnStartup()
    }
}
