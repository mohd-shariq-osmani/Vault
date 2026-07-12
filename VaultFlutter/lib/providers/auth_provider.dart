import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:local_auth/error_codes.dart' as auth_error;
import 'package:local_auth/local_auth.dart';

final authProvider =
    StateNotifierProvider<AuthNotifier, bool>((_) => AuthNotifier());

class AuthNotifier extends StateNotifier<bool> {
  final LocalAuthentication _auth = LocalAuthentication();
  AuthNotifier() : super(false);

  Future<bool> authenticate() async {
    try {
      final canAuth =
          await _auth.canCheckBiometrics || await _auth.isDeviceSupported();
      if (!canAuth) {
        state = true;
        return true;
      }
      final result = await _auth.authenticate(
        localizedReason: 'Authenticate to access your private documents',
        options: const AuthenticationOptions(
          stickyAuth: true,
          biometricOnly: false,
          useErrorDialogs: true,
        ),
      );
      state = result;
      return result;
    } catch (e) {
      if (e is PlatformException) {
        if (e.code == auth_error.notAvailable ||
            e.code == auth_error.notEnrolled) {
          state = true;
          return true;
        }
      }
      state = false;
      return false;
    }
  }

  void lock() => state = false;
}

final isLaunchingExternalProvider = StateProvider<bool>((ref) => false);
