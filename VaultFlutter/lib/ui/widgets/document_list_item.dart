import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../models/document.dart';
import '../theme/colors.dart';
import 'glassmorphic_card.dart';

class DocumentListItem extends StatelessWidget {
  final VaultDocument document;
  final bool isReorderMode;
  final VoidCallback? onTap;
  final VoidCallback? onLongPress;
  final VoidCallback? onMoveUp;
  final VoidCallback? onMoveDown;

  const DocumentListItem({
    super.key,
    required this.document,
    this.isReorderMode = false,
    this.onTap,
    this.onLongPress,
    this.onMoveUp,
    this.onMoveDown,
  });

  String _getTypeBadge() {
    switch (document.type) {
      case DocumentType.paymentCard:
        return document.cardType ?? 'CARD';
      case DocumentType.aadhaarCard:
        return 'AADHAAR';
      case DocumentType.panCard:
        return 'PAN';
      case DocumentType.driversLicense:
        return 'DL';
      case DocumentType.vehicleRc:
        return 'RC';
      case DocumentType.genericId:
        return document.genericIdType?.toUpperCase() ?? 'ID';
    }
  }

  IconData _getTypeIcon() {
    switch (document.type) {
      case DocumentType.paymentCard:
        return Icons.credit_card;
      case DocumentType.aadhaarCard:
        return Icons.badge;
      case DocumentType.panCard:
        return Icons.article;
      case DocumentType.driversLicense:
        return Icons.drive_eta;
      case DocumentType.vehicleRc:
        return Icons.directions_car;
      case DocumentType.genericId:
        return Icons.card_membership;
    }
  }

  String _getMaskedNumber() {
    switch (document.type) {
      case DocumentType.paymentCard:
        final num = document.cardNumber ?? '';
        final cleaned = num.replaceAll(' ', '');
        if (cleaned.length >= 4) {
          return '•••• •••• •••• ${cleaned.substring(cleaned.length - 4)}';
        }
        return '•••• •••• •••• ••••';
      case DocumentType.aadhaarCard:
        final num = document.aadhaarNumber ?? '';
        final cleaned = num.replaceAll(' ', '');
        if (cleaned.length >= 4) {
          return '•••• •••• ${cleaned.substring(cleaned.length - 4)}';
        }
        return '•••• •••• ••••';
      case DocumentType.panCard:
        final num = document.panNumber ?? '';
        if (num.length >= 4) {
          return '••••• ${num.substring(num.length - 4).toUpperCase()} •';
        }
        return '•••• ••••••';
      case DocumentType.driversLicense:
        return document.dlNumber ?? '—';
      case DocumentType.vehicleRc:
        return document.rcNumber ?? '—';
      case DocumentType.genericId:
        final num = document.genericIdNumber ?? '';
        final cleaned = num.replaceAll(' ', '');
        if (cleaned.length >= 4) {
          return '•••• •••• ${cleaned.substring(cleaned.length - 4)}';
        }
        return '•••• ••••';
    }
  }

  String _getHolderName() {
    switch (document.type) {
      case DocumentType.paymentCard:
        return document.cardholderName ?? '';
      case DocumentType.aadhaarCard:
        return document.aadhaarName ?? '';
      case DocumentType.panCard:
        return document.panName ?? '';
      case DocumentType.driversLicense:
        return document.dlHolderName ?? '';
      case DocumentType.vehicleRc:
        return document.rcOwnerName ?? '';
      case DocumentType.genericId:
        return document.genericIdName ?? '';
    }
  }

  String _getValidityDate() {
    switch (document.type) {
      case DocumentType.paymentCard:
        return document.cardExpiry ?? '';
      case DocumentType.aadhaarCard:
        return '';
      case DocumentType.panCard:
        return '';
      case DocumentType.driversLicense:
        return document.dlExpiry ?? '';
      case DocumentType.vehicleRc:
        return document.rcExpiry ?? '';
      case DocumentType.genericId:
        return document.genericIdExpiry ?? '';
    }
  }

  @override
  Widget build(BuildContext context) {
    final grad = gradientForDoc(document.cardColorIndex);
    final accent = accentForDoc(document.cardColorIndex);
    final maskedNum = _getMaskedNumber();
    final holderName = _getHolderName();
    final validityDate = _getValidityDate();
    final badge = _getTypeBadge();
    final icon = _getTypeIcon();

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: GlassmorphicCard(
        gradient: grad,
        glowColor: accent,
        onTap: onTap,
        onLongPress: onLongPress,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                // Type badge
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: accent.withAlpha(51),
                    borderRadius: BorderRadius.circular(6),
                    border: Border.all(color: accent.withAlpha(77)),
                  ),
                  child: Text(
                    badge,
                    style: GoogleFonts.inter(
                      fontSize: 9,
                      color: accent,
                      fontWeight: FontWeight.w700,
                      letterSpacing: 1.2,
                    ),
                  ),
                ),
                const Spacer(),
                if (isReorderMode)
                  Row(
                    children: [
                      InkWell(
                        onTap: onMoveUp,
                        borderRadius: BorderRadius.circular(16),
                        child: Container(
                          width: 32,
                          height: 32,
                          decoration: BoxDecoration(
                            color: Colors.white.withAlpha(26),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(Icons.arrow_upward, size: 16, color: textPrimary),
                        ),
                      ),
                      const SizedBox(width: 8),
                      InkWell(
                        onTap: onMoveDown,
                        borderRadius: BorderRadius.circular(16),
                        child: Container(
                          width: 32,
                          height: 32,
                          decoration: BoxDecoration(
                            color: Colors.white.withAlpha(26),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(Icons.arrow_downward, size: 16, color: textPrimary),
                        ),
                      ),
                    ],
                  )
                else
                  Icon(icon, color: accent.withAlpha(180), size: 20),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              document.title,
              style: GoogleFonts.inter(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: textPrimary,
                letterSpacing: 0.3,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 6),
            Text(
              maskedNum,
              style: GoogleFonts.robotoMono(
                fontSize: 13,
                color: textSecondary,
                letterSpacing: 1.5,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                if (holderName.isNotEmpty)
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'NAME',
                        style: GoogleFonts.inter(
                          fontSize: 8,
                          color: textMuted,
                          letterSpacing: 1.2,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      Text(
                        holderName.toUpperCase(),
                        style: GoogleFonts.inter(
                          fontSize: 11,
                          color: textSecondary,
                          fontWeight: FontWeight.w500,
                          letterSpacing: 0.5,
                        ),
                      ),
                    ],
                  ),
                const Spacer(),
                if (validityDate.isNotEmpty)
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(
                        'VALID THRU',
                        style: GoogleFonts.inter(
                          fontSize: 8,
                          color: textMuted,
                          letterSpacing: 1.2,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      Text(
                        validityDate,
                        style: GoogleFonts.inter(
                          fontSize: 11,
                          color: textSecondary,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                const SizedBox(width: 8),
                Icon(Icons.lock, size: 12, color: accent.withAlpha(128)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
