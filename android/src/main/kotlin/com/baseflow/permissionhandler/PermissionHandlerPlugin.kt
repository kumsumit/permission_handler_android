package com.baseflow.permissionhandler

import android.app.Activity
import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel

/**
 * Platform implementation of the permission_handler Flutter plugin.
 *
 * Instantiate this in an add-to-app scenario to gracefully handle activity and context changes.
 * See `com.example.permissionhandlerexample.MainActivity` for an example.
 */
class PermissionHandlerPlugin : FlutterPlugin, ActivityAware {
    private var permissionManager: PermissionManager? = null
    private var methodChannel: MethodChannel? = null
    private var pluginBinding: ActivityPluginBinding? = null
    private var methodCallHandler: MethodCallHandlerImpl? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        permissionManager = PermissionManager(binding.applicationContext)
        startListening(
            binding.applicationContext,
            binding.binaryMessenger
        )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        stopListening()
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        startListeningToActivity(binding.activity)
        pluginBinding = binding
        registerListeners()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        stopListeningToActivity()
        deregisterListeners()
        pluginBinding = null
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    private fun startListening(applicationContext: Context, messenger: BinaryMessenger) {
        methodChannel = MethodChannel(
            messenger,
            "flutter.baseflow.com/permissions/methods"
        )
        methodCallHandler = MethodCallHandlerImpl(
            applicationContext,
            AppSettingsManager(),
            permissionManager!!,
            ServiceManager()
        )
        methodChannel!!.setMethodCallHandler(methodCallHandler)
    }

    private fun stopListening() {
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
        methodCallHandler = null
    }

    private fun startListeningToActivity(activity: Activity) {
        permissionManager?.setActivity(activity)
    }

    private fun stopListeningToActivity() {
        permissionManager?.setActivity(null)
    }

    private fun registerListeners() {
        pluginBinding?.let {
            it.addActivityResultListener(permissionManager!!)
            it.addRequestPermissionsResultListener(permissionManager!!)
        }
    }

    private fun deregisterListeners() {
        pluginBinding?.let {
            it.removeActivityResultListener(permissionManager!!)
            it.removeRequestPermissionsResultListener(permissionManager!!)
        }
    }
}