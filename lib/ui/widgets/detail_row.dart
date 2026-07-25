import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import '../theme/colors.dart';
import 'apple_touchable.dart';

class DetailRow extends StatefulWidget {
  final String label;
  final String value;
  final bool isSensitive;

  const DetailRow({
    super.key,
    required this.label,
    required this.value,
    this.isSensitive = false,
  });

  @override
  State<DetailRow> createState() => _DetailRowState();
}

class _DetailRowState extends State<DetailRow> {
  bool _revealed = false;

  @override
  Widget build(BuildContext context) {
    if (widget.value.isEmpty) return const SizedBox.shrink();

    final displayValue = widget.isSensitive && !_revealed
        ? '•' * widget.value.length
        : widget.value;

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        color: appleCardBackground,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: appleBorderStroke),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.label.toUpperCase(),
                  style: GoogleFonts.inter(
                    fontSize: 10,
                    color: textSecondary,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 1.1,
                  ),
                ),
                const SizedBox(height: 5),
                Text(
                  displayValue,
                  style: GoogleFonts.inter(
                    fontSize: 16,
                    color: textPrimary,
                    fontWeight: FontWeight.w600,
                    letterSpacing: widget.isSensitive && !_revealed ? 2.0 : -0.2,
                  ),
                ),
              ],
            ),
          ),
          if (widget.isSensitive) ...[
            AppleTouchable(
              onTap: () {
                setState(() => _revealed = !_revealed);
              },
              child: Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: appleCardSecondary,
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  _revealed ? Icons.visibility_off_rounded : Icons.visibility_rounded,
                  size: 18,
                  color: appleBlue,
                ),
              ),
            ),
            const SizedBox(width: 8),
          ],
          AppleTouchable(
            onTap: () {
              Clipboard.setData(ClipboardData(text: widget.value));
              HapticFeedback.mediumImpact();
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text('${widget.label} copied to clipboard'),
                  duration: const Duration(seconds: 2),
                ),
              );
            },
            child: Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: appleCardSecondary,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.copy_rounded,
                size: 18,
                color: textSecondary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
