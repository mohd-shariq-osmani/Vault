import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../models/document.dart';
import '../theme/colors.dart';
import 'apple_touchable.dart';
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
        return Icons.credit_card_rounded;
      case DocumentType.aadhaarCard:
        return Icons.badge_rounded;
      case DocumentType.panCard:
        return Icons.article_rounded;
      case DocumentType.driversLicense:
        return Icons.drive_eta_rounded;
      case DocumentType.vehicleRc:
        return Icons.directions_car_rounded;
      case DocumentType.genericId:
        return Icons.card_membership_rounded;
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
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 7),
      child: GlassmorphicCard(
        gradient: grad,
        glowColor: accent,
        onTap: onTap,
        onLongPress: onLongPress,
        borderRadius: 22,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                // Apple-style Pill Badge
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.white.withAlpha(30),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: Colors.white.withAlpha(50), width: 0.8),
                  ),
                  child: Text(
                    badge,
                    style: GoogleFonts.inter(
                      fontSize: 10,
                      color: Colors.white,
                      fontWeight: FontWeight.w700,
                      letterSpacing: 1.2,
                    ),
                  ),
                ),
                const Spacer(),
                if (isReorderMode)
                  Row(
                    children: [
                      AppleTouchable(
                        onTap: onMoveUp,
                        child: Container(
                          width: 34,
                          height: 34,
                          decoration: BoxDecoration(
                            color: Colors.white.withAlpha(35),
                            shape: BoxShape.circle,
                            border: Border.all(color: Colors.white.withAlpha(50)),
                          ),
                          child: const Icon(Icons.arrow_upward_rounded, size: 18, color: Colors.white),
                        ),
                      ),
                      const SizedBox(width: 10),
                      AppleTouchable(
                        onTap: onMoveDown,
                        child: Container(
                          width: 34,
                          height: 34,
                          decoration: BoxDecoration(
                            color: Colors.white.withAlpha(35),
                            shape: BoxShape.circle,
                            border: Border.all(color: Colors.white.withAlpha(50)),
                          ),
                          child: const Icon(Icons.arrow_downward_rounded, size: 18, color: Colors.white),
                        ),
                      ),
                    ],
                  )
                else
                  Container(
                    width: 32,
                    height: 32,
                    decoration: BoxDecoration(
                      color: Colors.white.withAlpha(25),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(icon, color: Colors.white.withAlpha(230), size: 18),
                  ),
              ],
            ),
            const SizedBox(height: 16),
            Text(
              document.title,
              style: GoogleFonts.inter(
                fontSize: 20,
                fontWeight: FontWeight.w800,
                color: Colors.white,
                letterSpacing: -0.3,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 6),
            Text(
              maskedNum,
              style: GoogleFonts.inter(
                fontSize: 14,
                color: Colors.white.withAlpha(200),
                letterSpacing: 1.2,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                if (holderName.isNotEmpty)
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'NAME',
                        style: GoogleFonts.inter(
                          fontSize: 9,
                          color: Colors.white.withAlpha(140),
                          letterSpacing: 1.2,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        holderName.toUpperCase(),
                        style: GoogleFonts.inter(
                          fontSize: 12,
                          color: Colors.white.withAlpha(230),
                          fontWeight: FontWeight.w600,
                          letterSpacing: 0.4,
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
                        'EXPIRES',
                        style: GoogleFonts.inter(
                          fontSize: 9,
                          color: Colors.white.withAlpha(140),
                          letterSpacing: 1.2,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        validityDate,
                        style: GoogleFonts.inter(
                          fontSize: 12,
                          color: Colors.white.withAlpha(230),
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                const SizedBox(width: 10),
                Icon(Icons.lock_rounded, size: 14, color: Colors.white.withAlpha(160)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
