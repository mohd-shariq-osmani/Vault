import 'dart:convert';
import 'dart:math';
import 'dart:typed_data';
import 'package:encrypt/encrypt.dart' as enc;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class CryptoManager {
  static const _keyName = 'vault_aes_key';
  static const _ivSize = 12; // GCM standard

  final FlutterSecureStorage _secureStorage;
  String? _cachedBase64Key;

  CryptoManager({FlutterSecureStorage? storage})
      : _secureStorage = storage ?? const FlutterSecureStorage();

  Future<String> _getOrCreateKey() async {
    if (_cachedBase64Key != null) return _cachedBase64Key!;

    final existing = await _secureStorage.read(key: _keyName);
    if (existing != null && existing.isNotEmpty) {
      _cachedBase64Key = existing;
      return existing;
    }

    // Generate 32 random bytes
    final random = Random.secure();
    final keyBytes = Uint8List(32);
    for (int i = 0; i < 32; i++) {
      keyBytes[i] = random.nextInt(256);
    }
    final base64Key = base64Encode(keyBytes);
    await _secureStorage.write(key: _keyName, value: base64Key);
    _cachedBase64Key = base64Key;
    return base64Key;
  }

  Future<Uint8List> encrypt(Uint8List data) async {
    final base64Key = await _getOrCreateKey();
    final key = enc.Key.fromBase64(base64Key);
    final iv = enc.IV.fromSecureRandom(_ivSize);
    final encrypter = enc.Encrypter(enc.AES(key, mode: enc.AESMode.gcm));
    final encrypted = encrypter.encryptBytes(List<int>.from(data), iv: iv);

    // Format: [4 bytes IV length (little-endian)][IV bytes][ciphertext]
    final ivBytes = iv.bytes;
    final ivLength = ivBytes.length;
    final result = BytesBuilder();
    result.add([
      ivLength & 0xFF,
      (ivLength >> 8) & 0xFF,
      (ivLength >> 16) & 0xFF,
      (ivLength >> 24) & 0xFF,
    ]);
    result.add(ivBytes);
    result.add(encrypted.bytes);
    return result.toBytes();
  }

  Future<Uint8List> decrypt(Uint8List encryptedData) async {
    final base64Key = await _getOrCreateKey();
    final key = enc.Key.fromBase64(base64Key);

    // Parse: [4 bytes IV length][IV bytes][ciphertext]
    final ivLength = encryptedData[0] |
        (encryptedData[1] << 8) |
        (encryptedData[2] << 16) |
        (encryptedData[3] << 24);

    final ivBytes = encryptedData.sublist(4, 4 + ivLength);
    final cipherBytes = encryptedData.sublist(4 + ivLength);

    final iv = enc.IV(ivBytes);
    final encrypter = enc.Encrypter(enc.AES(key, mode: enc.AESMode.gcm));
    final decrypted = encrypter.decryptBytes(
      enc.Encrypted(cipherBytes),
      iv: iv,
    );
    return Uint8List.fromList(decrypted);
  }
}
