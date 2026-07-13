import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import '../theme/colors.dart';

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

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    widget.label.toUpperCase(),
                    style: GoogleFonts.inter(
                      fontSize: 9,
                      color: textSecondary,
                      fontWeight: FontWeight.w600,
                      letterSpacing: 1.2,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    displayValue,
                    style: widget.isSensitive
                        ? const TextStyle(
                            fontFamily: 'monospace',
                            fontSize: 14,
                            color: textPrimary,
                          )
                        : GoogleFonts.inter(
                            fontSize: 14,
                            color: textPrimary,
                          ),
                  ),
                ],
              ),
            ),
            if (widget.isSensitive)
              IconButton(
                icon: Icon(
                  _revealed ? Icons.visibility_off : Icons.visibility,
                  size: 18,
                  color: textMuted,
                ),
                onPressed: () => setState(() => _revealed = !_revealed),
                tooltip: _revealed ? 'Hide' : 'Reveal',
              ),
            IconButton(
              icon: const Icon(Icons.copy, size: 18, color: textMuted),
              onPressed: () {
                Clipboard.setData(ClipboardData(text: widget.value));
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('${widget.label} copied to clipboard'),
                    duration: const Duration(seconds: 2),
                  ),
                );
              },
              tooltip: 'Copy',
            ),
          ],
        ),
        const Divider(color: cinemaStroke, height: 1),
        const SizedBox(height: 12),
      ],
    );
  }
}
