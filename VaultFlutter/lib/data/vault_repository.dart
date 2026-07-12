import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:path_provider/path_provider.dart';
import '../models/document.dart';
import 'crypto_manager.dart';

class VaultRepository {
  static const _dataFile = 'vault_documents.bin';

  final CryptoManager _crypto;

  VaultRepository({CryptoManager? crypto})
      : _crypto = crypto ?? CryptoManager();

  Future<Directory> _getStorageDir() async {
    final appDir = await getApplicationDocumentsDirectory();
    final vaultDir = Directory('${appDir.path}/vault_data');
    if (!await vaultDir.exists()) {
      await vaultDir.create(recursive: true);
    }
    return vaultDir;
  }

  Future<File> _getDataFile() async {
    final dir = await _getStorageDir();
    return File('${dir.path}/$_dataFile');
  }

  Future<List<VaultDocument>> loadDocuments() async {
    try {
      final file = await _getDataFile();
      if (!await file.exists()) return [];

      final encryptedBytes = await file.readAsBytes();
      if (encryptedBytes.isEmpty) return [];

      final decryptedBytes = await _crypto.decrypt(encryptedBytes);
      final jsonString = utf8.decode(decryptedBytes);
      final List<dynamic> jsonList = json.decode(jsonString);
      return jsonList
          .map((e) => VaultDocument.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      // If decryption fails or file is corrupted, return empty
      return [];
    }
  }

  Future<void> saveDocuments(List<VaultDocument> documents) async {
    final file = await _getDataFile();
    final jsonList = documents.map((d) => d.toJson()).toList();
    final jsonString = json.encode(jsonList);
    final plainBytes = utf8.encode(jsonString);
    final encryptedBytes = await _crypto.encrypt(Uint8List.fromList(plainBytes));
    await file.writeAsBytes(encryptedBytes, flush: true);
  }

  Future<void> addDocument(
    VaultDocument doc, {
    Uint8List? imageBytes,
    String? ext,
  }) async {
    final docs = await loadDocuments();
    VaultDocument finalDoc = doc;

    if (imageBytes != null && ext != null) {
      final fileName = '${doc.id}.$ext';
      await saveEncryptedImage(fileName, imageBytes);
      finalDoc = doc.copyWith(imagePath: fileName);
    }

    docs.insert(0, finalDoc);
    await saveDocuments(docs);
  }

  Future<void> updateDocument(
    VaultDocument doc, {
    Uint8List? imageBytes,
    String? ext,
  }) async {
    final docs = await loadDocuments();
    VaultDocument finalDoc = doc;

    if (imageBytes != null && ext != null) {
      final fileName = '${doc.id}.$ext';
      await saveEncryptedImage(fileName, imageBytes);
      finalDoc = doc.copyWith(imagePath: fileName);
    }

    final idx = docs.indexWhere((d) => d.id == doc.id);
    if (idx != -1) {
      docs[idx] = finalDoc;
    }
    await saveDocuments(docs);
  }

  Future<void> deleteDocument(String id) async {
    final docs = await loadDocuments();
    final doc = docs.firstWhere((d) => d.id == id, orElse: () => docs.first);

    // Delete associated images if any
    if (doc.imagePath != null) {
      await deleteEncryptedImage(doc.imagePath!);
    }
    if (doc.backImagePath != null) {
      await deleteEncryptedImage(doc.backImagePath!);
    }

    docs.removeWhere((d) => d.id == id);
    await saveDocuments(docs);
  }

  Future<void> move(String id, bool moveUp) async {
    final docs = await loadDocuments();
    final idx = docs.indexWhere((d) => d.id == id);
    if (idx == -1) return;

    if (moveUp && idx > 0) {
      final tmp = docs[idx - 1];
      docs[idx - 1] = docs[idx];
      docs[idx] = tmp;
    } else if (!moveUp && idx < docs.length - 1) {
      final tmp = docs[idx + 1];
      docs[idx + 1] = docs[idx];
      docs[idx] = tmp;
    }
    await saveDocuments(docs);
  }

  Future<String> saveEncryptedImage(String fileName, Uint8List bytes) async {
    final dir = await _getStorageDir();
    final encryptedBytes = await _crypto.encrypt(bytes);
    final file = File('${dir.path}/$fileName.enc');
    await file.writeAsBytes(encryptedBytes, flush: true);
    return fileName;
  }

  Future<Uint8List?> loadDecryptedImage(String fileName) async {
    try {
      final dir = await _getStorageDir();
      final file = File('${dir.path}/$fileName.enc');
      if (!await file.exists()) return null;
      final encryptedBytes = await file.readAsBytes();
      return await _crypto.decrypt(encryptedBytes);
    } catch (e) {
      return null;
    }
  }

  Future<void> deleteEncryptedImage(String fileName) async {
    try {
      final dir = await _getStorageDir();
      final file = File('${dir.path}/$fileName.enc');
      if (await file.exists()) {
        await file.delete();
      }
    } catch (_) {}
  }
}
