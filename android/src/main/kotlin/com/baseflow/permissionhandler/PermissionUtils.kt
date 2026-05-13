package com.baseflow.permissionhandler

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.util.*

object PermissionUtils {
    const val SHARED_PREFERENCES_PERMISSION_WAS_DENIED_BEFORE_KEY = "sp_permission_handler_permission_was_denied_before"

    @PermissionConstants.PermissionGroup
    fun parseManifestName(permission: String): Int = when (permission) {
        Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR ->
            PermissionConstants.PERMISSION_GROUP_CALENDAR
        Manifest.permission.CAMERA -> PermissionConstants.PERMISSION_GROUP_CAMERA
        Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.GET_ACCOUNTS -> PermissionConstants.PERMISSION_GROUP_CONTACTS
        Manifest.permission.ACCESS_BACKGROUND_LOCATION ->
            PermissionConstants.PERMISSION_GROUP_LOCATION_ALWAYS
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION ->
            PermissionConstants.PERMISSION_GROUP_LOCATION
        Manifest.permission.RECORD_AUDIO ->
            PermissionConstants.PERMISSION_GROUP_MICROPHONE
        Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG, Manifest.permission.ADD_VOICEMAIL,
        Manifest.permission.USE_SIP -> PermissionConstants.PERMISSION_GROUP_PHONE
        Manifest.permission.BODY_SENSORS -> PermissionConstants.PERMISSION_GROUP_SENSORS
        Manifest.permission.BODY_SENSORS_BACKGROUND ->
            PermissionConstants.PERMISSION_GROUP_SENSORS_ALWAYS
        Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_WAP_PUSH,
        Manifest.permission.RECEIVE_MMS -> PermissionConstants.PERMISSION_GROUP_SMS
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE ->
            PermissionConstants.PERMISSION_GROUP_STORAGE
        Manifest.permission.ACCESS_MEDIA_LOCATION ->
            PermissionConstants.PERMISSION_GROUP_ACCESS_MEDIA_LOCATION
        Manifest.permission.ACTIVITY_RECOGNITION ->
            PermissionConstants.PERMISSION_GROUP_ACTIVITY_RECOGNITION
        Manifest.permission.MANAGE_EXTERNAL_STORAGE ->
            PermissionConstants.PERMISSION_GROUP_MANAGE_EXTERNAL_STORAGE
        Manifest.permission.SYSTEM_ALERT_WINDOW ->
            PermissionConstants.PERMISSION_GROUP_SYSTEM_ALERT_WINDOW
        Manifest.permission.REQUEST_INSTALL_PACKAGES ->
            PermissionConstants.PERMISSION_GROUP_REQUEST_INSTALL_PACKAGES
        Manifest.permission.ACCESS_NOTIFICATION_POLICY ->
            PermissionConstants.PERMISSION_GROUP_ACCESS_NOTIFICATION_POLICY
        Manifest.permission.BLUETOOTH_SCAN ->
            PermissionConstants.PERMISSION_GROUP_BLUETOOTH_SCAN
        Manifest.permission.BLUETOOTH_ADVERTISE ->
            PermissionConstants.PERMISSION_GROUP_BLUETOOTH_ADVERTISE
        Manifest.permission.BLUETOOTH_CONNECT ->
            PermissionConstants.PERMISSION_GROUP_BLUETOOTH_CONNECT
        Manifest.permission.POST_NOTIFICATIONS ->
            PermissionConstants.PERMISSION_GROUP_NOTIFICATION
        Manifest.permission.NEARBY_WIFI_DEVICES ->
            PermissionConstants.PERMISSION_GROUP_NEARBY_WIFI_DEVICES
        Manifest.permission.READ_MEDIA_IMAGES ->
            PermissionConstants.PERMISSION_GROUP_PHOTOS
        Manifest.permission.READ_MEDIA_VIDEO ->
            PermissionConstants.PERMISSION_GROUP_VIDEOS
        Manifest.permission.READ_MEDIA_AUDIO -> PermissionConstants.PERMISSION_GROUP_AUDIO
        Manifest.permission.SCHEDULE_EXACT_ALARM ->
            PermissionConstants.PERMISSION_GROUP_SCHEDULE_EXACT_ALARM
        else -> PermissionConstants.PERMISSION_GROUP_UNKNOWN
    }

