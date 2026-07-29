import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

final themeProvider = StateNotifierProvider<ThemeNotifier, ThemeMode>((ref) {
  return ThemeNotifier();
});

class ThemeNotifier extends StateNotifier<ThemeMode> {
  static const _storageKey = 'vault_theme_mode';
  final _storage = const FlutterSecureStorage();

  ThemeNotifier() : super(ThemeMode.dark) {
    _loadTheme();
  }

  Future<void> _loadTheme() async {
    try {
      final savedTheme = await _storage.read(key: _storageKey);
      if (savedTheme == 'light') {
        state = ThemeMode.light;
      } else if (savedTheme == 'dark') {
        state = ThemeMode.dark;
      } else {
        state = ThemeMode.dark;
      }
    } catch (_) {
      state = ThemeMode.dark;
    }
  }

  Future<void> toggleTheme() async {
    final nextTheme = state == ThemeMode.dark ? ThemeMode.light : ThemeMode.dark;
    state = nextTheme;
    try {
      await _storage.write(key: _storageKey, value: nextTheme == ThemeMode.light ? 'light' : 'dark');
    } catch (_) {}
  }

  bool get isDarkMode => state == ThemeMode.dark;
}
