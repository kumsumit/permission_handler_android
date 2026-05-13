package com.baseflow.permissionhandler

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri

class AppSettingsManager {
    fun interface OpenAppSettingsSuccessCallback {
        fun onSuccess(appSettingsOpenedSuccessfully: Boolean)
    }

    fun openAppSettings(
        context: Context,
        successCallback: OpenAppSettingsSuccessCallback,
        errorCallback: ErrorCallback
    ) {

        try {
            val settingsIntent = Intent()
            settingsIntent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            settingsIntent.addCategory(Intent.CATEGORY_DEFAULT)
            settingsIntent.data = ("package:" + context.packageName).toUri()
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

            context.startActivity(settingsIntent)

            successCallback.onSuccess(true)
        } catch (ex: Exception) {
            successCallback.onSuccess(false)
        }
    }
}