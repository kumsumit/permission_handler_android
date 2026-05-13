package com.baseflow.permissionhandler

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi

class ServiceManager {
    fun interface SuccessCallback {
        fun onSuccess(@PermissionConstants.ServiceStatus serviceStatus: Int)
    }

    fun checkServiceStatus(
        permission: Int,
        context: Context?,
        successCallback: SuccessCallback,
        errorCallback: ErrorCallback
    ) {
        if (context == null) {
            Log.d(PermissionConstants.LOG_TAG, "Context cannot be null.")
            errorCallback.onError("PermissionHandler.ServiceManager", "Android context cannot be null.")
            return
        }

        when (permission) {
            PermissionConstants.PERMISSION_GROUP_LOCATION,
            PermissionConstants.PERMISSION_GROUP_LOCATION_ALWAYS,
            PermissionConstants.PERMISSION_GROUP_LOCATION_WHEN_IN_USE -> {
                val serviceStatus = if (isLocationServiceEnabled(context))
                    PermissionConstants.SERVICE_STATUS_ENABLED
                else
                    PermissionConstants.SERVICE_STATUS_DISABLED

                successCallback.onSuccess(serviceStatus)
                return
            }

            PermissionConstants.PERMISSION_GROUP_BLUETOOTH -> {
                val serviceStatus = if (isBluetoothServiceEnabled(context))
                    PermissionConstants.SERVICE_STATUS_ENABLED
                else
                    PermissionConstants.SERVICE_STATUS_DISABLED

                successCallback.onSuccess(serviceStatus)
                return
            }

            PermissionConstants.PERMISSION_GROUP_PHONE -> {
                val pm = context.packageManager
                if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                    successCallback.onSuccess(PermissionConstants.SERVICE_STATUS_NOT_APPLICABLE)
                    return
                }

                val telephonyManager = context
                    .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

                if (telephonyManager == null || telephonyManager.phoneType == TelephonyManager.PHONE_TYPE_NONE) {
                    successCallback.onSuccess(PermissionConstants.SERVICE_STATUS_NOT_APPLICABLE)
                    return
                }

                val callAppsList = getCallAppsList(pm)

                if (callAppsList.isEmpty()) {
                    successCallback.onSuccess(PermissionConstants.SERVICE_STATUS_NOT_APPLICABLE)
                    return
                }

                if (telephonyManager.simState != TelephonyManager.SIM_STATE_READY) {
                    successCallback.onSuccess(PermissionConstants.SERVICE_STATUS_DISABLED)
                    return
                }

                successCallback.onSuccess(PermissionConstants.SERVICE_STATUS_ENABLED)
                return
            }

            PermissionConstants.PERMISSION_GROUP_IGNORE_BATTERY_OPTIMIZATIONS -> {
                val serviceStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PermissionConstants.SERVICE_STATUS_ENABLED
                else
                    PermissionConstants.SERVICE_STATUS_NOT_APPLICABLE
                successCallback.onSuccess(serviceStatus)
                return
            }
        }

        successCallback.onSuccess(PermissionConstants.SERVICE_STATUS_NOT_APPLICABLE)
    }

    private fun isLocationServiceEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val locationManager = context.getSystemService(LocationManager::class.java)
            locationManager?.isLocationEnabled ?: false
        } else {
            isLocationServiceEnabledKitKat(context)
        }
    }

    // Suppress deprecation warnings since its purpose is to support to be backwards compatible with
    // pre Pie versions of Android.
    private fun isLocationServiceEnabledKitKat(context: Context): Boolean {
        val locationMode: Int = try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE
            )
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            return false
        }

        return locationMode != Settings.Secure.LOCATION_MODE_OFF
    }

    // Suppress deprecation warnings since its purpose is to support to be backwards compatible with
    // pre S versions of Android
    private fun isBluetoothServiceEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        return adapter.isEnabled
    }

    // Suppress deprecation warnings since its purpose is to support to be backwards compatible with
    // pre TIRAMISU versions of Android
    private fun getCallAppsList(pm: PackageManager): List<ResolveInfo> {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:123123")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(callIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(callIntent, 0)
        }
    }
}