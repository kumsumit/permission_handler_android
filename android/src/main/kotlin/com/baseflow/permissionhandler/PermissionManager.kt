package com.baseflow.permissionhandler

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.flutter.plugin.common.PluginRegistry
import java.util.*

class PermissionManager(
    @NonNull private val context: Context
) : PluginRegistry.ActivityResultListener, PluginRegistry.RequestPermissionsResultListener {
    @Nullable
    private var successCallback: RequestPermissionsSuccessCallback? = null
    @Nullable
    private var activity: Activity? = null
    /**
     * The number of pending permission requests.
     *
     * This number is set by [this.requestPermissions] and then reduced when receiving results in
     * [this.onActivityResult] and [this.onRequestPermissionsResult].
     */
    private var pendingRequestCount = 0
    /**
     * The results of resolved permission requests.
     *
     * This map holds the results to resolved permission requests received through
     * [this.onActivityResult] and [this.onRequestPermissionsResult].
     * It is (re)initialized when new permissions are requested through
     * [this.requestPermissions].
     */
    private var requestResults: MutableMap<Int, Int>? = null

    fun setActivity(@Nullable activity: Activity?) {
        this.activity = activity
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (activity == null) {
            return false
        }

        // The [onActivityResult] with a [requestResult] that is `null` when the Application was
        // terminated while not in the foreground with a permission request in progress (e.g. when
        // Android decides to kill apps while requesting one of the special permissions). In these
        // cases we should casually return and make sure the `pendingRequestCount` is set to `0`.
        if (requestResults == null) {
            pendingRequestCount = 0
            return false
        }

        var status = PermissionConstants.PERMISSION_STATUS_DENIED
        var permission = PermissionConstants.PERMISSION_GROUP_UNKNOWN

        when (requestCode) {
            PermissionConstants.PERMISSION_CODE_IGNORE_BATTERY_OPTIMIZATIONS -> {
                permission = PermissionConstants.PERMISSION_GROUP_IGNORE_BATTERY_OPTIMIZATIONS

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val packageName = context.packageName
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    status = if (pm != null && pm.isIgnoringBatteryOptimizations(packageName))
                        PermissionConstants.PERMISSION_STATUS_GRANTED
                    else
                        PermissionConstants.PERMISSION_STATUS_DENIED
                } else {
                    status = PermissionConstants.PERMISSION_STATUS_RESTRICTED
                }
            }

            PermissionConstants.PERMISSION_CODE_MANAGE_EXTERNAL_STORAGE -> {
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                     status = if (Environment.isExternalStorageManager())
                         PermissionConstants.PERMISSION_STATUS_GRANTED
                     else
                         PermissionConstants.PERMISSION_STATUS_DENIED
                } else {
                    return false
                }
                permission = PermissionConstants.PERMISSION_GROUP_MANAGE_EXTERNAL_STORAGE
            }

            PermissionConstants.PERMISSION_CODE_SYSTEM_ALERT_WINDOW -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    status = if (Settings.canDrawOverlays(activity))
                        PermissionConstants.PERMISSION_STATUS_GRANTED
                    else
                        PermissionConstants.PERMISSION_STATUS_DENIED
                    permission = PermissionConstants.PERMISSION_GROUP_SYSTEM_ALERT_WINDOW
                } else {
                    return false
                }
            }

            PermissionConstants.PERMISSION_CODE_REQUEST_INSTALL_PACKAGES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    status = if (activity.packageManager.canRequestPackageInstalls())
                        PermissionConstants.PERMISSION_STATUS_GRANTED
                    else
                        PermissionConstants.PERMISSION_STATUS_DENIED
                    permission = PermissionConstants.PERMISSION_GROUP_REQUEST_INSTALL_PACKAGES
                } else {
                    return false
                }
            }

            PermissionConstants.PERMISSION_CODE_ACCESS_NOTIFICATION_POLICY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val notificationManager = activity.getSystemService(Application.NOTIFICATION_SERVICE) as NotificationManager
                    if (notificationManager != null) {
                        status = if (notificationManager.isNotificationPolicyAccessGranted)
                            PermissionConstants.PERMISSION_STATUS_GRANTED
                        else
                            PermissionConstants.PERMISSION_STATUS_DENIED
                    }
                    permission = PermissionConstants.PERMISSION_GROUP_ACCESS_NOTIFICATION_POLICY
                } else {
                    return false
                }
            }

            PermissionConstants.PERMISSION_CODE_SCHEDULE_EXACT_ALARM -> {
                permission = PermissionConstants.PERMISSION_GROUP_SCHEDULE_EXACT_ALARM
                val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (alarmManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    status = if (alarmManager.canScheduleExactAlarms())
                        PermissionConstants.PERMISSION_STATUS_GRANTED
                    else
                        PermissionConstants.PERMISSION_STATUS_DENIED
                } else {
                    status = PermissionConstants.PERMISSION_STATUS_GRANTED
                }
            }
        }

        requestResults!![permission] = status
        pendingRequestCount--

        // Post result if all requests have been handled.
        if (successCallback != null && pendingRequestCount == 0) {
            successCallback!!.onSuccess(requestResults!!)
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        @NonNull permissions: Array<String>,
        @NonNull grantResults: IntArray
    ): Boolean {
        if (requestCode != PermissionConstants.PERMISSION_CODE) {
            pendingRequestCount = 0
            return false
        }

        if (requestResults == null) {
            return false
        }

        if (permissions.isEmpty() && grantResults.isEmpty()) {
            Log.w(
                PermissionConstants.LOG_TAG,
                "onRequestPermissionsResult is called without results. This is probably caused by interfering request codes. If you see this error, please file an issue in flutter-permission-handler, including a list of plugins used by this application: https://github.com/Baseflow/flutter-permission-handler/issues"
            )
            return false
        }

        // Calendar permissions are split between WRITE and READ in Android, and split between WRITE
        // and FULL ACCESS in the plugin. We need special logic for this translation.
        val permissionList = Arrays.asList(*permissions)
        val calendarWriteIndex = permissionList.indexOf(Manifest.permission.WRITE_CALENDAR)
        // WRITE -> WRITE.
        if (calendarWriteIndex >= 0) {
            val writeGrantResult = grantResults[calendarWriteIndex]
            val writeStatus = PermissionUtils.toPermissionStatus(
                activity,
                Manifest.permission.WRITE_CALENDAR,
                writeGrantResult
            )
            requestResults!![PermissionConstants.PERMISSION_GROUP_CALENDAR_WRITE_ONLY] = writeStatus

            // WRITE + READ -> FULL ACCESS.
            val calendarReadIndex = permissionList.indexOf(Manifest.permission.READ_CALENDAR)
            if (calendarReadIndex >= 0) {
                val readGrantResult = grantResults[calendarReadIndex]
                val readStatus = PermissionUtils.toPermissionStatus(
                    activity,
                    Manifest.permission.READ_CALENDAR,
                    readGrantResult
                )
                val fullAccessStatus = PermissionUtils.strictestStatus(writeStatus, readStatus)
                requestResults!![PermissionConstants.PERMISSION_GROUP_CALENDAR_FULL_ACCESS] = fullAccessStatus
                // Support deprecated CALENDAR permission.
                requestResults!![PermissionConstants.PERMISSION_GROUP_CALENDAR] = fullAccessStatus
            }
        }

        for (i in permissions.indices) {
            val permissionName = permissions[i]

            // WRITE_CALENDAR and READ_CALENDAR permission results have already been handled.
            if (permissionName == Manifest.permission.WRITE_CALENDAR || permissionName == Manifest.permission.READ_CALENDAR) {
                continue
            }

            val permission = PermissionUtils.parseManifestName(permissionName)

            if (permission == PermissionConstants.PERMISSION_GROUP_UNKNOWN) continue

            val result = grantResults[i]

            if (permission == PermissionConstants.PERMISSION_GROUP_PHONE) {
                val previousResult = requestResults!![PermissionConstants.PERMISSION_GROUP_PHONE]
                val newResult = PermissionUtils.toPermissionStatus(activity, permissionName, result)
                val strictestStatus = PermissionUtils.strictestStatus(previousResult, newResult)
                requestResults!![PermissionConstants.PERMISSION_GROUP_PHONE] = strictestStatus
            } else if (permission == PermissionConstants.PERMISSION_GROUP_MICROPHONE) {
                if (!requestResults!!.containsKey(PermissionConstants.PERMISSION_GROUP_MICROPHONE)) {
                    requestResults!![PermissionConstants.PERMISSION_GROUP_MICROPHONE] =
                        PermissionUtils.toPermissionStatus(activity, permissionName, result)
                }
                if (!requestResults!!.containsKey(PermissionConstants.PERMISSION_GROUP_SPEECH)) {
                    requestResults!![PermissionConstants.PERMISSION_GROUP_SPEECH] =
                        PermissionUtils.toPermissionStatus(activity, permissionName, result)
                }
            } else if (permission == PermissionConstants.PERMISSION_GROUP_LOCATION_ALWAYS) {
                val permissionStatus = PermissionUtils.toPermissionStatus(activity, permissionName, result)

                if (!requestResults!!.containsKey(PermissionConstants.PERMISSION_GROUP_LOCATION_ALWAYS)) {
                    requestResults!![PermissionConstants.PERMISSION_GROUP_LOCATION_ALWAYS] = permissionStatus
                }
            } else if (permission == PermissionConstants.PERMISSION_GROUP_LOCATION) {
                val permissionStatus = PermissionUtils.toPermissionStatus(activity, permissionName, result)

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if (!requestResults!!.containsKey(PermissionConstants.PERMISSION_GROUP_LOCATION_ALWAYS)) {
                        requestResults!![PermissionConstants.PERMISSION_GROUP_LOCATION_ALWAYS] = permissionStatus
                    }
                }

                if (!requestResults!!.containsKey(PermissionConstants.PERMISSION_GROUP_LOCATION_WHEN_IN_USE)) {
                    requestResults!![PermissionConstants.PERMISSION_GROUP_LOCATION_WHEN_IN_USE] = permissionStatus
                }

                requestResults!![permission] = permissionStatus
            } else if (permission == PermissionConstants.PERMISSION_GROUP_PHOTOS || permission == PermissionConstants.PERMISSION_GROUP_VIDEOS) {
                // [grantResults] can only contain PermissionConstants.PERMISSION_STATUS_GRANTED or PermissionConstants.PERMISSION_STATUS_DENIED status.
                // But these permissions can have status PermissionConstants.PERMISSION_STATUS_LIMITED, so we need to recheck status
                requestResults!![permission] = determinePermissionStatus(permission)
            } else {
                if (!requestResults!!.containsKey(permission)) {
                    requestResults!![permission] = PermissionUtils.toPermissionStatus(activity, permissionName, result)
                }
            }
        }

        pendingRequestCount -= grantResults.size

        // Post result if all requests have been handled.
        if (successCallback != null && pendingRequestCount == 0) {
            successCallback!!.onSuccess(requestResults!!)
        }
        return true
    }

    /**
     * Determines the permission status of the provided permission.
     *
     * To distinguish between a status of 'denied' and a status of 'permanently denied', the plugin
     * needs access to an activity. If `this.activity` is null, for example when running the
     * application in the background, the resolved status will be 'denied' for both 'denied' and
     * 'permanently denied'.
     *
     * @param permission      the permission for which to determine the status.
     * @param successCallback the callback to which the resolved status must be supplied.
     */
    fun checkPermissionStatus(
        permission: @PermissionConstants.PermissionGroup Int,
        successCallback: CheckPermissionsSuccessCallback
    ) {
        successCallback.onSuccess(determinePermissionStatus(permission))
    }

    /**
     * Requests the user for the provided permissions.
     *
     * This method will throw an error if it is called before all permission requests that were
     * requested in a previous call have been resolved.
     *
     * Android distinguishes between
     * [runtime permissions](https://developer.android.com/guide/topics/permissions/overview#runtime) and
     * [special permissions](https://developer.android.com/guide/topics/permissions/overview#special).
     * Runtime permissions give an app additional access to restricted data or let the app perform
     * restricted actions that more substantially affect the system and other apps. These
     * permissions present the user with a dialog where they can choose to grant or deny the
     * permission. Special permissions guard access to system resources that are particularly
     * sensitive or not directly related to user privacy. These permissions are requested by sending
     * an [Intent] to the OS. The OS will open a special settings page where the user can
     * grant the permission.
     * Runtime permission request results will be reported through
     * [this.onRequestPermissionsResult], while special permissions request results will be reported through
     * [this.onActivityResult].
     * When these methods receive request results, they check whether all permissions that were
     * requested through this method were handled, and if so, return the result back to Dart.
     *
     * @param permissions     the permissions that are requested.
     * @param successCallback the callback for returning the permission results.
     * @param errorCallback   the callback to call in case of an error.
     */
    fun requestPermissions(
        permissions: List<Int>,
        successCallback: RequestPermissionsSuccessCallback,
        errorCallback: ErrorCallback
    ) {
        if (pendingRequestCount > 0) {
            errorCallback.onError(
                "PermissionHandler.PermissionManager",
                "A request for permissions is already running, please wait for it to finish before doing another request (note: you can request multiple permissions at the same time)."
            )
            return
        }

        if (activity == null) {
            Log.d(PermissionConstants.LOG_TAG, "Unable to detect current Activity.")

            errorCallback.onError(
                "PermissionHandler.PermissionManager",
                "Unable to detect current Android Activity."
            )
            return
        }

        this.successCallback = successCallback
        this.requestResults = HashMap()
        this.pendingRequestCount = 0 // sanity check

        val permissionsToRequest = ArrayList<String>()
        for (permission in permissions) {
            val permissionStatus = determinePermissionStatus(permission)
            if (permissionStatus == PermissionConstants.PERMISSION_STATUS_GRANTED) {
                if (!requestResults!!.containsKey(permission)) {
                    requestResults!![permission] = PermissionConstants.PERMISSION_STATUS_GRANTED
                }
                continue
            }

            val names = PermissionUtils.getManifestNames(activity, permission)

            // check to see if we can find manifest names
            // if we can't add as unknown and continue
            if (names == null || names.isEmpty()) {
                if (!requestResults!!.containsKey(permission)) {
                    // On Android below M, the android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS flag in AndroidManifest.xml
                    // may be ignored and not visible to the App as it's a new permission setting as a whole.
                    if (permission == PermissionConstants.PERMISSION_GROUP_IGNORE_BATTERY_OPTIMIZATIONS && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        requestResults!![permission] = PermissionConstants.PERMISSION_STATUS_RESTRICTED
                    } else {
                        requestResults!![permission] = PermissionConstants.PERMISSION_STATUS_DENIED
                    }
                    // On Android below R, the android.permission.MANAGE_EXTERNAL_STORAGE flag in AndroidManifest.xml
                    // may be ignored and not visible to the App as it's a new permission setting as a whole.
                    if (permission == PermissionConstants.PERMISSION_GROUP_MANAGE_EXTERNAL_STORAGE && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        requestResults!![permission] = PermissionConstants.PERMISSION_STATUS_RESTRICTED
                    } else {
                        requestResults!![permission] = PermissionConstants.PERMISSION_STATUS_DENIED
                    }
                }

                continue
            }

            // Request special permissions.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permission == PermissionConstants.PERMISSION_GROUP_IGNORE_BATTERY_OPTIMIZATIONS) {
                launchSpecialPermission(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    PermissionConstants.PERMISSION_CODE_IGNORE_BATTERY_OPTIMIZATIONS
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && permission == PermissionConstants.PERMISSION_GROUP_MANAGE_EXTERNAL_STORAGE) {
                launchSpecialPermission(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    PermissionConstants.PERMISSION_CODE_MANAGE_EXTERNAL_STORAGE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permission == PermissionConstants.PERMISSION_GROUP_SYSTEM_ALERT_WINDOW) {
                launchSpecialPermission(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    PermissionConstants.PERMISSION_CODE_SYSTEM_ALERT_WINDOW
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && permission == PermissionConstants.PERMISSION_GROUP_REQUEST_INSTALL_PACKAGES) {
                launchSpecialPermission(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    PermissionConstants.PERMISSION_CODE_REQUEST_INSTALL_PACKAGES
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permission == PermissionConstants.PERMISSION_GROUP_ACCESS_NOTIFICATION_POLICY) {
                launchSpecialPermission(
                    Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS,
                    PermissionConstants.PERMISSION_CODE_ACCESS_NOTIFICATION_POLICY
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && permission == PermissionConstants.PERMISSION_GROUP_SCHEDULE_EXACT_ALARM) {
                launchSpecialPermission(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    PermissionConstants.PERMISSION_CODE_SCHEDULE_EXACT_ALARM
                )
            } else if (permission == PermissionConstants.PERMISSION_GROUP_CALENDAR_FULL_ACCESS || permission == PermissionConstants.PERMISSION_GROUP_CALENDAR) {
                // Deny CALENDAR_FULL_ACCESS permission if manifest is not listing both write- and read permissions.
                val isValidManifest = isValidManifestForCalendarFullAccess()
                if (isValidManifest) {
                    permissionsToRequest.add(Manifest.permission.WRITE_CALENDAR)
                    permissionsToRequest.add(Manifest.permission.READ_CALENDAR)
                    pendingRequestCount += 2
                } else {
                    requestResults!![permission] = PermissionConstants.PERMISSION_STATUS_DENIED
                }
            } else {
                permissionsToRequest.addAll(names)
                pendingRequestCount += names.size
            }
        }

        // Request runtime permissions.
        if (permissionsToRequest.isNotEmpty()) {
            val requestPermissions = permissionsToRequest.toTypedArray()
            ActivityCompat.requestPermissions(
                activity,
                requestPermissions,
                PermissionConstants.PERMISSION_CODE
            )
        }

        // Post results immediately if no requests are pending.
        if (successCallback != null && pendingRequestCount == 0) {
            successCallback.onSuccess(requestResults!!)
        }
    }

    @PermissionConstants.PermissionStatus
    private fun determinePermissionStatus(permission: @PermissionConstants.PermissionGroup Int): Int {
        if (permission == PermissionConstants.PERMISSION_GROUP_NOTIFICATION) {
            return checkNotificationPermissionStatus()
        }

        if (permission == PermissionConstants.PERMISSION_GROUP_BLUETOOTH) {
            return checkBluetoothPermissionStatus()
        }

        if (permission == PermissionConstants.PERMISSION_GROUP_BLUETOOTH_CONNECT
            || permission == PermissionConstants.PERMISSION_GROUP_BLUETOOTH_SCAN
            || permission == PermissionConstants.PERMISSION_GROUP_BLUETOOTH_ADVERTISE
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return checkBluetoothPermissionStatus()
            }
        }

        // Inspect the manifest for CALENDAR_FULL_ACCESS, as a misconfigured manifest will give a false positive if READ access has been provided.
        if (permission == PermissionConstants.PERMISSION_GROUP_CALENDAR_FULL_ACCESS || permission == PermissionConstants.PERMISSION_GROUP_CALENDAR) {
            val isValidManifest = isValidManifestForCalendarFullAccess()
            if (!isValidManifest) return PermissionConstants.PERMISSION_STATUS_DENIED
        }

        val names = PermissionUtils.getManifestNames(context, permission)

        if (names == null) {
            Log.d(PermissionConstants.LOG_TAG, "No android specific permissions needed for: $permission")

            return PermissionConstants.PERMISSION_STATUS_GRANTED
        }

        //if no permissions were found then there is an issue and permission is not set in Android manifest
        if (names.isEmpty()) {
            Log.d(PermissionConstants.LOG_TAG, "No permissions found in manifest for: $names permission: $permission")

            // On Android below M, the android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS flag in AndroidManifest.xml
            // may be ignored and not visible to the App as it's a new permission setting as a whole.
            if (permission == PermissionConstants.PERMISSION_GROUP_IGNORE_BATTERY_OPTIMIZATIONS) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    return PermissionConstants.PERMISSION_STATUS_RESTRICTED
                }
            }

            // On Android below R, the android.permission.MANAGE_EXTERNAL_STORAGE flag in AndroidManifest.xml
            // may be ignored and not visible to the App as it's a new permission setting as a whole.
            if (permission == PermissionConstants.PERMISSION_GROUP_MANAGE_EXTERNAL_STORAGE) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    return PermissionConstants.PERMISSION_STATUS_RESTRICTED
                }
            }

            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                PermissionConstants.PERMISSION_STATUS_GRANTED
            else
                PermissionConstants.PERMISSION_STATUS_DENIED
        }

        val requiresExplicitPermission = context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.M

        if (requiresExplicitPermission) {
            val permissionStatuses: MutableSet<@PermissionConstants.PermissionStatus Int> = HashSet()

            for (name in names) {
                if (permission == PermissionConstants.PERMISSION_GROUP_IGNORE_BATTERY_OPTIMIZATIONS) {
                    val packageName = context.packageName
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    if (pm != null && pm.isIgnoringBatteryOptimizations(packageName)) {
                        permissionStatuses.add(PermissionConstants.PERMISSION_STATUS_GRANTED)
                    } else {
                        permissionStatuses.add(PermissionConstants.PERMISSION_STATUS_DENIED)
                    }
                } else if (permission == PermissionConstants.PERMISSION_GROUP_MANAGE_EXTERNAL_STORAGE) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        permissionStatuses.add(PermissionConstants.PERMISSION_STATUS_RESTRICTED)
                    }

                     val status = if (Environment.isExternalStorageManager())
                         PermissionConstants.PERMISSION_STATUS_GRANTED
                     else
                         PermissionConstants.PERMISSION_STATUS_DENIED
                    permissionStatuses.add(status)
                } else if (permission == PermissionConstants.PERMISSION_GROUP_SYSTEM_ALERT_WINDOW) {
                    val status = if (Settings.canDrawOverlays(context))
                        PermissionConstants.PERMISSION_STATUS_GRANTED
                    else
                        PermissionConstants.PERMISSION_STATUS_DENIED
                    permissionStatuses.add(status)
                } else if (permission == PermissionConstants.PERMISSION_GROUP_REQUEST_INSTALL_PACKAGES) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val status = if (context.packageManager.canRequestPackageInstalls())
                            PermissionConstants.PERMISSION_STATUS_GRANTED
                        else
                            PermissionConstants.PERMISSION_STATUS_DENIED
                        permissionStatuses.add(status)
                    }
                } else if (permission == PermissionConstants.PERMISSION_GROUP_ACCESS_NOTIFICATION_POLICY) {
                    val notificationManager = context.getSystemService(Application.NOTIFICATION_SERVICE) as NotificationManager
                    val status = if (notificationManager.isNotificationPolicyAccessGranted)
                        PermissionConstants.PERMISSION_STATUS_GRANTED
                    else
                        PermissionConstants.PERMISSION_STATUS_DENIED
                    permissionStatuses.add(status)
                } else if (permission == PermissionConstants.PERMISSION_GROUP_SCHEDULE_EXACT_ALARM) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val status = if (alarmManager.canScheduleExactAlarms())
                            PermissionConstants.PERMISSION_STATUS_GRANTED
                        else
                            PermissionConstants.PERMISSION_STATUS_DENIED
                        permissionStatuses.add(status)
                    } else {
                        permissionStatuses.add(PermissionConstants.PERMISSION_STATUS_GRANTED)
                    }
                } else if (permission == PermissionConstants.PERMISSION_GROUP_PHOTOS || permission == PermissionConstants.PERMISSION_GROUP_VIDEOS) {
                    val permissionStatus = ContextCompat.checkSelfPermission(context, name)
                    var permissionStatusLimited = permissionStatus
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        permissionStatusLimited = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                    }
                    if (permissionStatusLimited == PackageManager.PERMISSION_GRANTED && permissionStatus == PackageManager.PERMISSION_DENIED) {
                        permissionStatuses.add(PermissionConstants.PERMISSION_STATUS_LIMITED)
                    } else if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                        permissionStatuses.add(PermissionConstants.PERMISSION_STATUS_GRANTED)
                    } else {
                        permissionStatuses.add(PermissionUtils.determineDeniedVariant(activity, name))
                    }
                } else {
                    val permissionStatus = ContextCompat.checkSelfPermission(context, name)
                    if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                        permissionStatuses.add(PermissionUtils.determineDeniedVariant(activity, name))
                    }
                }
            }
            if (permissionStatuses.isNotEmpty()) {
                return PermissionUtils.strictestStatus(permissionStatuses)
            }
        }

        return PermissionConstants.PERMISSION_STATUS_GRANTED
    }

    /**
     * Launches a request for a [special permission](https://developer.android.com/training/permissions/requesting-special).
     *
     * There is a special case for [Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS]. See
     * [this comment](https://github.com/Baseflow/flutter-permission-handler/pull/587#discussion_r649295489)
     * for more details.
     *
     * @param permissionAction the action for launching the settings page for a particular permission.
     * @param requestCode      a request code to verify incoming results.
     */
    private fun launchSpecialPermission(permissionAction: String, requestCode: Int) {
        if (activity == null) {
            return
        }

        val intent = Intent(permissionAction)
        if (permissionAction != Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) {
            val packageName = activity.packageName
            intent.data = Uri.parse("package:$packageName")
        }
        activity.startActivityForResult(intent, requestCode)
        pendingRequestCount++
    }

    fun shouldShowRequestPermissionRationale(
        permission: Int,
        successCallback: ShouldShowRequestPermissionRationaleSuccessCallback,
        errorCallback: ErrorCallback
    ) {
        if (activity == null) {
            Log.d(PermissionConstants.LOG_TAG, "Unable to detect current Activity.")

            errorCallback.onError(
                "PermissionHandler.PermissionManager",
                "Unable to detect current Android Activity."
            )
            return
        }

        val names = PermissionUtils.getManifestNames(activity, permission)

        // if isn't an android specific group then go ahead and return false;
        if (names == null) {
            Log.d(PermissionConstants.LOG_TAG, "No android specific permissions needed for: $permission")
            successCallback.onSuccess(false)
            return
        }

        if (names.isEmpty()) {
            Log.d(PermissionConstants.LOG_TAG, "No permissions found in manifest for: $permission no need to show request rationale")
            successCallback.onSuccess(false)
            return
        }

        successCallback.onSuccess(ActivityCompat.shouldShowRequestPermissionRationale(activity, names[0]))
    }

    @PermissionConstants.PermissionStatus
    private fun checkNotificationPermissionStatus(): Int {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val manager = NotificationManagerCompat.from(context)
            val isGranted = manager.areNotificationsEnabled()
            if (isGranted) {
                PermissionConstants.PERMISSION_STATUS_GRANTED
            } else {
                PermissionConstants.PERMISSION_STATUS_DENIED
            }
        } else {
            val status = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            if (status == PackageManager.PERMISSION_GRANTED) {
                PermissionConstants.PERMISSION_STATUS_GRANTED
            } else {
                PermissionUtils.determineDeniedVariant(activity, Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @PermissionConstants.PermissionStatus
    private fun checkBluetoothPermissionStatus(): Int {
        val names = PermissionUtils.getManifestNames(context, PermissionConstants.PERMISSION_GROUP_BLUETOOTH)
        val missingInManifest = names == null || names.isEmpty()
        return if (missingInManifest) {
            Log.d(PermissionConstants.LOG_TAG, "Bluetooth permission missing in manifest")
            PermissionConstants.PERMISSION_STATUS_DENIED
        } else {
            PermissionConstants.PERMISSION_STATUS_GRANTED
        }
    }

    /**
     * Checks if the manifest contains both [Manifest.permission.WRITE_CALENDAR] and
     * [Manifest.permission.READ_CALENDAR] permission declarations.
     */
    private fun isValidManifestForCalendarFullAccess(): Boolean {
        val names = PermissionUtils.getManifestNames(context, PermissionConstants.PERMISSION_GROUP_CALENDAR_FULL_ACCESS)
        val writeInManifest = names != null && names.contains(Manifest.permission.WRITE_CALENDAR)
        val readInManifest = names != null && names.contains(Manifest.permission.READ_CALENDAR)
        val validManifest = writeInManifest && readInManifest
        if (!validManifest) {
            if (!writeInManifest) Log.d(PermissionConstants.LOG_TAG, Manifest.permission.WRITE_CALENDAR + " missing in manifest")
            if (!readInManifest) Log.d(PermissionConstants.LOG_TAG, Manifest.permission.READ_CALENDAR + " missing in manifest")
            return false
        }
        return true
    }

    fun interface RequestPermissionsSuccessCallback {
        fun onSuccess(results: Map<Int, Int>)
    }

    fun interface CheckPermissionsSuccessCallback {
        fun onSuccess(@PermissionConstants.PermissionStatus permissionStatus: Int)
    }

    fun interface ShouldShowRequestPermissionRationaleSuccessCallback {
        fun onSuccess(shouldShowRequestPermissionRationale: Boolean)
    }
}