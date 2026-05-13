import 'package:permission_handler_platform_interface/permission_handler_platform_interface.dart';

enum PermissionCategory {
  all('All'),
  media('Media'),
  location('Location'),
  device('Device'),
  communication('Communication'),
  special('Special');

  const PermissionCategory(this.label);

  final String label;
}

bool isAndroidExamplePermission(Permission permission) {
  return permission != Permission.unknown &&
      permission != Permission.mediaLibrary &&
      permission != Permission.photosAddOnly &&
      permission != Permission.reminders &&
      permission != Permission.bluetooth &&
      permission != Permission.appTrackingTransparency &&
      permission != Permission.criticalAlerts &&
      permission != Permission.assistant &&
      permission != Permission.backgroundRefresh;
}

PermissionCategory categoryForPermission(Permission permission) {
  switch (permission) {
    case Permission.photos:
    case Permission.videos:
    case Permission.audio:
    case Permission.storage:
    case Permission.accessMediaLocation:
      return PermissionCategory.media;
    case Permission.location:
    case Permission.locationAlways:
    case Permission.locationWhenInUse:
      return PermissionCategory.location;
    case Permission.camera:
    case Permission.microphone:
    case Permission.speech:
    case Permission.sensors:
    case Permission.sensorsAlways:
    case Permission.activityRecognition:
    case Permission.nearbyWifiDevices:
      return PermissionCategory.device;
    case Permission.contacts:
    case Permission.phone:
    case Permission.sms:
    // Deprecated, but still shown because the Android implementation supports it.
    // ignore: deprecated_member_use
    case Permission.calendar:
    case Permission.calendarWriteOnly:
    case Permission.calendarFullAccess:
      return PermissionCategory.communication;
    case Permission.ignoreBatteryOptimizations:
    case Permission.notification:
    case Permission.manageExternalStorage:
    case Permission.systemAlertWindow:
    case Permission.requestInstallPackages:
    case Permission.accessNotificationPolicy:
    case Permission.bluetoothScan:
    case Permission.bluetoothAdvertise:
    case Permission.bluetoothConnect:
    case Permission.scheduleExactAlarm:
      return PermissionCategory.special;
    case Permission.unknown:
    case Permission.mediaLibrary:
    case Permission.photosAddOnly:
    case Permission.reminders:
    case Permission.bluetooth:
    case Permission.appTrackingTransparency:
    case Permission.criticalAlerts:
    case Permission.assistant:
    case Permission.backgroundRefresh:
      return PermissionCategory.all;
  }

  return PermissionCategory.all;
}

String permissionLabel(Permission permission) {
  final name = permission.toString().split('.').last;
  return name.replaceAllMapped(
    RegExp(r'([A-Z])'),
    (match) => ' ${match.group(1)!.toLowerCase()}',
  );
}

String statusLabel(PermissionStatus status) {
  return status.toString().split('.').last;
}

String serviceStatusLabel(ServiceStatus status) {
  return status.toString().split('.').last;
}
