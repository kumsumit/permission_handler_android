package com.baseflow.permissionhandler

import android.content.Context
import androidx.annotation.NonNull
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class MethodCallHandlerImpl(
    private val applicationContext: Context,
    private val appSettingsManager: AppSettingsManager,
    private val permissionManager: PermissionManager,
    private val serviceManager: ServiceManager
) : MethodChannel.MethodCallHandler {

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        when (call.method) {
            "checkServiceStatus" -> {
                val permission = call.arguments.toString().toInt()
                serviceManager.checkServiceStatus(
                    permission,
                    applicationContext,
                    result::success,
                    { errorCode, errorDescription -> result.error(errorCode, errorDescription, null) }
                )
            }

            "checkPermissionStatus" -> {
                val permission = call.arguments.toString().toInt()
                permissionManager.checkPermissionStatus(
                    permission,
                    result::success
                )
            }

             "requestPermissions" -> {
                 val permissions = call.arguments<List<Int>>() ?: emptyList()
                 permissionManager.requestPermissions(
                     permissions,
                     result::success,
                     { errorCode, errorDescription -> result.error(errorCode, errorDescription, null) }
                 )
             }

            "shouldShowRequestPermissionRationale" -> {
                val permission = call.arguments.toString().toInt()
                permissionManager.shouldShowRequestPermissionRationale(
                    permission,
                    result::success,
                    { errorCode, errorDescription -> result.error(errorCode, errorDescription, null) }
                )
            }

            "openAppSettings" -> {
                appSettingsManager.openAppSettings(
                    applicationContext,
                    result::success,
                    { errorCode, errorDescription -> result.error(errorCode, errorDescription, null) }
                )
            }

            else -> result.notImplemented()
        }
    }
}