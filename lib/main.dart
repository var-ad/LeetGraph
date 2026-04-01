import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import 'widget_bridge.dart';

void main() {
  runApp(const LeetGraphApp());
}

class LeetGraphApp extends StatelessWidget {
  const LeetGraphApp({super.key});

  @override
  Widget build(BuildContext context) {
    const seed = Color(0xFF2F6B4F);

    return MaterialApp(
      title: 'LeetGraph',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: seed,
          brightness: Brightness.light,
        ),
        scaffoldBackgroundColor: const Color(0xFFF5F2EA),
        useMaterial3: true,
      ),
      home: const WidgetSetupPage(),
    );
  }
}

class WidgetSetupPage extends StatefulWidget {
  const WidgetSetupPage({super.key});

  @override
  State<WidgetSetupPage> createState() => _WidgetSetupPageState();
}

class _WidgetSetupPageState extends State<WidgetSetupPage> {
  final TextEditingController _usernameController = TextEditingController();
  final WidgetPlatformBridge _bridge = const WidgetPlatformBridge();

  LeetWidgetState _widgetState = const LeetWidgetState.empty();
  StatusBanner? _banner;
  bool _isBusy = false;

  bool get _isAndroid =>
      !kIsWeb && defaultTargetPlatform == TargetPlatform.android;

  @override
  void initState() {
    super.initState();
    _loadWidgetState();
  }

  @override
  void dispose() {
    _usernameController.dispose();
    super.dispose();
  }

  Future<void> _loadWidgetState() async {
    if (!_isAndroid) {
      setState(() {
        _banner = StatusBanner.info(
          'The widget is implemented for Android home screens only.',
        );
      });
      return;
    }

    final widgetState = await _bridge.getWidgetState();
    if (!mounted) {
      return;
    }

    _usernameController.value = TextEditingValue(
      text: widgetState.username,
      selection: TextSelection.collapsed(offset: widgetState.username.length),
    );

    setState(() {
      _widgetState = widgetState;
      if (widgetState.errorMessage != null &&
          widgetState.errorMessage!.isNotEmpty) {
        _banner = StatusBanner.error(widgetState.errorMessage!);
      }
    });
  }

  Future<void> _saveAndSync() async {
    final username = _usernameController.text.trim();
    if (username.isEmpty) {
      setState(() {
        _banner = StatusBanner.error(
          'Enter your public LeetCode username first.',
        );
      });
      return;
    }

    setState(() {
      _isBusy = true;
      _banner = StatusBanner.info(
        'Saving your username and refreshing the widget...',
      );
    });

    await _bridge.saveUsername(username);
    final refreshResult = await _bridge.refreshWidget();
    final widgetState = await _bridge.getWidgetState();

    if (!mounted) {
      return;
    }

    setState(() {
      _isBusy = false;
      _widgetState = widgetState;
      _banner = refreshResult.success
          ? StatusBanner.success(
              'Widget synced. You can pin it to your home screen now.',
            )
          : StatusBanner.error(
              refreshResult.message ?? 'Unable to refresh the widget.',
            );
    });
  }

  Future<void> _refreshWidget() async {
    if (_widgetState.username.isEmpty) {
      setState(() {
        _banner = StatusBanner.error(
          'Save a LeetCode username before refreshing.',
        );
      });
      return;
    }

    setState(() {
      _isBusy = true;
      _banner = StatusBanner.info('Refreshing the widget data...');
    });

    final refreshResult = await _bridge.refreshWidget();
    final widgetState = await _bridge.getWidgetState();

    if (!mounted) {
      return;
    }

    setState(() {
      _isBusy = false;
      _widgetState = widgetState;
      _banner = refreshResult.success
          ? StatusBanner.success('Widget data refreshed.')
          : StatusBanner.error(
              refreshResult.message ?? 'Unable to refresh the widget.',
            );
    });
  }

