import '../models/document.dart';

class OcrAutofill {
  static const _headerKeywords = {
    'government',
    'india',
    'of',
    'republic',
    'authority',
    'unique',
    'identification',
    'motor',
    'vehicle',
    'income',
    'tax',
    'department',
    'permanent',
    'account',
    'number',
    'card',
    'driving',
    'licence',
    'license',
    'registration',
    'certificate',
    'transport',
    'rtto',
    'rto',
  };

  static String _cleanNumericString(String s) {
    return s
        .replaceAll('O', '0')
        .replaceAll('o', '0')
        .replaceAll('I', '1')
        .replaceAll('l', '1')
        .replaceAll('L', '1')
        .replaceAll('S', '5')
        .replaceAll('Z', '2')
        .replaceAll('B', '8');
  }

  static bool _isNameCandidate(String line) {
    final trimmed = line.trim();
    if (trimmed.length < 4) return false;
    if (trimmed.contains(RegExp(r'\d'))) return false;
    final lower = trimmed.toLowerCase();
    if (_headerKeywords.contains(lower)) return false;
    final alpha = trimmed.replaceAll(RegExp(r'[^a-zA-Z ]'), '');
    return alpha.length > trimmed.length * 0.7;
  }

  static String? _getValueFromLine(String line, String keyword) {
    final lower = line.toLowerCase();
    final kLower = keyword.toLowerCase();
    final idx = lower.indexOf(kLower);
    if (idx == -1) return null;
    final afterKeyword = line.substring(idx + keyword.length).trim();
    final colonIdx = afterKeyword.indexOf(':');
    if (colonIdx != -1) {
      final val = afterKeyword.substring(colonIdx + 1).trim();
      if (val.isNotEmpty) return val;
    }
    if (afterKeyword.isNotEmpty && !afterKeyword.toLowerCase().contains(kLower)) {
      return afterKeyword;
    }
    return null;
  }

  static String? _getValueAfterLabel(List<String> lines, int labelIdx) {
    for (int offset = 1; offset <= 2; offset++) {
      final nextIdx = labelIdx + offset;
      if (nextIdx >= lines.length) break;
      final next = lines[nextIdx].trim();
      if (next.isNotEmpty && !_headerKeywords.contains(next.toLowerCase())) {
        return next;
      }
    }
    return null;
  }

  static String? _findValueForLabel(List<String> lines, int idx, String keyword) {
    // Try same-line first
    final sameLine = _getValueFromLine(lines[idx], keyword);
    if (sameLine != null && sameLine.isNotEmpty) return sameLine;
    // Then try next lines
    return _getValueAfterLabel(lines, idx);
  }

  static Map<String, String?> runAutoFill(String text, DocumentType type) {
    final lines =
        text.split('\n').map((l) => l.trim()).where((l) => l.isNotEmpty).toList();
    final result = <String, String?>{};

    switch (type) {
      case DocumentType.paymentCard:
        _fillPaymentCard(lines, result);
      case DocumentType.aadhaarCard:
        _fillAadhaar(lines, result);
      case DocumentType.panCard:
        _fillPan(lines, result);
      case DocumentType.driversLicense:
        _fillDL(lines, result);
      case DocumentType.vehicleRc:
        _fillRC(lines, result);
      case DocumentType.genericId:
        _fillGenericId(lines, result);
    }

    return result;
  }

  static bool _isPaymentCardNameCandidate(String line) {
    if (!_isNameCandidate(line)) return false;
    final lower = line.toLowerCase();
    final blacklist = {
      'bank', 'sbi', 'hdfc', 'icici', 'axis', 'kotak', 'pnb', 'hsbc', 'chase', 'citi', 'capital', 'wellsfargo', 'bofa',
      'visa', 'mastercard', 'master', 'amex', 'rupay', 'discover', 'diners', 'debit', 'credit', 'card', 'gold', 'platinum',
      'signature', 'classic', 'premier', 'priority', 'american express'
    };
    for (final word in blacklist) {
      if (lower.contains(word)) return false;
    }
    return true;
  }

