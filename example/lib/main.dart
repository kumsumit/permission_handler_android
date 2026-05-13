import 'package:flutter/material.dart';
import 'package:permission_handler_platform_interface/permission_handler_platform_interface.dart';

import 'permission_explorer.dart';

void main() {
  runApp(const PermissionHandlerExampleApp());
}

class PermissionHandlerExampleApp extends StatelessWidget {
  const PermissionHandlerExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color.fromRGBO(48, 49, 60, 1),
        ),
        useMaterial3: true,
      ),
      home: Scaffold(
        appBar: AppBar(title: const Text('Permission Handler')),
        body: const PermissionHandlerWidget(),
      ),
    );
  }
}

/// A Flutter application demonstrating the functionality of this plugin
class PermissionHandlerWidget extends StatefulWidget {
  /// Creates a [PermissionHandlerWidget].
  const PermissionHandlerWidget({super.key});

  @override
  _PermissionHandlerWidgetState createState() =>
      _PermissionHandlerWidgetState();
}

class _PermissionHandlerWidgetState extends State<PermissionHandlerWidget> {
  PermissionCategory _selectedCategory = PermissionCategory.all;
  String _query = '';

  List<Permission> get _visiblePermissions {
    final normalizedQuery = _query.trim().toLowerCase();

    return Permission.values.where((permission) {
      if (!isAndroidExamplePermission(permission)) {
        return false;
      }

      if (_selectedCategory != PermissionCategory.all &&
          categoryForPermission(permission) != _selectedCategory) {
        return false;
      }

      if (normalizedQuery.isEmpty) {
        return true;
      }

      return permission.toString().toLowerCase().contains(normalizedQuery);
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final permissions = _visiblePermissions;

    return SafeArea(
      child: RefreshIndicator(
        onRefresh: () async => setState(() {}),
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
          children: [
            const _EnvironmentHeader(),
            const SizedBox(height: 12),
            TextField(
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                hintText: 'Search permissions',
                prefixIcon: Icon(Icons.search),
              ),
              onChanged: (value) => setState(() => _query = value),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: PermissionCategory.values.map((category) {
                return ChoiceChip(
                  label: Text(category.label),
                  selected: _selectedCategory == category,
                  onSelected: (_) =>
                      setState(() => _selectedCategory = category),
                );
              }).toList(),
            ),
            const SizedBox(height: 12),
            Text(
              '${permissions.length} permissions',
              style: Theme.of(context).textTheme.labelLarge,
            ),
            const SizedBox(height: 8),
            if (permissions.isEmpty)
              const _EmptyState()
            else
              ...permissions.map((permission) => PermissionWidget(permission)),
          ],
        ),
      ),
    );
  }
}

class _EnvironmentHeader extends StatelessWidget {
  const _EnvironmentHeader();

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        border: Border.all(color: Theme.of(context).dividerColor),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Android permission explorer',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 4),
            Text(
              'Pull to refresh statuses after changing permissions in settings.',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 32),
      child: Center(
        child: Text(
          'No permissions match this filter.',
          style: Theme.of(context).textTheme.bodyLarge,
        ),
      ),
    );
  }
}

/// Permission widget containing information about the passed [Permission]
class PermissionWidget extends StatefulWidget {
  /// Constructs a [PermissionWidget] for the supplied [Permission]
  const PermissionWidget(this._permission, {super.key});

  final Permission _permission;

  @override
  _PermissionState createState() => _PermissionState();
}

class _PermissionState extends State<PermissionWidget> {
  _PermissionState();

  final PermissionHandlerPlatform _permissionHandler =
      PermissionHandlerPlatform.instance;
  PermissionStatus _permissionStatus = PermissionStatus.denied;
  ServiceStatus? _serviceStatus;
  bool _isBusy = false;

  @override
  void initState() {
    super.initState();

    _listenForPermissionStatus();
  }

  void _listenForPermissionStatus() async {
    final status = await _permissionHandler.checkPermissionStatus(
      widget._permission,
    );
    if (!mounted) {
      return;
    }

    setState(() => _permissionStatus = status);
  }

  Color getPermissionColor() {
    switch (_permissionStatus) {
      case PermissionStatus.denied:
        return Colors.red;
      case PermissionStatus.granted:
        return Colors.green;
      case PermissionStatus.limited:
        return Colors.orange;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4),
      child: ListTile(
        title: Text(
          permissionLabel(widget._permission),
          style: Theme.of(context).textTheme.bodyLarge,
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Wrap(
            spacing: 8,
            runSpacing: 4,
            children: [
              _StatusPill(
                label: statusLabel(_permissionStatus),
                color: getPermissionColor(),
              ),
              _StatusPill(
                label: categoryForPermission(widget._permission).label,
                color: Colors.blueGrey,
              ),
              if (_serviceStatus != null)
                _StatusPill(
                  label: 'service ${serviceStatusLabel(_serviceStatus!)}',
                  color: _serviceStatus == ServiceStatus.enabled
                      ? Colors.green
                      : Colors.orange,
                ),
            ],
          ),
        ),
        trailing: _isBusy
            ? const SizedBox.square(
                dimension: 24,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : PopupMenuButton<_PermissionAction>(
                onSelected: _handleAction,
                itemBuilder: (context) => [
                  const PopupMenuItem(
                    value: _PermissionAction.check,
                    child: Text('Check status'),
                  ),
                  const PopupMenuItem(
                    value: _PermissionAction.request,
                    child: Text('Request permission'),
                  ),
                  if (widget._permission is PermissionWithService)
                    const PopupMenuItem(
                      value: _PermissionAction.service,
                      child: Text('Check service'),
                    ),
                  const PopupMenuItem(
                    value: _PermissionAction.settings,
                    child: Text('Open app settings'),
                  ),
                ],
              ),
        onTap: () => requestPermission(widget._permission),
      ),
    );
  }

  Future<void> _handleAction(_PermissionAction action) async {
    switch (action) {
      case _PermissionAction.check:
        await checkPermissionStatus();
      case _PermissionAction.request:
        await requestPermission(widget._permission);
      case _PermissionAction.service:
        await checkServiceStatus(widget._permission as PermissionWithService);
      case _PermissionAction.settings:
        await _permissionHandler.openAppSettings();
    }
  }

  Future<void> checkPermissionStatus() async {
    setState(() => _isBusy = true);
    final status = await _permissionHandler.checkPermissionStatus(
      widget._permission,
    );
    if (!mounted) {
      return;
    }

    setState(() {
      _permissionStatus = status;
      _isBusy = false;
    });
  }

  Future<void> checkServiceStatus(PermissionWithService permission) async {
    setState(() => _isBusy = true);
    final status = await _permissionHandler.checkServiceStatus(permission);
    if (!mounted) {
      return;
    }

    setState(() {
      _serviceStatus = status;
      _isBusy = false;
    });
  }

  Future<void> requestPermission(Permission permission) async {
    setState(() => _isBusy = true);
    final status = await _permissionHandler.requestPermissions([permission]);
    if (!mounted) {
      return;
    }

    setState(() {
      _permissionStatus = status[permission] ?? PermissionStatus.denied;
      _isBusy = false;
    });
  }
}

class _StatusPill extends StatelessWidget {
  const _StatusPill({required this.label, required this.color});

  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        border: Border.all(color: color.withValues(alpha: 0.4)),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        child: Text(
          label,
          style: Theme.of(context).textTheme.labelSmall?.copyWith(color: color),
        ),
      ),
    );
  }
}

enum _PermissionAction { check, request, service, settings }