  Future<void> _requestPinWidget() async {
    if (!_isAndroid) {
      return;
    }

    final didOpenSystemPicker = await _bridge.requestPinWidget();

    if (!mounted) {
      return;
    }

    setState(() {
      _banner = didOpenSystemPicker
          ? StatusBanner.success(
              'The launcher widget picker opened. Place LeetGraph on your home screen.',
            )
          : StatusBanner.info(
              'Open your launcher widget picker and add LeetGraph manually if the system does not support pinning.',
            );
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(20, 20, 20, 28),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 720),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'LeetGraph',
                    style: theme.textTheme.displaySmall?.copyWith(
                      fontWeight: FontWeight.w800,
                      color: const Color(0xFF183126),
                    ),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    'A small Android app whose whole job is powering a resizeable LeetCode heatmap widget on your home screen.',
                    style: theme.textTheme.titleMedium?.copyWith(
                      color: const Color(0xFF4A5A52),
                      height: 1.4,
                    ),
                  ),
                  const SizedBox(height: 20),
                  const WidgetMockCard(),
                  const SizedBox(height: 20),
                  if (_banner != null) ...[
                    _BannerCard(banner: _banner!),
                    const SizedBox(height: 20),
                  ],
                  _StatusCard(widgetState: _widgetState),
                  const SizedBox(height: 20),
                  _SetupCard(
                    controller: _usernameController,
                    isBusy: _isBusy,
                    isAndroid: _isAndroid,
                    onSave: _saveAndSync,
                    onRefresh: _refreshWidget,
                    onPinWidget: _requestPinWidget,
                  ),
                  const SizedBox(height: 20),
                  const _HowItWorksCard(),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _SetupCard extends StatelessWidget {
  const _SetupCard({
    required this.controller,
    required this.isBusy,
    required this.isAndroid,
    required this.onSave,
    required this.onRefresh,
    required this.onPinWidget,
  });

  final TextEditingController controller;
  final bool isBusy;
  final bool isAndroid;
  final Future<void> Function() onSave;
  final Future<void> Function() onRefresh;
  final Future<void> Function() onPinWidget;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      elevation: 0,
      color: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      child: Padding(
        padding: const EdgeInsets.all(22),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Widget setup',
              style: theme.textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'The widget only needs your public LeetCode username. After that it keeps syncing in the background and you can resize it from the home screen.',
              style: theme.textTheme.bodyLarge?.copyWith(
                color: const Color(0xFF586861),
                height: 1.45,
              ),
            ),
            const SizedBox(height: 18),
            TextField(
              controller: controller,
              enabled: isAndroid && !isBusy,
              textInputAction: TextInputAction.done,
              onSubmitted: (_) => onSave(),
              decoration: InputDecoration(
                labelText: 'LeetCode username',
                hintText: 'for example: leetcode',
                filled: true,
                fillColor: const Color(0xFFF4F7F2),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(20),
                  borderSide: BorderSide.none,
                ),
                prefixIcon: const Icon(Icons.alternate_email_rounded),
              ),
            ),
            const SizedBox(height: 18),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                FilledButton.icon(
                  onPressed: isAndroid && !isBusy ? onSave : null,
                  icon: isBusy
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.cloud_sync_rounded),
                  label: const Text('Save and sync'),
                ),
                OutlinedButton.icon(
                  onPressed: isAndroid && !isBusy ? onRefresh : null,
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Refresh now'),
                ),
                OutlinedButton.icon(
                  onPressed: isAndroid && !isBusy ? onPinWidget : null,
                  icon: const Icon(Icons.add_to_home_screen_rounded),
                  label: const Text('Add widget'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _StatusCard extends StatelessWidget {
  const _StatusCard({required this.widgetState});

  final LeetWidgetState widgetState;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final lastUpdated = widgetState.lastUpdated;

    return Card(
      elevation: 0,
      color: const Color(0xFF183126),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      child: Padding(
        padding: const EdgeInsets.all(22),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              widgetState.username.isEmpty
                  ? 'No username saved yet'
                  : '@${widgetState.username}',
              style: theme.textTheme.headlineSmall?.copyWith(
                color: Colors.white,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              widgetState.username.isEmpty
                  ? 'Save a username to let the Android widget pull your LeetCode activity.'
                  : widgetState.hasSnapshot
                  ? 'Background sync is ready. The widget can refresh on its own and also on demand.'
                  : 'Username saved. The first widget sync will fill in the heatmap.',
              style: theme.textTheme.bodyLarge?.copyWith(
                color: const Color(0xFFC8D3CD),
                height: 1.4,
              ),
            ),
            const SizedBox(height: 18),
            Wrap(
              spacing: 10,
              runSpacing: 10,
              children: [
                _StatPill(
                  label: 'Submissions',
                  value: widgetState.hasSnapshot
                      ? widgetState.totalSubmissions.toString()
                      : '--',
                ),
                _StatPill(
                  label: 'Active days',
                  value: widgetState.hasSnapshot
                      ? widgetState.activeDays.toString()
                      : '--',
                ),
                _StatPill(
                  label: 'Streak',
                  value: widgetState.hasSnapshot
                      ? '${widgetState.currentStreak}d'
                      : '--',
                ),
              ],
            ),
            const SizedBox(height: 16),
            Text(
              lastUpdated == null
                  ? 'Last sync: waiting for first update'
                  : 'Last sync: ${_formatTimestamp(lastUpdated)}',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: const Color(0xFF9FB2A6),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _formatTimestamp(DateTime timestamp) {
    final local = timestamp.toLocal();
    String twoDigits(int value) => value.toString().padLeft(2, '0');
    return '${local.year}-${twoDigits(local.month)}-${twoDigits(local.day)} '
        '${twoDigits(local.hour)}:${twoDigits(local.minute)}';
  }
}

class _HowItWorksCard extends StatelessWidget {
  const _HowItWorksCard();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      elevation: 0,
      color: const Color(0xFFE8EEE7),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      child: Padding(
        padding: const EdgeInsets.all(22),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'How it works',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 14),
            const _StepRow(
              index: '1',
              title: 'Save your username',
              description:
                  'The app stores your public LeetCode handle for the Android widget.',
            ),
            const SizedBox(height: 12),
            const _StepRow(
              index: '2',
              title: 'Pin the widget',
              description:
                  'Use the Add widget button or your launcher widget picker to place LeetGraph on the home screen.',
            ),
            const SizedBox(height: 12),
            const _StepRow(
              index: '3',
              title: 'Resize whenever you want',
              description:
                  'The heatmap redraws itself to fit larger or smaller widget sizes.',
            ),
          ],
        ),
      ),
    );
  }
}

class _BannerCard extends StatelessWidget {
  const _BannerCard({required this.banner});

