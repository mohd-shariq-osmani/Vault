import 'package:flutter/services.dart';

/// Formats credit card number with a space every 4 digits (max 16 digits).
class CreditCardFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    if (newValue.text.length < oldValue.text.length) {
      final int deletedIdx = newValue.selection.end;
      if (oldValue.text.length > deletedIdx && oldValue.text[deletedIdx] == ' ') {
        final cleanText = newValue.text.substring(0, deletedIdx - 1) + newValue.text.substring(deletedIdx);
        final digits = cleanText.replaceAll(RegExp(r'[^\d]'), '');
        final formatted = _format(digits);
        final newOffset = (deletedIdx - 1).clamp(0, formatted.length);
        return TextEditingValue(
          text: formatted,
          selection: TextSelection.collapsed(offset: newOffset),
        );
      }
    }

    final digitsOnly = newValue.text.replaceAll(RegExp(r'[^\d]'), '');
    if (digitsOnly.length > 16) {
      return oldValue;
    }

    final formatted = _format(digitsOnly);
    int selectionIndex = newValue.selection.end;
    int originalDigitsBeforeSelection = newValue.text.substring(0, selectionIndex).replaceAll(RegExp(r'[^\d]'), '').length;
    
    int newSelectionIndex = 0;
    int digitsSeen = 0;
    while (newSelectionIndex < formatted.length && digitsSeen < originalDigitsBeforeSelection) {
      if (formatted[newSelectionIndex] != ' ') {
        digitsSeen++;
      }
      newSelectionIndex++;
    }
    
    if (newSelectionIndex < formatted.length && formatted[newSelectionIndex] == ' ') {
      newSelectionIndex++;
    }

    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: newSelectionIndex),
    );
  }

  String _format(String digits) {
    final buffer = StringBuffer();
    for (int i = 0; i < digits.length; i++) {
      if (i > 0 && i % 4 == 0) buffer.write(' ');
      buffer.write(digits[i]);
    }
    return buffer.toString();
  }
}

/// Formats expiry date as MM/YY (max 4 digits).
class ExpiryDateFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    if (newValue.text.length < oldValue.text.length) {
      final int deletedIdx = newValue.selection.end;
      if (oldValue.text.length > deletedIdx && oldValue.text[deletedIdx] == '/') {
        final cleanText = newValue.text.substring(0, deletedIdx - 1) + newValue.text.substring(deletedIdx);
        final digits = cleanText.replaceAll(RegExp(r'[^\d]'), '');
        final formatted = _format(digits);
        final newOffset = (deletedIdx - 1).clamp(0, formatted.length);
        return TextEditingValue(
          text: formatted,
          selection: TextSelection.collapsed(offset: newOffset),
        );
      }
    }

    var digitsOnly = newValue.text.replaceAll(RegExp(r'[^\d]'), '');
    if (digitsOnly.length > 4) digitsOnly = digitsOnly.substring(0, 4);

    final formatted = _format(digitsOnly);
    int selectionIndex = newValue.selection.end;
    int originalDigitsBeforeSelection = newValue.text.substring(0, selectionIndex).replaceAll(RegExp(r'[^\d]'), '').length;
    
    int newSelectionIndex = 0;
    int digitsSeen = 0;
    while (newSelectionIndex < formatted.length && digitsSeen < originalDigitsBeforeSelection) {
      if (formatted[newSelectionIndex] != '/') {
        digitsSeen++;
      }
      newSelectionIndex++;
    }
    
    if (newSelectionIndex < formatted.length && formatted[newSelectionIndex] == '/') {
      newSelectionIndex++;
    }

    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: newSelectionIndex),
    );
  }

  String _format(String digits) {
    if (digits.length >= 3) {
      return '${digits.substring(0, 2)}/${digits.substring(2)}';
    }
    return digits;
  }
}

/// Formats Aadhaar number with a space every 4 digits (max 12 digits).
class AadhaarFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    if (newValue.text.length < oldValue.text.length) {
      final int deletedIdx = newValue.selection.end;
      if (oldValue.text.length > deletedIdx && oldValue.text[deletedIdx] == ' ') {
        final cleanText = newValue.text.substring(0, deletedIdx - 1) + newValue.text.substring(deletedIdx);
        final digits = cleanText.replaceAll(RegExp(r'[^\d]'), '');
        final formatted = _format(digits);
        final newOffset = (deletedIdx - 1).clamp(0, formatted.length);
        return TextEditingValue(
          text: formatted,
          selection: TextSelection.collapsed(offset: newOffset),
        );
      }
    }

    final digitsOnly = newValue.text.replaceAll(RegExp(r'[^\d]'), '');
    if (digitsOnly.length > 12) return oldValue;

    final formatted = _format(digitsOnly);
    int selectionIndex = newValue.selection.end;
    int originalDigitsBeforeSelection = newValue.text.substring(0, selectionIndex).replaceAll(RegExp(r'[^\d]'), '').length;
    
    int newSelectionIndex = 0;
    int digitsSeen = 0;
    while (newSelectionIndex < formatted.length && digitsSeen < originalDigitsBeforeSelection) {
      if (formatted[newSelectionIndex] != ' ') {
        digitsSeen++;
      }
      newSelectionIndex++;
    }
    
    if (newSelectionIndex < formatted.length && formatted[newSelectionIndex] == ' ') {
      newSelectionIndex++;
    }

    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: newSelectionIndex),
    );
  }

  String _format(String digits) {
    final buffer = StringBuffer();
    for (int i = 0; i < digits.length; i++) {
      if (i > 0 && i % 4 == 0) buffer.write(' ');
      buffer.write(digits[i]);
    }
    return buffer.toString();
  }
}

String maskCardNumber(String? number) {
  if (number == null || number.isEmpty) return '•••• •••• •••• ••••';
  final cleaned = number.replaceAll(' ', '');
  if (cleaned.length >= 4) {
    return '•••• •••• •••• ${cleaned.substring(cleaned.length - 4)}';
  }
  return '•••• •••• •••• ••••';
}

String maskAadhaar(String? number) {
  if (number == null || number.isEmpty) return '•••• •••• ••••';
  final cleaned = number.replaceAll(' ', '');
  if (cleaned.length >= 4) {
    return '•••• •••• ${cleaned.substring(cleaned.length - 4)}';
  }
  return '•••• •••• ••••';
}

String maskPan(String? number) {
  if (number == null || number.isEmpty) return '••••• ••••••';
  final upper = number.toUpperCase();
  if (upper.length >= 4) {
    return '••••• ${upper.substring(upper.length - 4)} •';
  }
  return '•••• ••••••';
}

// Input formatting helpers for initialization
String formatCardNumberInput(String digits) {
  final cleaned = digits.replaceAll(' ', '');
  final buffer = StringBuffer();
  for (int i = 0; i < cleaned.length; i++) {
    if (i > 0 && i % 4 == 0) buffer.write(' ');
    buffer.write(cleaned[i]);
  }
  return buffer.toString();
}

String formatAadhaarInput(String digits) {
  final cleaned = digits.replaceAll(' ', '');
  final buffer = StringBuffer();
  for (int i = 0; i < cleaned.length; i++) {
    if (i > 0 && i % 4 == 0) buffer.write(' ');
    buffer.write(cleaned[i]);
  }
  return buffer.toString();
}

String formatExpiryInput(String digits) {
  final cleaned = digits.replaceAll('/', '').replaceAll(' ', '');
  if (cleaned.length >= 2) {
    return '${cleaned.substring(0, 2)}/${cleaned.substring(2)}';
  }
  return cleaned;
}
