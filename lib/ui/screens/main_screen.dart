import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../models/document.dart';
import '../../providers/vault_provider.dart';
import '../../providers/auth_provider.dart';
import '../theme/colors.dart';
import '../widgets/apple_segmented_control.dart';
import '../widgets/apple_theme_toggle.dart';
import '../widgets/apple_touchable.dart';
import '../widgets/document_list_item.dart';
import 'add_document_screen.dart';
import 'view_document_screen.dart';

class MainScreen extends ConsumerStatefulWidget {
  const MainScreen({super.key});

  @override
  ConsumerState<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends ConsumerState<MainScreen> {
  bool _isReorderMode = false;
  String _searchQuery = '';
  String _activeFilter = 'All';
  final TextEditingController _searchController = TextEditingController();

  final List<Map<String, dynamic>> _categories = [
    {'label': 'All', 'icon': Icons.grid_view_rounded},
    {'label': 'Cards', 'icon': Icons.credit_card_rounded},
    {'label': 'IDs', 'icon': Icons.badge_rounded},
    {'label': 'Vehicle', 'icon': Icons.directions_car_rounded},
  ];

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  List<VaultDocument> _applyFilter(List<VaultDocument> docs) {
    List<VaultDocument> filtered;
    switch (_activeFilter) {
      case 'Cards':
        filtered = docs.where((d) => d.type == DocumentType.paymentCard).toList();
      case 'IDs':
        filtered = docs
            .where((d) =>
                d.type == DocumentType.aadhaarCard ||
                d.type == DocumentType.panCard ||
                d.type == DocumentType.genericId)
            .toList();
      case 'Vehicle':
        filtered = docs
            .where((d) =>
                d.type == DocumentType.driversLicense ||
                d.type == DocumentType.vehicleRc)
            .toList();
      default:
        filtered = docs;
    }

    if (_searchQuery.isNotEmpty) {
      final query = _searchQuery.toLowerCase();
      filtered = filtered.where((d) {
        return d.title.toLowerCase().contains(query) ||
            (d.cardNumber?.contains(query) ?? false) ||
            (d.aadhaarNumber?.contains(query) ?? false) ||
            (d.panNumber?.toLowerCase().contains(query) ?? false) ||
            (d.dlNumber?.toLowerCase().contains(query) ?? false) ||
            (d.rcNumber?.toLowerCase().contains(query) ?? false) ||
            (d.genericIdNumber?.toLowerCase().contains(query) ?? false);
      }).toList();
    }
    return filtered;
  }

  void _showAddDocumentMenu() {
    HapticFeedback.mediumImpact();
    final isLight = Theme.of(context).brightness == Brightness.light;
    final sheetBg = isLight ? Colors.white : appleCardBackground;
    final titleColor = isLight ? lightTextPrimary : textPrimary;
    final subColor = isLight ? lightTextSecondary : textSecondary;
    final itemBg = isLight ? const Color(0xFFF2F2F7) : appleCardSecondary;

    showModalBottomSheet(
      context: context,
      backgroundColor: sheetBg,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Center(
                  child: Container(
                    width: 36,
                    height: 4,
                    decoration: BoxDecoration(
                      color: isLight ? Colors.black12 : Colors.white24,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Text(
                  'Add New Item',
                  style: GoogleFonts.inter(
                    fontSize: 20,
                    fontWeight: FontWeight.w700,
                    color: titleColor,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  'Select document type to securely store in Vault',
                  style: GoogleFonts.inter(
                    fontSize: 13,
                    color: subColor,
                  ),
                ),
                const SizedBox(height: 20),
                _buildAddMenuItem(
                  ctx: ctx,
                  icon: Icons.credit_card_rounded,
                  title: 'Payment Card',
                  subtitle: 'Credit, Debit, Visa, Mastercard, Amex, RuPay',
                  type: DocumentType.paymentCard,
                  bgColor: itemBg,
                ),
                _buildAddMenuItem(
                  ctx: ctx,
                  icon: Icons.badge_rounded,
                  title: 'Aadhaar Card',
                  subtitle: '12-digit Indian National Identity',
                  type: DocumentType.aadhaarCard,
                  bgColor: itemBg,
                ),
                _buildAddMenuItem(
                  ctx: ctx,
                  icon: Icons.article_rounded,
                  title: 'PAN Card',
                  subtitle: '10-char Income Tax Permanent Account',
                  type: DocumentType.panCard,
                  bgColor: itemBg,
                ),
                _buildAddMenuItem(
                  ctx: ctx,
                  icon: Icons.drive_eta_rounded,
                  title: "Driver's Licence",
                  subtitle: 'Driving permit & holder credentials',
                  type: DocumentType.driversLicense,
                  bgColor: itemBg,
                ),
                _buildAddMenuItem(
                  ctx: ctx,
                  icon: Icons.directions_car_rounded,
                  title: 'Vehicle RC',
                  subtitle: 'Registration certificate & engine details',
                  type: DocumentType.vehicleRc,
                  bgColor: itemBg,
                ),
                _buildAddMenuItem(
                  ctx: ctx,
                  icon: Icons.card_membership_rounded,
                  title: 'Generic ID Card',
                  subtitle: 'Passport, Employee ID, Membership Card',
                  type: DocumentType.genericId,
                  bgColor: itemBg,
                ),
                const SizedBox(height: 8),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildAddMenuItem({
    required BuildContext ctx,
    required IconData icon,
    required String title,
    required String subtitle,
    required DocumentType type,
    required Color bgColor,
  }) {
    final isLight = Theme.of(ctx).brightness == Brightness.light;
    final primaryTextColor = isLight ? lightTextPrimary : textPrimary;
    final secondaryTextColor = isLight ? lightTextSecondary : textSecondary;

    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: AppleTouchable(
        onTap: () {
          Navigator.pop(ctx);
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => AddDocumentScreen(documentType: type),
            ),
          );
        },
        child: Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: bgColor,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: isLight ? const Color(0x1F000000) : appleBorderStroke,
            ),
          ),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: appleBlue.withAlpha(25),
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: appleBlue, size: 20),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: GoogleFonts.inter(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                        color: primaryTextColor,
                      ),
                    ),
                    Text(
                      subtitle,
                      style: GoogleFonts.inter(
                        fontSize: 12,
                        color: secondaryTextColor,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right_rounded, color: secondaryTextColor, size: 20),
            ],
          ),
        ),
      ),
    );
  }

  void _confirmDelete(BuildContext context, String docId, String title) {
    HapticFeedback.heavyImpact();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Delete $title?'),
        content: const Text(
          'This action cannot be undone. All encrypted local files for this document will be permanently deleted.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(
              'Cancel',
              style: GoogleFonts.inter(color: appleBlue, fontWeight: FontWeight.w600),
            ),
          ),
          TextButton(
            onPressed: () async {
              Navigator.pop(ctx);
              await ref.read(vaultProvider.notifier).deleteDocument(docId);
            },
            child: Text(
              'Delete',
              style: GoogleFonts.inter(color: appleRed, fontWeight: FontWeight.w700),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final vaultState = ref.watch(vaultProvider);
    final isLight = Theme.of(context).brightness == Brightness.light;

    final primaryTextColor = isLight ? lightTextPrimary : textPrimary;
    final secondaryTextColor = isLight ? lightTextSecondary : textSecondary;
    final cardBgColor = isLight ? Colors.white : appleCardBackground;
    final searchBorderColor = isLight ? const Color(0x1F000000) : appleBorderStroke;
    final iconBtnBg = isLight ? const Color(0xFFE5E5EA) : appleCardSecondary;

    return Scaffold(
      backgroundColor: isLight ? lightBackground : pitchBlack,
      body: SafeArea(
        child: vaultState.when(
          loading: () => const Center(
            child: CircularProgressIndicator(color: appleBlue),
          ),
          error: (err, st) => Center(
            child: Text('Error loading vault: $err', style: const TextStyle(color: appleRed)),
          ),
          data: (docs) {
            final filteredDocs = _applyFilter(docs);

            return CustomScrollView(
              physics: const BouncingScrollPhysics(),
              slivers: [
                // Apple Large Navigation Header
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(20, 16, 20, 12),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.center,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                  decoration: BoxDecoration(
                                    color: appleGreen.withAlpha(30),
                                    borderRadius: BorderRadius.circular(12),
                                    border: Border.all(color: appleGreen.withAlpha(60)),
                                  ),
                                  child: Row(
                                    children: [
                                      const Icon(Icons.shield_rounded, size: 12, color: appleGreen),
                                      const SizedBox(width: 4),
                                      Text(
                                        '256-bit AES',
                                        style: GoogleFonts.inter(
                                          fontSize: 10,
                                          color: appleGreen,
                                          fontWeight: FontWeight.w700,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Text(
                                  '${docs.length} Items',
                                  style: GoogleFonts.inter(
                                    fontSize: 12,
                                    color: secondaryTextColor,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 6),
                            Text(
                              'Vault',
                              style: GoogleFonts.inter(
                                fontSize: 34,
                                fontWeight: FontWeight.w800,
                                color: primaryTextColor,
                                letterSpacing: -0.5,
                              ),
                            ),
                          ],
                        ),
                        const Spacer(),

                        // Header Actions Row (Theme Toggle, Reorder, Lock)
                        Row(
                          children: [
                            const AppleThemeToggle(),
                            const SizedBox(width: 8),
                            // Reorder mode toggle
                            AppleTouchable(
                              onTap: () {
                                setState(() => _isReorderMode = !_isReorderMode);
                              },
                              child: Container(
                                width: 38,
                                height: 38,
                                decoration: BoxDecoration(
                                  color: _isReorderMode ? appleBlue : iconBtnBg,
                                  shape: BoxShape.circle,
                                  border: Border.all(color: searchBorderColor),
                                ),
                                child: Icon(
                                  _isReorderMode ? Icons.check_rounded : Icons.swap_vert_rounded,
                                  color: _isReorderMode ? Colors.white : appleBlue,
                                  size: 19,
                                ),
                              ),
                            ),
                            const SizedBox(width: 8),
                            // Lock app button
                            AppleTouchable(
                              onTap: () {
                                ref.read(authProvider.notifier).lock();
                              },
                              child: Container(
                                width: 38,
                                height: 38,
                                decoration: BoxDecoration(
                                  color: iconBtnBg,
                                  shape: BoxShape.circle,
                                  border: Border.all(color: searchBorderColor),
                                ),
                                child: Icon(Icons.lock_rounded, color: secondaryTextColor, size: 17),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),

                // Apple Search Bar
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 6),
                    child: Container(
                      height: 44,
                      decoration: BoxDecoration(
                        color: cardBgColor,
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: searchBorderColor),
                        boxShadow: isLight
                            ? [
                                BoxShadow(
                                  color: Colors.black.withAlpha(8),
                                  blurRadius: 6,
                                  offset: const Offset(0, 2),
                                ),
                              ]
                            : [],
                      ),
                      child: TextField(
                        controller: _searchController,
                        onChanged: (val) => setState(() => _searchQuery = val),
                        style: GoogleFonts.inter(color: primaryTextColor, fontSize: 15),
                        decoration: InputDecoration(
                          hintText: 'Search cards & documents...',
                          hintStyle: GoogleFonts.inter(
                            color: isLight ? lightTextMuted : textMuted,
                            fontSize: 15,
                          ),
                          prefixIcon: Icon(Icons.search_rounded, color: secondaryTextColor, size: 20),
                          suffixIcon: _searchQuery.isNotEmpty
                              ? IconButton(
                                  icon: Icon(Icons.cancel_rounded, color: secondaryTextColor, size: 18),
                                  onPressed: () {
                                    _searchController.clear();
                                    setState(() => _searchQuery = '');
                                  },
                                )
                              : null,
                          border: InputBorder.none,
                          enabledBorder: InputBorder.none,
                          focusedBorder: InputBorder.none,
                          contentPadding: const EdgeInsets.symmetric(vertical: 10),
                        ),
                      ),
                    ),
                  ),
                ),

                // Horizontal Category Segment Control Bar
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(20, 10, 20, 16),
                    child: AppleSegmentedControl(
                      items: _categories,
                      activeLabel: _activeFilter,
                      onSelected: (label) => setState(() => _activeFilter = label),
                    ),
                  ),
                ),

                // Document List / Grid
                if (filteredDocs.isEmpty)
                  SliverFillRemaining(
                    hasScrollBody: false,
                    child: Center(
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 32),
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Container(
                              width: 80,
                              height: 80,
                              decoration: BoxDecoration(
                                color: appleBlue.withAlpha(20),
                                shape: BoxShape.circle,
                              ),
                              child: const Icon(Icons.shield_outlined, size: 40, color: appleBlue),
                            ),
                            const SizedBox(height: 20),
                            Text(
                              _searchQuery.isNotEmpty
                                  ? 'No matching documents'
                                  : 'Your Vault is Empty',
                              style: GoogleFonts.inter(
                                fontSize: 20,
                                fontWeight: FontWeight.w700,
                                color: primaryTextColor,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              _searchQuery.isNotEmpty
                                  ? 'Try searching for another keyword or title'
                                  : 'Tap the + button to add your cards, Aadhaar, PAN, or Licence securely.',
                              textAlign: TextAlign.center,
                              style: GoogleFonts.inter(
                                fontSize: 14,
                                color: secondaryTextColor,
                                height: 1.4,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  )
                else
                  SliverPadding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    sliver: SliverList(
                      delegate: SliverChildBuilderDelegate(
                        (ctx, index) {
                          final doc = filteredDocs[index];
                          final isFirst = index == 0;
                          final isLast = index == filteredDocs.length - 1;

                          return DocumentListItem(
                            document: doc,
                            isReorderMode: _isReorderMode,
                            onTap: () {
                              Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (_) => ViewDocumentScreen(documentId: doc.id),
                                ),
                              );
                            },
                            onLongPress: () => _confirmDelete(context, doc.id, doc.title),
                            onMoveUp: !isFirst
                                ? () => ref.read(vaultProvider.notifier).move(doc.id, true)
                                : null,
                            onMoveDown: !isLast
                                ? () => ref.read(vaultProvider.notifier).move(doc.id, false)
                                : null,
                          );
                        },
                        childCount: filteredDocs.length,
                      ),
                    ),
                  ),

                const SliverToBoxAdapter(
                  child: SizedBox(height: 100),
                ),
              ],
            );
          },
        ),
      ),
      floatingActionButton: AppleTouchable(
        onTap: _showAddDocumentMenu,
        child: Container(
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            color: appleBlue,
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(
                color: appleBlue.withAlpha(100),
                blurRadius: 16,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: const Icon(Icons.add_rounded, color: Colors.white, size: 28),
        ),
      ),
    );
  }
}