  static void _fillPaymentCard(List<String> lines, Map<String, String?> result) {
    for (int i = 0; i < lines.length; i++) {
      final line = lines[i];
      // Card number: 16 consecutive digits with possible spaces/dashes
      final cardMatch = RegExp(r'[\d\s\-]{16,23}').firstMatch(line);
      if (cardMatch != null) {
        final numStr =
            _cleanNumericString(cardMatch.group(0)!.replaceAll(RegExp(r'[\s\-]'), ''));
        if (numStr.length == 16 && RegExp(r'^\d+$').hasMatch(numStr)) {
          result['cardNumber'] = numStr;
        }
      }
      // Expiry MM/YY or MM/YYYY
      final expiryMatch = RegExp(r'\b(0[1-9]|1[0-2])\s?[/\-.]\s?(\d{2,4})\b').firstMatch(line);
      if (expiryMatch != null && result['cardExpiry'] == null) {
        final mm = expiryMatch.group(1)!;
        final yy = expiryMatch.group(2)!;
        result['cardExpiry'] = '$mm/${yy.length == 4 ? yy.substring(2) : yy}';
      }
      // CVV
      final lowerLine = line.toLowerCase();
      if (lowerLine.contains('cvv') || lowerLine.contains('cvc') || lowerLine.contains('cid') || lowerLine.contains('security')) {
        final cvvMatch = RegExp(r'\b\d{3,4}\b').firstMatch(line);
        if (cvvMatch != null) {
          result['cardCvv'] = cvvMatch.group(0);
        } else if (i + 1 < lines.length) {
          final nextLine = lines[i + 1];
          final nextMatch = RegExp(r'\b\d{3,4}\b').firstMatch(nextLine);
          if (nextMatch != null) {
            result['cardCvv'] = nextMatch.group(0);
          }
        }
      }
      // Cardholder name
      if (_isPaymentCardNameCandidate(line) && result['cardholderName'] == null) {
        result['cardholderName'] = line.trim();
      }
    }
  }

  static void _fillAadhaar(List<String> lines, Map<String, String?> result) {
    for (int i = 0; i < lines.length; i++) {
      final line = lines[i];
      // Aadhaar number: 12 digits
      final aadharMatch = RegExp(r'\b(\d[\d\s]{10,14}\d)\b').firstMatch(line);
      if (aadharMatch != null && result['aadhaarNumber'] == null) {
        final numStr =
            _cleanNumericString(aadharMatch.group(0)!.replaceAll(RegExp(r'\s'), ''));
        if (numStr.length == 12 && RegExp(r'^\d+$').hasMatch(numStr)) {
          result['aadhaarNumber'] = numStr;
        }
      }
      // DOB
      final dobMatch = RegExp(
              r'\b(DOB|Date of Birth|D\.O\.B\.?)\s*[:\-]?\s*(\d{2}[/\-]\d{2}[/\-]\d{2,4})',
              caseSensitive: false)
          .firstMatch(line);
      if (dobMatch != null && result['aadhaarDob'] == null) {
        result['aadhaarDob'] = dobMatch.group(2);
      }
      // If no DOB found inline, scan upward for date pattern or YOB
      if (result['aadhaarDob'] == null) {
        final dateMatch = RegExp(r'\b(\d{2}[/\-]\d{2}[/\-]\d{2,4})\b').firstMatch(line);
        if (dateMatch != null) {
          result['aadhaarDob'] = dateMatch.group(0);
        } else {
          final yobMatch = RegExp(r'(Year\s*of\s*Birth|YOB)\s*[:\-]?\s*(\d{4})', caseSensitive: false).firstMatch(line);
          if (yobMatch != null) {
            result['aadhaarDob'] = '01/01/${yobMatch.group(2)}';
          }
        }
      }
      // Gender
      final lower = line.toLowerCase();
      if (lower.contains('male') && result['aadhaarGender'] == null) {
        result['aadhaarGender'] = lower.contains('female') ? 'Female' : 'Male';
      }
      if (lower == 'female' || lower.contains('female')) {
        result['aadhaarGender'] = 'Female';
      }
      // Name
      if (_isNameCandidate(line) && result['aadhaarName'] == null) {
        result['aadhaarName'] = line.trim();
      }
    }
  }

  static void _fillPan(List<String> lines, Map<String, String?> result) {
    for (int i = 0; i < lines.length; i++) {
      final line = lines[i];
      // PAN: AAAAA0000A pattern
      final panMatch = RegExp(r'\b([A-Z]{5}[0-9]{4}[A-Z])\b').firstMatch(line);
      if (panMatch != null) {
        result['panNumber'] = panMatch.group(0);
      }
      // DOB
      final dobMatch =
          RegExp(r'\b(\d{2}[/\-]\d{2}[/\-]\d{2,4})\b').firstMatch(line);
      if (dobMatch != null && result['panDob'] == null) {
        result['panDob'] = dobMatch.group(0);
      }
      // Name / Father's name
      if (_isNameCandidate(line)) {
        if (result['panName'] == null) {
          result['panName'] = line.trim();
        } else if (result['panFatherName'] == null) {
          result['panFatherName'] = line.trim();
        }
      }
    }
  }

