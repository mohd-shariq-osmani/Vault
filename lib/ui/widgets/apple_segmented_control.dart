import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import '../theme/colors.dart';
import 'apple_touchable.dart';

class AppleSegmentedControl extends StatelessWidget {
  final List<Map<String, dynamic>> items;
  final String activeLabel;
  final ValueChanged<String> onSelected;

  const AppleSegmentedControl({
    super.key,
    required this.items,
    required this.activeLabel,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 44,
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(
        color: appleCardBackground,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: appleBorderStroke),
      ),
      child: Row(
        children: items.map((item) {
          final label = item['label'] as String;
          final icon = item['icon'] as IconData;
          final isSelected = label == activeLabel;

          return Expanded(
            child: AppleTouchable(
              onTap: () {
                if (!isSelected) {
                  HapticFeedback.selectionClick();
                  onSelected(label);
                }
              },
              scaleFactor: 0.95,
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 220),
                curve: Curves.easeOutCubic,
                decoration: BoxDecoration(
                  color: isSelected ? appleCardTertiary : Colors.transparent,
                  borderRadius: BorderRadius.circular(19),
                  boxShadow: isSelected
                      ? [
                          BoxShadow(
                            color: Colors.black.withAlpha(80),
                            blurRadius: 8,
                            offset: const Offset(0, 2),
                          ),
                        ]
                      : [],
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      icon,
                      size: 16,
                      color: isSelected ? Colors.white : textSecondary,
                    ),
                    const SizedBox(width: 6),
                    Text(
                      label,
                      style: GoogleFonts.inter(
                        fontSize: 13,
                        fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                        color: isSelected ? Colors.white : textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }
}