    @TargetApi(22)
    fun getManifestNames(context: Context, @PermissionConstants.PermissionGroup permission: Int): List<String>? {
        val permissionNames = ArrayList<String>()

        when (permission) {
            PermissionConstants.PERMISSION_GROUP_CALENDAR_WRITE_ONLY -> {
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.WRITE_CALENDAR))
                    permissionNames.add(Manifest.permission.WRITE_CALENDAR)
            }

            PermissionConstants.PERMISSION_GROUP_CALENDAR_FULL_ACCESS,
            PermissionConstants.PERMISSION_GROUP_CALENDAR -> {
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.WRITE_CALENDAR))
                    permissionNames.add(Manifest.permission.WRITE_CALENDAR)
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.READ_CALENDAR))
                    permissionNames.add(Manifest.permission.READ_CALENDAR)
            }

            PermissionConstants.PERMISSION_GROUP_CAMERA -> {
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.CAMERA))
                    permissionNames.add(Manifest.permission.CAMERA)
            }

            PermissionConstants.PERMISSION_GROUP_CONTACTS -> {
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.READ_CONTACTS))
                    permissionNames.add(Manifest.permission.READ_CONTACTS)

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.WRITE_CONTACTS))
                    permissionNames.add(Manifest.permission.WRITE_CONTACTS)

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.GET_ACCOUNTS))
                    permissionNames.add(Manifest.permission.GET_ACCOUNTS)
            }

            PermissionConstants.PERMISSION_GROUP_LOCATION_ALWAYS,
            PermissionConstants.PERMISSION_GROUP_LOCATION_WHEN_IN_USE,
            PermissionConstants.PERMISSION_GROUP_LOCATION -> {
                // Note that the LOCATION_ALWAYS will deliberately fallthrough to the LOCATION
                // case on pre Android Q devices. The ACCESS_BACKGROUND_LOCATION permission was only
                // introduced in Android Q, before it should be treated as the ACCESS_COARSE_LOCATION or
                // ACCESS_FINE_LOCATION.
                if (permission == PermissionConstants.PERMISSION_GROUP_LOCATION_ALWAYS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (hasPermissionInManifest(context, permissionNames, Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                        permissionNames.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    return permissionNames
                }

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.ACCESS_COARSE_LOCATION))
                    permissionNames.add(Manifest.permission.ACCESS_COARSE_LOCATION)

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.ACCESS_FINE_LOCATION))
                    permissionNames.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            PermissionConstants.PERMISSION_GROUP_SPEECH,
            PermissionConstants.PERMISSION_GROUP_MICROPHONE -> {
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.RECORD_AUDIO))
                    permissionNames.add(Manifest.permission.RECORD_AUDIO)
            }

            PermissionConstants.PERMISSION_GROUP_PHONE -> {
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.READ_PHONE_STATE))
                    permissionNames.add(Manifest.permission.READ_PHONE_STATE)

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && hasPermissionInManifest(context, permissionNames, Manifest.permission.READ_PHONE_NUMBERS))
                    permissionNames.add(Manifest.permission.READ_PHONE_NUMBERS)

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.CALL_PHONE))
                    permissionNames.add(Manifest.permission.CALL_PHONE)

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.READ_CALL_LOG))
                    permissionNames.add(Manifest.permission.READ_CALL_LOG)

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.WRITE_CALL_LOG))
                    permissionNames.add(Manifest.permission.WRITE_CALL_LOG)

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.ADD_VOICEMAIL))
                    permissionNames.add(Manifest.permission.ADD_VOICEMAIL)

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.USE_SIP))
                    permissionNames.add(Manifest.permission.USE_SIP)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasPermissionInManifest(context, permissionNames, Manifest.permission.ANSWER_PHONE_CALLS))
                    permissionNames.add(Manifest.permission.ANSWER_PHONE_CALLS)
            }

            PermissionConstants.PERMISSION_GROUP_SENSORS -> {
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.BODY_SENSORS)) {
                    permissionNames.add(Manifest.permission.BODY_SENSORS)
                }
            }

            PermissionConstants.PERMISSION_GROUP_SENSORS_ALWAYS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (hasPermissionInManifest(context, permissionNames, Manifest.permission.BODY_SENSORS_BACKGROUND)) {
                        permissionNames.add(Manifest.permission.BODY_SENSORS_BACKGROUND)
                    }
                }
            }

            PermissionConstants.PERMISSION_GROUP_SMS -> {
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.SEND_SMS))
                    permissionNames.add(Manifest.permission.SEND_SMS)

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.RECEIVE_SMS))
                    permissionNames.add(Manifest.permission.RECEIVE_SMS)

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.READ_SMS))
                    permissionNames.add(Manifest.permission.READ_SMS)

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.RECEIVE_WAP_PUSH))
                    permissionNames.add(Manifest.permission.RECEIVE_WAP_PUSH)

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.RECEIVE_MMS))
                    permissionNames.add(Manifest.permission.RECEIVE_MMS)
            }

            PermissionConstants.PERMISSION_GROUP_STORAGE -> {
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.READ_EXTERNAL_STORAGE))
                    permissionNames.add(Manifest.permission.READ_EXTERNAL_STORAGE)

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && Environment.isExternalStorageLegacy())) {
                    if (hasPermissionInManifest(context, permissionNames, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        permissionNames.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }

            PermissionConstants.PERMISSION_GROUP_IGNORE_BATTERY_OPTIMIZATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasPermissionInManifest(context, permissionNames, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))
                    permissionNames.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            }

            PermissionConstants.PERMISSION_GROUP_ACCESS_MEDIA_LOCATION -> {
                // The ACCESS_MEDIA_LOCATION permission is introduced in Android Q, meaning we should
                // not handle permissions on pre Android Q devices.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.ACCESS_MEDIA_LOCATION))
                    permissionNames.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
            }

            PermissionConstants.PERMISSION_GROUP_ACTIVITY_RECOGNITION -> {
                // The ACTIVITY_RECOGNITION permission is introduced in Android Q, meaning we should
                // not handle permissions on pre Android Q devices.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.ACTIVITY_RECOGNITION))
                    permissionNames.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }

            PermissionConstants.PERMISSION_GROUP_BLUETOOTH -> {
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.BLUETOOTH))
                    permissionNames.add(Manifest.permission.BLUETOOTH)
            }

            PermissionConstants.PERMISSION_GROUP_MANAGE_EXTERNAL_STORAGE -> {
                // The MANAGE_EXTERNAL_STORAGE permission is introduced in Android R, meaning we should
                // not handle permissions on pre Android R devices.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasPermissionInManifest(context, permissionNames, Manifest.permission.MANAGE_EXTERNAL_STORAGE))
                    permissionNames.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            }

            PermissionConstants.PERMISSION_GROUP_SYSTEM_ALERT_WINDOW -> {
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.SYSTEM_ALERT_WINDOW))
                    permissionNames.add(Manifest.permission.SYSTEM_ALERT_WINDOW)
            }

            PermissionConstants.PERMISSION_GROUP_REQUEST_INSTALL_PACKAGES -> {
                // The REQUEST_INSTALL_PACKAGES permission is introduced in Android M, meaning we should
                // not handle permissions on pre Android M devices.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasPermissionInManifest(context, permissionNames, Manifest.permission.REQUEST_INSTALL_PACKAGES))
                    permissionNames.add(Manifest.permission.REQUEST_INSTALL_PACKAGES)
            }

            PermissionConstants.PERMISSION_GROUP_ACCESS_NOTIFICATION_POLICY -> {
                // The REQUEST_NOTIFICATION_POLICY permission is introduced in Android M, meaning we should
                // not handle permissions on pre Android M devices.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasPermissionInManifest(context, permissionNames, Manifest.permission.ACCESS_NOTIFICATION_POLICY))
                    permissionNames.add(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
            }

            PermissionConstants.PERMISSION_GROUP_BLUETOOTH_SCAN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // The BLUETOOTH_SCAN permission is introduced in Android S, meaning we should
                    // not handle permissions on pre Android S devices.
                    val result = determineBluetoothPermission(context, Manifest.permission.BLUETOOTH_SCAN)

                    if (result != null) {
                        permissionNames.add(result)
                    }
                }
            }

            PermissionConstants.PERMISSION_GROUP_BLUETOOTH_ADVERTISE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // The BLUETOOTH_ADVERTISE permission is introduced in Android S, meaning we should
                    // not handle permissions on pre Android S devices.
                    val result = determineBluetoothPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)

                    if (result != null) {
                        permissionNames.add(result)
                    }
                }
            }

            PermissionConstants.PERMISSION_GROUP_BLUETOOTH_CONNECT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // The BLUETOOTH_CONNECT permission is introduced in Android S, meaning we should
                    // not handle permissions on pre Android S devices.
                    val result = determineBluetoothPermission(context, Manifest.permission.BLUETOOTH_CONNECT)

                    if (result != null) {
                        permissionNames.add(result)
                    }
                }
            }

            PermissionConstants.PERMISSION_GROUP_NOTIFICATION -> {
                // The POST_NOTIFICATIONS permission is introduced in Android TIRAMISU, meaning we should
                // not handle permissions on pre Android TIRAMISU devices.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasPermissionInManifest(context, permissionNames, Manifest.permission.POST_NOTIFICATIONS))
                    permissionNames.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            PermissionConstants.PERMISSION_GROUP_NEARBY_WIFI_DEVICES -> {
                // The NEARBY_WIFI_DEVICES permission is introduced in Android TIRAMISU, meaning we should
                // not handle permissions on pre Android TIRAMISU devices.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasPermissionInManifest(context, permissionNames, Manifest.permission.NEARBY_WIFI_DEVICES))
                    permissionNames.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }

            PermissionConstants.PERMISSION_GROUP_PHOTOS -> {
                // The READ_MEDIA_IMAGES permission is introduced in Android TIRAMISU, meaning we should
                // not handle permissions on pre Android TIRAMISU devices.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasPermissionInManifest(context, permissionNames, Manifest.permission.READ_MEDIA_IMAGES))
                    permissionNames.add(Manifest.permission.READ_MEDIA_IMAGES)
            }

            PermissionConstants.PERMISSION_GROUP_VIDEOS -> {
                // The READ_MEDIA_VIDEOS permission is introduced in Android TIRAMISU, meaning we should
                // not handle permissions on pre Android TIRAMISU devices.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasPermissionInManifest(context, permissionNames, Manifest.permission.READ_MEDIA_VIDEO))
                    permissionNames.add(Manifest.permission.READ_MEDIA_VIDEO)
            }

            PermissionConstants.PERMISSION_GROUP_AUDIO -> {
                // The READ_MEDIA_AUDIO permission is introduced in Android TIRAMISU, meaning we should
                // not handle permissions on pre Android TIRAMISU devices.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && hasPermissionInManifest(context, permissionNames, Manifest.permission.READ_MEDIA_AUDIO))
                    permissionNames.add(Manifest.permission.READ_MEDIA_AUDIO)
            }

            PermissionConstants.PERMISSION_GROUP_SCHEDULE_EXACT_ALARM -> {
                // The SCHEDULE_EXACT_ALARM permission is introduced in Android S, before Android 31 it should alway return Granted
                if (hasPermissionInManifest(context, permissionNames, Manifest.permission.SCHEDULE_EXACT_ALARM))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissionNames.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
                    }
            }

            PermissionConstants.PERMISSION_GROUP_MEDIA_LIBRARY,
            PermissionConstants.PERMISSION_GROUP_REMINDERS,
            PermissionConstants.PERMISSION_GROUP_UNKNOWN -> return null

            PermissionConstants.PERMISSION_GROUP_ASSISTANT -> {}
        }

        return permissionNames
    }

    private fun hasPermissionInManifest(context: Context, confirmedPermissions: ArrayList<String>, permission: String): Boolean {
        try {
            if (confirmedPermissions != null) {
                for (r in confirmedPermissions) {
                    if (r == permission) {
                        return true
                    }
                }
            }

            if (context == null) {
                Log.d(PermissionConstants.LOG_TAG, "Unable to detect current Activity or App Context.")
                return false
            }

            val info = getPackageInfo(context)

            if (info == null) {
                Log.d(PermissionConstants.LOG_TAG, "Unable to get Package info, will not be able to determine permissions to request.")
                return false
            }

            val requestedPermissions = info.requestedPermissions ?: return false
            confirmedPermissions.addAll(Arrays.asList(*requestedPermissions))
            for (r in confirmedPermissions) {
                if (r == permission) {
                    return true
                }
            }
        } catch (ex: Exception) {
            Log.d(PermissionConstants.LOG_TAG, "Unable to check manifest for permission: ", ex)
        }
        return false
    }

    /**
     * Returns a [PermissionConstants] for a given permission.
     *
     * When [PackageManager.PERMISSION_DENIED] is received, we do not know if the permission was
     * denied permanently. The OS does not tell us whether the user dismissed the dialog or pressed
     * 'deny'. Therefore, we need a more sophisticated (read: hacky) approach to determine whether the
     * permission status is [PermissionConstants.PERMISSION_STATUS_DENIED] or
     * [PermissionConstants.PERMISSION_STATUS_NEVER_ASK_AGAIN].
     *
     * The OS behavior has been researched experimentally and is displayed in the following diagrams:
     *
     * State machine diagram:
     *
     * Dismissed
     *    ┌┐
     * ┌──┘▼─────┐  Granted ┌───────┐
     * │Not asked├──────────►Granted│
     * └─┬───────┘          └─▲─────┘
     *   │           Granted  │
     *   │Denied  ┌───────────┘
     *   │        │
     * ┌─▼────────┴┐        ┌────────────────────────────────┐
     * │Denied once├────────►Denied twice(permanently denied)│
     * └──▲┌───────┘ Denied └────────────────────────────────┘
     *    └┘
     * Dismissed
     *
     * Scenario table listing output of
     * [ActivityCompat.shouldShowRequestPermissionRationale(Activity, String)]:
     * ┌────────────┬────────────────┬─────────┬───────────────────────────────────┬─────────────────────────┐
     * │ Scenario # │ Previous state │ Action  │ New state                         │ 'Show rationale' output │
     * ├────────────┼────────────────┼─────────┼───────────────────────────────────┼─────────────────────────┤
     * │ 1.         │ Not asked      │ Dismiss │ Not asked                         │ false                   │
     * │ 2.         │ Not asked      │ Deny    │ Denied once                       │ true                    │
     * │ 3.         │ Denied once    │ Dismiss │ Denied once                       │ true                    │
     * │ 4.         │ Denied once    │ Deny    │ Denied twice (permanently denied) │ false                   │
     * └────────────┴────────────────┴─────────┴───────────────────────────────────┴─────────────────────────┘
     *
     * To distinguish between scenarios, we can use
     * [ActivityCompat.shouldShowRequestPermissionRationale(Activity, String)]. If it returns
     * true, we can safely return [PermissionConstants.PERMISSION_STATUS_DENIED]. To distinguish
     * between scenarios 1 and 4, however, we need an extra mechanism. We opt to store a boolean
     * stating whether permission has been requested before. Using a combination of checking for
     * showing the permission rationale and the boolean, we can distinguish all scenarios and return
     * the appropriate permission status.
     *
     * Changing permissions via the app info screen, so outside of the application, changes the
     * permission state to 'Granted' if the permission is allowed, or 'Denied once' if denied. This
     * behavior should not require any additional logic.
     *
     * @param activity       the activity for context
     * @param permissionName the name of the permission
     * @param grantResult    the result of the permission intent. Either
     *                       [PackageManager.PERMISSION_DENIED] or [PackageManager.PERMISSION_GRANTED].
     * @return [PermissionConstants.PERMISSION_STATUS_GRANTED],
     * [PermissionConstants.PERMISSION_STATUS_DENIED], or
     * [PermissionConstants.PERMISSION_STATUS_NEVER_ASK_AGAIN].
     */
    @PermissionConstants.PermissionStatus
    fun toPermissionStatus(
        activity: Activity?,
        permissionName: String,
        grantResult: Int
    ): Int {
        return if (grantResult == PackageManager.PERMISSION_DENIED) {
            determineDeniedVariant(activity, permissionName)
        } else {
            PermissionConstants.PERMISSION_STATUS_GRANTED
        }
    }

    @NonNull
    @PermissionConstants.PermissionStatus
    fun strictestStatus(statuses: Collection<@PermissionConstants.PermissionStatus Int>): Int {
        if (statuses.contains(PermissionConstants.PERMISSION_STATUS_NEVER_ASK_AGAIN))
            return PermissionConstants.PERMISSION_STATUS_NEVER_ASK_AGAIN
        if (statuses.contains(PermissionConstants.PERMISSION_STATUS_RESTRICTED))
            return PermissionConstants.PERMISSION_STATUS_RESTRICTED
        if (statuses.contains(PermissionConstants.PERMISSION_STATUS_DENIED))
            return PermissionConstants.PERMISSION_STATUS_DENIED
        if (statuses.contains(PermissionConstants.PERMISSION_STATUS_LIMITED))
            return PermissionConstants.PERMISSION_STATUS_LIMITED
        return PermissionConstants.PERMISSION_STATUS_GRANTED
    }

    @NonNull
    @PermissionConstants.PermissionStatus
    fun strictestStatus(
        statusA: @PermissionConstants.PermissionStatus Int?,
        statusB: @PermissionConstants.PermissionStatus Int?
    ): Int {
        val statuses: MutableCollection<@PermissionConstants.PermissionStatus Int> = HashSet()
        if (statusA != null) statuses.add(statusA)
        if (statusB != null) statuses.add(statusB)
        return strictestStatus(statuses)
    }

    /**
     * Determines whether a permission was either 'denied' or 'permanently denied'.
     *
     * To distinguish between these two variants, the method needs access to an [Activity].
     * If the provided activity is null, the result will always be resolved to 'denied'.
     *
     * @param activity       the activity needed to resolve the permission status.
     * @param permissionName the name of the permission.
     * @return either [PermissionConstants.PERMISSION_STATUS_DENIED] or
     * [PermissionConstants.PERMISSION_STATUS_NEVER_ASK_AGAIN].
     */
    @PermissionConstants.PermissionStatus
    fun determineDeniedVariant(
        activity: Activity?,
        permissionName: String
    ): Int {
        if (activity == null) {
            return PermissionConstants.PERMISSION_STATUS_DENIED
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return PermissionConstants.PERMISSION_STATUS_DENIED
        }

        val wasDeniedBefore = wasPermissionDeniedBefore(activity, permissionName)
        val shouldShowRational = !isNeverAskAgainSelected(activity, permissionName)

        val isDenied = if (wasDeniedBefore) !shouldShowRational else shouldShowRational

        if (!wasDeniedBefore && isDenied) {
            setPermissionDenied(activity, permissionName)
        }

        return if (wasDeniedBefore && isDenied) {
            PermissionConstants.PERMISSION_STATUS_NEVER_ASK_AGAIN
        } else {
            PermissionConstants.PERMISSION_STATUS_DENIED
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun isNeverAskAgainSelected(
        activity: Activity,
        name: String
    ): Boolean {
        val shouldShowRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, name)
        return !shouldShowRequestPermissionRationale
    }

    private fun determineBluetoothPermission(context: Context, permission: String): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hasPermissionInManifest(context, null, permission)) {
            permission
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (hasPermissionInManifest(context, null, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Manifest.permission.ACCESS_FINE_LOCATION
            } else if (hasPermissionInManifest(context, null, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Manifest.permission.ACCESS_COARSE_LOCATION
            } else {
                null
            }
        } else {
            if (hasPermissionInManifest(context, null, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Manifest.permission.ACCESS_FINE_LOCATION
            } else {
                null
            }
        }
    }

    // Suppress deprecation warnings since its purpose is to support to be backwards compatible with
    // pre TIRAMISU versions of Android
    private fun getPackageInfo(context: Context): PackageInfo? {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        } else {
            @Suppress("DEPRECATION")
            try {
                pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    /**
     * Checks if permission was denied in the past by reading
     * [SHARED_PREFERENCES_PERMISSION_WAS_DENIED_BEFORE_KEY] from
     * [SharedPreferences].
     *
     * Because the state is red from shared preferences, it is persistent across application
     * sessions.
     *
     * @param context        context needed for accessing shared preferences
     * @param permissionName the name of the permission
     * @return whether the permission was denied in the past
     */
    private fun wasPermissionDeniedBefore(context: Context, permissionName: String): Boolean {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(permissionName, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(SHARED_PREFERENCES_PERMISSION_WAS_DENIED_BEFORE_KEY, false)
    }

    /**
     * Stores a boolean in [SharedPreferences] indicating the provided permission has been
     * denied.
     *
     * Because the state is stored in shared preferences, it is persistent across application
     * sessions.
     *
     * @param context        context needed for accessing shared preferences.
     * @param permissionName the name of the permission
     */
    private fun setPermissionDenied(context: Context, permissionName: String) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(permissionName, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(SHARED_PREFERENCES_PERMISSION_WAS_DENIED_BEFORE_KEY, true).apply()
    }
}