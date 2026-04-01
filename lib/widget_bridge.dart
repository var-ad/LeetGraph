import 'package:flutter/services.dart';

class WidgetPlatformBridge {
  const WidgetPlatformBridge();

  static const MethodChannel _channel = MethodChannel(
    'com.example.leetgraph/widget',
  );

  Future<LeetWidgetState> getWidgetState() async {
    try {
      final result = await _channel.invokeMapMethod<String, dynamic>(
        'getWidgetState',
      );
      return LeetWidgetState.fromMap(result);
    } on MissingPluginException {
      return const LeetWidgetState.empty();
    } on PlatformException catch (error) {
      return LeetWidgetState.fromMap({
        'errorMessage': error.message ?? 'Unable to load widget state.',
      });
    }
  }

  Future<void> saveUsername(String username) async {
    try {
      await _channel.invokeMethod<void>('saveUsername', {'username': username});
    } on MissingPluginException {
      return;
    }
  }

  Future<RefreshResult> refreshWidget() async {
    try {
      final result = await _channel.invokeMapMethod<String, dynamic>(
        'refreshWidget',
      );
      return RefreshResult.fromMap(result);
    } on MissingPluginException {
      return const RefreshResult(
        success: false,
        message: 'The Android widget bridge is unavailable.',
      );
    } on PlatformException catch (error) {
      return RefreshResult(
        success: false,
        message: error.message ?? 'Unable to refresh the widget.',
      );
    }
  }

  Future<bool> requestPinWidget() async {
    try {
      return await _channel.invokeMethod<bool>('requestPinWidget') ?? false;
    } on MissingPluginException {
      return false;
    } on PlatformException {
      return false;
    }
  }
}

class LeetWidgetState {
  const LeetWidgetState({
    required this.username,
    required this.totalSubmissions,
    required this.activeDays,
    required this.currentStreak,
    required this.lastUpdatedMillis,
    required this.hasSnapshot,
    required this.errorMessage,
  });

  const LeetWidgetState.empty()
    : username = '',
      totalSubmissions = 0,
      activeDays = 0,
      currentStreak = 0,
      lastUpdatedMillis = null,
      hasSnapshot = false,
      errorMessage = null;

  factory LeetWidgetState.fromMap(Map<dynamic, dynamic>? map) {
    final rawMap = map ?? const {};
    final rawLastUpdated = _toInt(rawMap['lastUpdatedMillis']);
    return LeetWidgetState(
      username: rawMap['username'] as String? ?? '',
      totalSubmissions: _toInt(rawMap['totalSubmissions']),
      activeDays: _toInt(rawMap['activeDays']),
      currentStreak: _toInt(rawMap['currentStreak']),
      lastUpdatedMillis: rawLastUpdated > 0 ? rawLastUpdated : null,
      hasSnapshot: rawMap['hasSnapshot'] as bool? ?? false,
      errorMessage: rawMap['errorMessage'] as String?,
    );
  }

  final String username;
  final int totalSubmissions;
  final int activeDays;
  final int currentStreak;
  final int? lastUpdatedMillis;
  final bool hasSnapshot;
  final String? errorMessage;

  DateTime? get lastUpdated => lastUpdatedMillis == null
      ? null
      : DateTime.fromMillisecondsSinceEpoch(lastUpdatedMillis!);

  static int _toInt(Object? value) {
    if (value is int) {
      return value;
    }
    if (value is num) {
      return value.toInt();
    }
    if (value is String) {
      return int.tryParse(value) ?? 0;
    }
    return 0;
  }
}

class RefreshResult {
  const RefreshResult({required this.success, required this.message});

  factory RefreshResult.fromMap(Map<dynamic, dynamic>? map) {
    final rawMap = map ?? const {};
    return RefreshResult(
      success: rawMap['success'] as bool? ?? false,
      message:
          rawMap['message'] as String? ??
          (rawMap['success'] as bool? ?? false
              ? 'Widget refreshed.'
              : 'Unable to refresh the widget.'),
    );
  }

  final bool success;
  final String? message;
}
