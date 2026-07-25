import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../models/document.dart';
import '../../providers/vault_provider.dart';
import '../../providers/auth_provider.dart';
import '../theme/colors.dart';
import '../widgets/apple_segmented_control.dart';
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
    showModalBottomSheet(
      context: context,
      backgroundColor: appleCardBackground,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      builder: (ctx) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Apple Bottom Sheet Handle
            Center(
              child: Container(
                width: 36,
                height: 5,
                decoration: BoxDecoration(
                  color: Colors.white.withAlpha(50),
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
            ),
            const SizedBox(height: 20),
            Text(
              'Add New Document',
              style: GoogleFonts.inter(
                color: textPrimary,
                fontWeight: FontWeight.w800,
                fontSize: 22,
                letterSpacing: -0.3,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              'Select document type to scan or upload',
              style: GoogleFonts.inter(
                color: textSecondary,
                fontSize: 14,
              ),
            ),
            const SizedBox(height: 16),
            _buildDocTypeOption(ctx, DocumentType.paymentCard, Icons.credit_card_rounded, 'Payment Card', 'Credit, Debit, Visa, Mastercard'),
            _buildDocTypeOption(ctx, DocumentType.aadhaarCard, Icons.badge_rounded, 'Aadhaar Card', 'UIDAI Identity Card'),
            _buildDocTypeOption(ctx, DocumentType.panCard, Icons.article_rounded, 'PAN Card', 'Income Tax Permanent Account Number'),
            _buildDocTypeOption(ctx, DocumentType.driversLicense, Icons.drive_eta_rounded, "Driver's Licence", 'State Transport Driving Licence'),
            _buildDocTypeOption(ctx, DocumentType.vehicleRc, Icons.directions_car_rounded, 'Vehicle RC', 'Registration Certificate'),
            _buildDocTypeOption(ctx, DocumentType.genericId, Icons.card_membership_rounded, 'Generic ID Card', 'Passport, Membership, Employee ID'),
          ],
        ),
      ),
    );
  }

  Widget _buildDocTypeOption(
    BuildContext ctx,
    DocumentType type,
    IconData icon,
    String title,
    String subtitle,
  ) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
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
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          decoration: BoxDecoration(
            color: appleCardSecondary,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: appleBorderStroke),
          ),
          child: Row(
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  color: appleBlue.withAlpha(30),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, color: appleBlue, size: 22),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: GoogleFonts.inter(
                        color: textPrimary,
                        fontWeight: FontWeight.w700,
                        fontSize: 15,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: GoogleFonts.inter(
                        color: textSecondary,
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right_rounded, color: textMuted, size: 20),
            ],
          ),
        ),
      ),
    );
  }

  void _confirmDelete(BuildContext context, VaultDocument doc) {
    HapticFeedback.heavyImpact();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete Document'),
        content: Text('Are you sure you want to delete "${doc.title}"? This action cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel', style: TextStyle(color: textSecondary)),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
              ref.read(vaultProvider.notifier).deleteDocument(doc.id);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Document deleted')),
              );
            },
            child: const Text('Delete', style: TextStyle(color: appleRed, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final vaultState = ref.watch(vaultProvider);

    return Scaffold(
      backgroundColor: applePitchBlack,
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
                                    color: textSecondary,
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
                                color: textPrimary,
                                letterSpacing: -0.5,
                              ),
                            ),
                          ],
                        ),
                        const Spacer(),
                        // Reorder mode toggle
                        AppleTouchable(
                          onTap: () {
                            setState(() => _isReorderMode = !_isReorderMode);
                          },
                          child: Container(
                            width: 40,
                            height: 40,
                            decoration: BoxDecoration(
                              color: _isReorderMode ? appleBlue : appleCardSecondary,
                              shape: BoxShape.circle,
                              border: Border.all(color: appleBorderStroke),
                            ),
                            child: Icon(
                              _isReorderMode ? Icons.check_rounded : Icons.swap_vert_rounded,
                              color: _isReorderMode ? Colors.white : appleBlue,
                              size: 20,
                            ),
                          ),
                        ),
                        const SizedBox(width: 10),
                        // Lock app button
                        AppleTouchable(
                          onTap: () {
                            ref.read(authProvider.notifier).lock();
                          },
                          child: Container(
                            width: 40,
                            height: 40,
                            decoration: BoxDecoration(
                              color: appleCardSecondary,
                              shape: BoxShape.circle,
                              border: Border.all(color: appleBorderStroke),
                            ),
                            child: const Icon(Icons.lock_rounded, color: textSecondary, size: 18),
                          ),
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
                        color: appleCardBackground,
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: appleBorderStroke),
                      ),
                      child: TextField(
                        controller: _searchController,
                        onChanged: (val) => setState(() => _searchQuery = val),
                        style: GoogleFonts.inter(color: textPrimary, fontSize: 15),
                        decoration: InputDecoration(
                          hintText: 'Search cards & documents...',
                          hintStyle: GoogleFonts.inter(color: textMuted, fontSize: 15),
                          prefixIcon: const Icon(Icons.search_rounded, color: textSecondary, size: 20),
                          suffixIcon: _searchQuery.isNotEmpty
                              ? IconButton(
                                  icon: const Icon(Icons.cancel_rounded, color: textMuted, size: 18),
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

                // Apple Sliding Segmented Filter Pills
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(20, 10, 20, 14),
                    child: AppleSegmentedControl(
                      items: _categories,
                      activeLabel: _activeFilter,
                      onSelected: (label) => setState(() => _activeFilter = label),
                    ),
                  ),
                ),

                // Empty State
                if (filteredDocs.isEmpty)
                  SliverFillRemaining(
                    hasScrollBody: false,
                    child: Center(
                      child: Padding(
                        padding: const EdgeInsets.all(32),
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Container(
                              width: 80,
                              height: 80,
                              decoration: BoxDecoration(
                                color: appleCardBackground,
                                shape: BoxShape.circle,
                                border: Border.all(color: appleBorderStroke),
                              ),
                              child: const Icon(
                                Icons.folder_zip_rounded,
                                size: 36,
                                color: textMuted,
                              ),
                            ),
                            const SizedBox(height: 20),
                            Text(
                              _searchQuery.isNotEmpty ? 'No Matching Documents' : 'Vault is Empty',
                              style: GoogleFonts.inter(
                                fontSize: 18,
                                fontWeight: FontWeight.w700,
                                color: textPrimary,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              _searchQuery.isNotEmpty
                                  ? 'Try searching for a different keyword or card type.'
                                  : 'Tap the "+" button below to store your payment cards and identity documents securely.',
                              textAlign: TextAlign.center,
                              style: GoogleFonts.inter(
                                fontSize: 13,
                                color: textSecondary,
                                height: 1.4,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  )
                else
                  // Documents List
                  SliverList(
                    delegate: SliverChildBuilderDelegate(
                      (context, index) {
                        final doc = filteredDocs[index];
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
                          onLongPress: () => _confirmDelete(context, doc),
                          onMoveUp: () {
                            ref.read(vaultProvider.notifier).move(doc.id, true);
                          },
                          onMoveDown: () {
                            ref.read(vaultProvider.notifier).move(doc.id, false);
                          },
                        );
                      },
                      childCount: filteredDocs.length,
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

      // Floating Add Button
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
                color: appleBlue.withAlpha(90),
                blurRadius: 18,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: const Icon(Icons.add_rounded, size: 28, color: Colors.white),
        ),
      ),
    );
  }
}