  final StatusBanner banner;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: banner.backgroundColor,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
        child: Row(
          children: [
            Icon(banner.icon, color: banner.foregroundColor),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                banner.message,
                style: TextStyle(
                  color: banner.foregroundColor,
                  fontWeight: FontWeight.w600,
                  height: 1.35,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatPill extends StatelessWidget {
  const _StatPill({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: const Color(0xFF254638),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            value,
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w800,
              fontSize: 16,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            style: const TextStyle(color: Color(0xFFA7B8AF), fontSize: 12),
          ),
        ],
      ),
    );
  }
}

class _StepRow extends StatelessWidget {
  const _StepRow({
    required this.index,
    required this.title,
    required this.description,
  });

  final String index;
  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 30,
          height: 30,
          decoration: const BoxDecoration(
            color: Color(0xFF183126),
            shape: BoxShape.circle,
          ),
          alignment: Alignment.center,
          child: Text(
            index,
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                description,
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: const Color(0xFF52635B),
                  height: 1.4,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class WidgetMockCard extends StatelessWidget {
  const WidgetMockCard({super.key});

  static const List<Color> _levels = [
    Color(0xFF1C232B),
    Color(0xFF0E4429),
    Color(0xFF006D32),
    Color(0xFF26A641),
    Color(0xFF39D353),
  ];

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: const Color(0xFF0F1720),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(32)),
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: LayoutBuilder(
          builder: (context, constraints) {
            final width = constraints.maxWidth.isFinite
                ? constraints.maxWidth - 36
                : MediaQuery.of(context).size.width - 80;
            final columns = width > 560
                ? 28
                : width > 420
                ? 22
                : 16;
            final cellSize = width > 560
                ? 11.0
                : width > 420
                ? 10.0
                : 9.0;

            return Center(
              child: Wrap(
                spacing: 4,
                runSpacing: 4,
                children: List.generate(columns * 7, (index) {
                  final column = index ~/ 7;
                  final row = index % 7;
                  final level =
                      ((column * 5) + row * 3 + (row.isEven ? 1 : 0)) % 7;

                  return Container(
                    width: cellSize,
                    height: cellSize,
                    decoration: BoxDecoration(
                      color: switch (level) {
                        0 || 1 => _levels[0],
                        2 => _levels[1],
                        3 || 4 => _levels[2],
                        5 => _levels[3],
                        _ => _levels[4],
                      },
                      borderRadius: BorderRadius.circular(3.2),
                    ),
                  );
                }),
              ),
            );
          },
        ),
      ),
    );
  }
}

class StatusBanner {
  const StatusBanner._({
    required this.message,
    required this.icon,
    required this.backgroundColor,
    required this.foregroundColor,
  });

  StatusBanner.success(String message)
    : this._(
        message: message,
        icon: Icons.check_circle_rounded,
        backgroundColor: Color(0xFFDDEFE1),
        foregroundColor: Color(0xFF1F5A36),
      );

  StatusBanner.info(String message)
    : this._(
        message: message,
        icon: Icons.info_rounded,
        backgroundColor: Color(0xFFE4EBF7),
        foregroundColor: Color(0xFF27456A),
      );

  StatusBanner.error(String message)
    : this._(
        message: message,
        icon: Icons.error_rounded,
        backgroundColor: Color(0xFFF7E1DF),
        foregroundColor: Color(0xFF7A2A24),
      );

  final String message;
  final IconData icon;
  final Color backgroundColor;
  final Color foregroundColor;
}
