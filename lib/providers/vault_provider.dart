import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/vault_repository.dart';
import '../models/document.dart';
import 'dart:typed_data';

final vaultRepositoryProvider =
    Provider<VaultRepository>((_) => VaultRepository());

final vaultProvider =
    StateNotifierProvider<VaultNotifier, AsyncValue<List<VaultDocument>>>(
  (ref) =>
      VaultNotifier(ref.watch(vaultRepositoryProvider))..loadDocuments(),
);

class VaultNotifier extends StateNotifier<AsyncValue<List<VaultDocument>>> {
  final VaultRepository _repo;

  VaultNotifier(this._repo) : super(const AsyncValue.loading());

  Future<void> loadDocuments() async {
    state = const AsyncValue.loading();
    try {
      final docs = await _repo.loadDocuments();
      state = AsyncValue.data(docs);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> addDocument(
    VaultDocument doc, {
    Uint8List? imageBytes,
    String? ext,
  }) async {
    await _repo.addDocument(doc, imageBytes: imageBytes, ext: ext);
    await loadDocuments();
  }

  Future<void> updateDocument(
    VaultDocument doc, {
    Uint8List? imageBytes,
    String? ext,
  }) async {
    await _repo.updateDocument(doc, imageBytes: imageBytes, ext: ext);
    await loadDocuments();
  }

  Future<void> deleteDocument(String id) async {
    await _repo.deleteDocument(id);
    await loadDocuments();
  }

  Future<void> move(String id, bool moveUp) async {
    await _repo.move(id, moveUp);
    await loadDocuments();
  }

  Future<Uint8List?> loadImage(String path) =>
      _repo.loadDecryptedImage(path);
}
