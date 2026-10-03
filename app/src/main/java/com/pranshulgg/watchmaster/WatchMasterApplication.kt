package com.pranshulgg.watchmaster

import android.app.Application
import coil.Coil
import com.pranshulgg.watchmaster.core.prefs.AppPrefs
import com.pranshulgg.watchmaster.core.utils.PreferencesHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WatchMasterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPrefs.initPrefs(this)

        // Remove the old persistent Coil artwork cache once when upgrading to
        // Pickxo's default Online image mode.
        if (AppPrefs.isOnlineImageMode() &&
            PreferencesHelper.getBool("online_image_cleanup_v1") != true
        ) {
            runCatching { Coil.imageLoader(this).diskCache?.clear() }
            PreferencesHelper.setBool("online_image_cleanup_v1", true)
        }
    }
}
