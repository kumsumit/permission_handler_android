import 'package:flutter_test/flutter_test.dart';
import 'package:permission_handler_android_example/permission_explorer.dart';
import 'package:permission_handler_platform_interface/permission_handler_platform_interface.dart';

void main() {
  test('filters permissions that are not actionable on Android', () {
    expect(isAndroidExamplePermission(Permission.camera), isTrue);
    expect(isAndroidExamplePermission(Permission.unknown), isFalse);
    expect(isAndroidExamplePermission(Permission.mediaLibrary), isFalse);
    expect(
      isAndroidExamplePermission(Permission.appTrackingTransparency),
      isFalse,
    );
  });

  test('groups common Android permissions into explorer categories', () {
    expect(categoryForPermission(Permission.photos), PermissionCategory.media);
    expect(
      categoryForPermission(Permission.locationAlways),
      PermissionCategory.location,
    );
    expect(categoryForPermission(Permission.camera), PermissionCategory.device);
    expect(
      categoryForPermission(Permission.sms),
      PermissionCategory.communication,
    );
    expect(
      categoryForPermission(Permission.scheduleExactAlarm),
      PermissionCategory.special,
    );
  });

  test('formats enum labels for display', () {
    expect(
      permissionLabel(Permission.calendarFullAccess),
      'calendar full access',
    );
    expect(
      statusLabel(PermissionStatus.permanentlyDenied),
      'permanentlyDenied',
    );
    expect(serviceStatusLabel(ServiceStatus.notApplicable), 'notApplicable');
  });
}
