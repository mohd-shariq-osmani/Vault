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
      decoration: BoxDecoration(
        color: appleCardBackground,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: appleBorderStroke),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(22),
        child: SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          physics: const BouncingScrollPhysics(),
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 4),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: items.map((item) {
              final label = item['label'] as String;
              final icon = item['icon'] as IconData;
              final isSelected = label == activeLabel;

              return Padding(
                padding: const EdgeInsets.symmetric(horizontal: 2),
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
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    decoration: BoxDecoration(
                      color: isSelected ? appleCardTertiary : Colors.transparent,
                      borderRadius: BorderRadius.circular(18),
                      border: isSelected
                          ? Border.all(color: appleBorderHighlight, width: 0.8)
                          : null,
                      boxShadow: isSelected
                          ? [
                              BoxShadow(
                                color: Colors.black.withAlpha(90),
                                blurRadius: 6,
                                offset: const Offset(0, 2),
                              ),
                            ]
                          : [],
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          icon,
                          size: 15,
                          color: isSelected ? Colors.white : textSecondary,
                        ),
                        const SizedBox(width: 6),
                        Text(
                          label,
                          style: GoogleFonts.inter(
                            fontSize: 13,
                            fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                            color: isSelected ? Colors.white : textSecondary,
                            letterSpacing: -0.1,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        ),
      ),
    );
  }
}