  static void _fillDL(List<String> lines, Map<String, String?> result) {
    for (int i = 0; i < lines.length; i++) {
      final line = lines[i];
      final lower = line.toLowerCase();

      // DL number: format like MH-12-20200012345
      final dlMatch = RegExp(r'\b([A-Z]{2}[0-9\-]{10,17})\b').firstMatch(line);
      if (dlMatch != null && result['dlNumber'] == null) {
        result['dlNumber'] =
            _cleanNumericString(dlMatch.group(0)!).toUpperCase();
      }

      if (lower.contains('name') || lower.startsWith('name')) {
        final val = _findValueForLabel(lines, i, 'name');
        if (val != null && _isNameCandidate(val) && result['dlHolderName'] == null) {
          result['dlHolderName'] = val;
        }
      }
      if (lower.contains('dob') || lower.contains('date of birth')) {
        final val = _findValueForLabel(lines, i, 'dob');
        if (val != null && result['dlDob'] == null) result['dlDob'] = val;
      }
      if (lower.contains('validity') || lower.contains('expiry') || lower.contains('exp')) {
        final val = _findValueForLabel(lines, i, 'validity');
        if (val != null && result['dlExpiry'] == null) result['dlExpiry'] = val;
      }
      if (lower.contains('state') || lower.contains('rto')) {
        final val = _findValueForLabel(lines, i, 'state');
        if (val != null && result['dlState'] == null) result['dlState'] = val;
      }

      final dobMatch =
          RegExp(r'\b(\d{2}[/\-]\d{2}[/\-]\d{2,4})\b').firstMatch(line);
      if (dobMatch != null && result['dlDob'] == null) {
        result['dlDob'] = dobMatch.group(0);
      }

      if (_isNameCandidate(line) && result['dlHolderName'] == null) {
        result['dlHolderName'] = line.trim();
      }
    }
  }

  static void _fillRC(List<String> lines, Map<String, String?> result) {
    for (int i = 0; i < lines.length; i++) {
      final line = lines[i];
      final lower = line.toLowerCase();

      // RC: state code + district + serial
      final rcMatch = RegExp(r'\b([A-Z]{2}[0-9]{2}[A-Z]{1,2}[0-9]{4})\b').firstMatch(line);
      if (rcMatch != null && result['rcNumber'] == null) {
        result['rcNumber'] =
            _cleanNumericString(rcMatch.group(0)!).toUpperCase();
      }

      if (lower.contains('chassis') || lower.contains('chasis')) {
        final val = _findValueForLabel(lines, i, 'chassis');
        if (val != null && result['rcChassisNumber'] == null) {
          result['rcChassisNumber'] = _cleanNumericString(val).toUpperCase();
        }
      }
      if (lower.contains('engine')) {
        final val = _findValueForLabel(lines, i, 'engine');
        if (val != null && result['rcEngineNumber'] == null) {
          result['rcEngineNumber'] = _cleanNumericString(val).toUpperCase();
        }
      }
      if (lower.contains('owner') || lower.contains('name')) {
        final val = _findValueForLabel(lines, i, 'owner');
        if (val != null && _isNameCandidate(val) && result['rcOwnerName'] == null) {
          result['rcOwnerName'] = val;
        }
      }
      if (lower.contains('valid') || lower.contains('expiry') || lower.contains('upto')) {
        final val = _findValueForLabel(lines, i, 'valid');
        if (val != null && result['rcExpiry'] == null) result['rcExpiry'] = val;
      }

      if (_isNameCandidate(line) && result['rcOwnerName'] == null) {
        result['rcOwnerName'] = line.trim();
      }
    }
  }

  static void _fillGenericId(List<String> lines, Map<String, String?> result) {
    for (int i = 0; i < lines.length; i++) {
      final line = lines[i];
      final lower = line.toLowerCase();

      // ID Number
      if (lower.contains('id') || lower.contains('no') || lower.contains('number') || lower.contains('code') || lower.contains('num')) {
        final val = _findValueForLabel(lines, i, 'id') ?? _findValueForLabel(lines, i, 'number') ?? _findValueForLabel(lines, i, 'no');
        if (val != null && result['genericIdNumber'] == null) {
          result['genericIdNumber'] = val.replaceAll(RegExp(r'[^\w\-]'), '');
        }
      }

      // Expiry Date
      final dateMatch = RegExp(r'\b(\d{2}[/\-.]\d{2}[/\-.]\d{2,4})\b').firstMatch(line);
      if (dateMatch != null && result['genericIdExpiry'] == null) {
        result['genericIdExpiry'] = dateMatch.group(0)!.replaceAll('.', '/').replaceAll('-', '/');
      }

      // Name candidate
      if (_isNameCandidate(line) && result['genericIdName'] == null) {
        result['genericIdName'] = line.trim();
      }
    }
  }
}
