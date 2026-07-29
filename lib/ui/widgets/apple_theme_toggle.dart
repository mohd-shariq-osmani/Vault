import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../providers/theme_provider.dart';
import '../theme/colors.dart';
import 'apple_touchable.dart';

class AppleThemeToggle extends ConsumerWidget {
  const AppleThemeToggle({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeMode = ref.watch(themeProvider);
    final isDarkMode = themeMode == ThemeMode.dark;
    final isLight = Theme.of(context).brightness == Brightness.light;

    final bgColor = isLight ? const Color(0xFFE5E5EA) : appleCardSecondary;
    final thumbColor = isLight ? Colors.white : appleCardTertiary;
    final borderColor = isLight ? const Color(0x1F000000) : appleBorderStroke;

    return AppleTouchable(
      onTap: () {
        HapticFeedback.selectionClick();
        ref.read(themeProvider.notifier).toggleTheme();
      },
      scaleFactor: 0.94,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 260),
        curve: Curves.easeInOutCubic,
        width: 68,
        height: 36,
        padding: const EdgeInsets.all(3),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: borderColor),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withAlpha(isLight ? 15 : 40),
              blurRadius: 4,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Stack(
          children: [
            // Sliding thumb
            AnimatedAlign(
              duration: const Duration(milliseconds: 260),
              curve: Curves.easeInOutCubic,
              alignment: isDarkMode ? Alignment.centerRight : Alignment.centerLeft,
              child: Container(
                width: 30,
                height: 30,
                decoration: BoxDecoration(
                  color: thumbColor,
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: isLight ? const Color(0x3D000000) : appleBorderHighlight,
                    width: 0.8,
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: isDarkMode
                          ? const Color(0x40000000)
                          : const Color(0x20000000),
                      blurRadius: 6,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
              ),
            ),

            // Icons layer
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Center(
                    child: AnimatedOpacity(
                      duration: const Duration(milliseconds: 200),
                      opacity: !isDarkMode ? 1.0 : 0.4,
                      child: const Icon(
                        Icons.wb_sunny_rounded,
                        size: 16,
                        color: Color(0xFFFF9F0C),
                      ),
                    ),
                  ),
                ),
                Expanded(
                  child: Center(
                    child: AnimatedOpacity(
                      duration: const Duration(milliseconds: 200),
                      opacity: isDarkMode ? 1.0 : 0.4,
                      child: const Icon(
                        Icons.nightlight_round,
                        size: 15,
                        color: Color(0xFF64D2FF),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
