import 'package:json_annotation/json_annotation.dart';

part 'document.g.dart';

enum DocumentType {
  paymentCard,
  aadhaarCard,
  panCard,
  driversLicense,
  vehicleRc,
  genericId,
}

@JsonSerializable()
class VaultDocument {
  final String id;
  final String title;
  final DocumentType type;
  final int dateAdded; // milliseconds since epoch
  final int cardColorIndex;

  // Driver's License
  final String? dlNumber;
  final String? dlHolderName;
  final String? dlDob;
  final String? dlExpiry;
  final String? dlState;

  // Vehicle RC
  final String? rcNumber;
  final String? rcOwnerName;
  final String? rcChassisNumber;
  final String? rcEngineNumber;
  final String? rcExpiry;

  // Aadhaar
  final String? aadhaarNumber;
  final String? aadhaarName;
  final String? aadhaarDob;
  final String? aadhaarGender;

  // PAN
  final String? panNumber;
  final String? panName;
  final String? panFatherName;
  final String? panDob;

  // Payment Card
  final String? cardholderName;
  final String? cardNumber;
  final String? cardExpiry;
  final String? cardCvv;
  final String? cardType; // Visa, Mastercard, Amex, RuPay

  // Generic ID
  final String? genericIdNumber;
  final String? genericIdName;
  final String? genericIdExpiry;
  final String? genericIdType;

  // Attachment
  final String? imagePath;
  final String? backImagePath;
  final String? ocrText;

  const VaultDocument({
    required this.id,
    required this.title,
    required this.type,
    required this.dateAdded,
    this.cardColorIndex = 0,
    this.dlNumber,
    this.dlHolderName,
    this.dlDob,
    this.dlExpiry,
    this.dlState,
    this.rcNumber,
    this.rcOwnerName,
    this.rcChassisNumber,
    this.rcEngineNumber,
    this.rcExpiry,
    this.aadhaarNumber,
    this.aadhaarName,
    this.aadhaarDob,
    this.aadhaarGender,
    this.panNumber,
    this.panName,
    this.panFatherName,
    this.panDob,
    this.cardholderName,
    this.cardNumber,
    this.cardExpiry,
    this.cardCvv,
    this.cardType,
    this.genericIdNumber,
    this.genericIdName,
    this.genericIdExpiry,
    this.genericIdType,
    this.imagePath,
    this.backImagePath,
    this.ocrText,
  });

  factory VaultDocument.fromJson(Map<String, dynamic> json) =>
      _$VaultDocumentFromJson(json);

  Map<String, dynamic> toJson() => _$VaultDocumentToJson(this);

  VaultDocument copyWith({
    String? id,
    String? title,
    DocumentType? type,
    int? dateAdded,
    int? cardColorIndex,
    String? dlNumber,
    String? dlHolderName,
    String? dlDob,
    String? dlExpiry,
    String? dlState,
    String? rcNumber,
    String? rcOwnerName,
    String? rcChassisNumber,
    String? rcEngineNumber,
    String? rcExpiry,
    String? aadhaarNumber,
    String? aadhaarName,
    String? aadhaarDob,
    String? aadhaarGender,
    String? panNumber,
    String? panName,
    String? panFatherName,
    String? panDob,
    String? cardholderName,
    String? cardNumber,
    String? cardExpiry,
    String? cardCvv,
    String? cardType,
    String? genericIdNumber,
    String? genericIdName,
    String? genericIdExpiry,
    String? genericIdType,
    String? imagePath,
    String? backImagePath,
    String? ocrText,
  }) {
    return VaultDocument(
      id: id ?? this.id,
      title: title ?? this.title,
      type: type ?? this.type,
      dateAdded: dateAdded ?? this.dateAdded,
      cardColorIndex: cardColorIndex ?? this.cardColorIndex,
      dlNumber: dlNumber ?? this.dlNumber,
      dlHolderName: dlHolderName ?? this.dlHolderName,
      dlDob: dlDob ?? this.dlDob,
      dlExpiry: dlExpiry ?? this.dlExpiry,
      dlState: dlState ?? this.dlState,
      rcNumber: rcNumber ?? this.rcNumber,
      rcOwnerName: rcOwnerName ?? this.rcOwnerName,
      rcChassisNumber: rcChassisNumber ?? this.rcChassisNumber,
      rcEngineNumber: rcEngineNumber ?? this.rcEngineNumber,
      rcExpiry: rcExpiry ?? this.rcExpiry,
      aadhaarNumber: aadhaarNumber ?? this.aadhaarNumber,
      aadhaarName: aadhaarName ?? this.aadhaarName,
      aadhaarDob: aadhaarDob ?? this.aadhaarDob,
      aadhaarGender: aadhaarGender ?? this.aadhaarGender,
      panNumber: panNumber ?? this.panNumber,
      panName: panName ?? this.panName,
      panFatherName: panFatherName ?? this.panFatherName,
      panDob: panDob ?? this.panDob,
      cardholderName: cardholderName ?? this.cardholderName,
      cardNumber: cardNumber ?? this.cardNumber,
      cardExpiry: cardExpiry ?? this.cardExpiry,
      cardCvv: cardCvv ?? this.cardCvv,
      cardType: cardType ?? this.cardType,
      genericIdNumber: genericIdNumber ?? this.genericIdNumber,
      genericIdName: genericIdName ?? this.genericIdName,
      genericIdExpiry: genericIdExpiry ?? this.genericIdExpiry,
      genericIdType: genericIdType ?? this.genericIdType,
      imagePath: imagePath ?? this.imagePath,
      backImagePath: backImagePath ?? this.backImagePath,
      ocrText: ocrText ?? this.ocrText,
    );
  }
}
