import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../models/document.dart';
import '../../providers/vault_provider.dart';
import '../../providers/auth_provider.dart';
import '../theme/colors.dart';
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
    {'label': 'All', 'icon': Icons.grid_view},
    {'label': 'Cards', 'icon': Icons.credit_card},
    {'label': 'IDs', 'icon': Icons.badge},
    {'label': 'Vehicle', 'icon': Icons.directions_car},
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
    showModalBottomSheet(
      context: context,
      backgroundColor: cinemaElevated,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 24, horizontal: 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.only(left: 8, bottom: 16),
              child: Text(
                'Add Document',
                style: GoogleFonts.inter(
                  color: textPrimary,
                  fontWeight: FontWeight.w700,
                  fontSize: 18,
                ),
              ),
            ),
            _buildDocTypeOption(ctx, DocumentType.paymentCard, Icons.credit_card, 'Payment Card'),
            _buildDocTypeOption(ctx, DocumentType.aadhaarCard, Icons.badge, 'Aadhaar Card'),
            _buildDocTypeOption(ctx, DocumentType.panCard, Icons.article, 'PAN Card'),
            _buildDocTypeOption(ctx, DocumentType.driversLicense, Icons.drive_eta, "Driver's Licence"),
            _buildDocTypeOption(ctx, DocumentType.vehicleRc, Icons.directions_car, 'Vehicle RC'),
            _buildDocTypeOption(ctx, DocumentType.genericId, Icons.card_membership, 'Generic ID Card'),
          ],
        ),
      ),
    );
  }

  Widget _buildDocTypeOption(
    BuildContext ctx,
    DocumentType type,
    IconData icon,
    String label,
  ) {
    return ListTile(
      leading: Container(
        width: 40,
        height: 40,
        decoration: BoxDecoration(
          color: accentIndigo.withAlpha(26),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Icon(icon, color: accentIndigo, size: 22),
      ),
      title: Text(label, style: GoogleFonts.inter(color: textPrimary, fontSize: 15)),
      onTap: () {
        Navigator.pop(ctx);
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => AddDocumentScreen(
              documentType: type,
              onSaved: () => Navigator.pop(context),
            ),
          ),
        );
      },
    );
  }

  void _confirmDelete(BuildContext context, VaultDocument doc) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete Document'),
        content: Text('Are you sure you want to permanently delete "${doc.title}"?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel', style: TextStyle(color: textSecondary)),
          ),
          TextButton(
            onPressed: () async {
              Navigator.pop(ctx);
              await ref.read(vaultProvider.notifier).deleteDocument(doc.id);
            },
            child: const Text('Delete', style: TextStyle(color: accentRed)),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final vaultState = ref.watch(vaultProvider);

    return Scaffold(
      backgroundColor: cinemaBase,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(vaultState),
            _buildSearchBar(),
            _buildFilterRow(),
            Expanded(
              child: vaultState.when(
                loading: () => const Center(
                  child: CircularProgressIndicator(color: accentIndigo),
                ),
                error: (e, _) => Center(
                  child: Text('Error: $e', style: const TextStyle(color: accentRed)),
                ),
                data: (docs) {
                  final filtered = _applyFilter(docs);
                  if (filtered.isEmpty) {
                    return _buildEmptyState(docs.isEmpty);
                  }
                  return ListView.builder(
                    padding: const EdgeInsets.only(bottom: 100),
                    itemCount: filtered.length,
                    itemBuilder: (ctx, idx) {
                      final doc = filtered[idx];
                      return DocumentListItem(
                        document: doc,
                        isReorderMode: _isReorderMode,
                        onTap: _isReorderMode
                            ? null
                            : () {
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (_) => ViewDocumentScreen(
                                      documentId: doc.id,
                                      onBack: () => Navigator.pop(context),
                                    ),
                                  ),
                                );
                              },
                        onLongPress: _isReorderMode
                            ? null
                            : () => _confirmDelete(context, doc),
                        onMoveUp: () => ref.read(vaultProvider.notifier).move(doc.id, true),
                        onMoveDown: () => ref.read(vaultProvider.notifier).move(doc.id, false),
                      );
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showAddDocumentMenu,
        tooltip: 'Add Document',
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildHeader(AsyncValue<List<VaultDocument>> vaultState) {
    final count = vaultState.valueOrNull?.length ?? 0;
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [cinemaElevated, cinemaBase],
        ),
      ),
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
      child: Column(
        children: [
          Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: accentIndigo.withAlpha(26),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(Icons.lock, color: accentIndigo, size: 20),
              ),
              const SizedBox(width: 12),
              Text(
                'VAULT',
                style: GoogleFonts.inter(
                  color: textPrimary,
                  fontWeight: FontWeight.w800,
                  fontSize: 24,
                  letterSpacing: 4,
                ),
              ),
              const Spacer(),
              IconButton(
                icon: Icon(
                  _isReorderMode ? Icons.check : Icons.reorder,
                  color: _isReorderMode ? accentEmerald : textSecondary,
                ),
                tooltip: _isReorderMode ? 'Done' : 'Reorder',
                onPressed: () => setState(() => _isReorderMode = !_isReorderMode),
              ),
              IconButton(
                icon: const Icon(Icons.lock_outline, color: textSecondary),
                tooltip: 'Lock',
                onPressed: () => ref.read(authProvider.notifier).lock(),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              const Icon(Icons.shield, size: 14, color: accentEmerald),
              const SizedBox(width: 6),
              Text(
                '$count encrypted item${count == 1 ? '' : 's'} · 256-bit AES',
                style: GoogleFonts.inter(
                  color: accentEmerald,
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSearchBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
      child: TextField(
        controller: _searchController,
        style: GoogleFonts.inter(color: textPrimary, fontSize: 14),
        onChanged: (v) => setState(() => _searchQuery = v),
        decoration: InputDecoration(
          hintText: 'Search documents...',
          prefixIcon: const Icon(Icons.search, color: textSecondary, size: 20),
          suffixIcon: _searchQuery.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.clear, color: textSecondary, size: 18),
                  onPressed: () {
                    _searchController.clear();
                    setState(() => _searchQuery = '');
                  },
                )
              : null,
        ),
      ),
    );
  }

  Widget _buildFilterRow() {
    return SizedBox(
      height: 46,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 2),
        itemCount: _categories.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (ctx, idx) {
          final cat = _categories[idx];
          final String label = cat['label'];
          final IconData icon = cat['icon'];
          final selected = _activeFilter == label;
          return FilterChip(
            showCheckmark: false,
            avatar: Icon(
              icon,
              size: 15,
              color: selected ? Colors.white : textSecondary,
            ),
            label: Text(label),
            selected: selected,
            onSelected: (_) => setState(() => _activeFilter = label),
            backgroundColor: cinemaSurface,
            selectedColor: accentIndigo,
            labelStyle: GoogleFonts.inter(
              color: selected ? Colors.white : textSecondary,
              fontWeight: selected ? FontWeight.bold : FontWeight.normal,
              fontSize: 13,
            ),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(10),
            ),
            side: BorderSide(
              color: selected ? Colors.transparent : cinemaStroke,
              width: 0.8,
            ),
          );
        },
      ),
    );
  }

  Widget _buildEmptyState(bool noDocsAtAll) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                color: cinemaElevated,
                borderRadius: BorderRadius.circular(20),
              ),
              child: const Icon(
                Icons.enhanced_encryption,
                color: textMuted,
                size: 40,
              ),
            ),
            const SizedBox(height: 20),
            Text(
              noDocsAtAll ? 'Vault is empty' : 'No matching documents',
              style: GoogleFonts.inter(
                color: textPrimary,
                fontWeight: FontWeight.w700,
                fontSize: 18,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              noDocsAtAll
                  ? 'Add your first document using the + button below'
                  : 'Try a different search or filter',
              textAlign: TextAlign.center,
              style: GoogleFonts.inter(color: textSecondary, fontSize: 14, height: 1.6),
            ),
          ],
        ),
      ),
    );
  }
}
