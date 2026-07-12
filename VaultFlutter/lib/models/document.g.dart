// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'document.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

VaultDocument _$VaultDocumentFromJson(Map<String, dynamic> json) =>
    VaultDocument(
      id: json['id'] as String,
      title: json['title'] as String,
      type: $enumDecode(_$DocumentTypeEnumMap, json['type']),
      dateAdded: (json['dateAdded'] as num).toInt(),
      cardColorIndex: (json['cardColorIndex'] as num?)?.toInt() ?? 0,
      dlNumber: json['dlNumber'] as String?,
      dlHolderName: json['dlHolderName'] as String?,
      dlDob: json['dlDob'] as String?,
      dlExpiry: json['dlExpiry'] as String?,
      dlState: json['dlState'] as String?,
      rcNumber: json['rcNumber'] as String?,
      rcOwnerName: json['rcOwnerName'] as String?,
      rcChassisNumber: json['rcChassisNumber'] as String?,
      rcEngineNumber: json['rcEngineNumber'] as String?,
      rcExpiry: json['rcExpiry'] as String?,
      aadhaarNumber: json['aadhaarNumber'] as String?,
      aadhaarName: json['aadhaarName'] as String?,
      aadhaarDob: json['aadhaarDob'] as String?,
      aadhaarGender: json['aadhaarGender'] as String?,
      panNumber: json['panNumber'] as String?,
      panName: json['panName'] as String?,
      panFatherName: json['panFatherName'] as String?,
      panDob: json['panDob'] as String?,
      cardholderName: json['cardholderName'] as String?,
      cardNumber: json['cardNumber'] as String?,
      cardExpiry: json['cardExpiry'] as String?,
      cardCvv: json['cardCvv'] as String?,
      cardType: json['cardType'] as String?,
      genericIdNumber: json['genericIdNumber'] as String?,
      genericIdName: json['genericIdName'] as String?,
      genericIdExpiry: json['genericIdExpiry'] as String?,
      genericIdType: json['genericIdType'] as String?,
      imagePath: json['imagePath'] as String?,
      backImagePath: json['backImagePath'] as String?,
      ocrText: json['ocrText'] as String?,
    );

Map<String, dynamic> _$VaultDocumentToJson(VaultDocument instance) =>
    <String, dynamic>{
      'id': instance.id,
      'title': instance.title,
      'type': _$DocumentTypeEnumMap[instance.type]!,
      'dateAdded': instance.dateAdded,
      'cardColorIndex': instance.cardColorIndex,
      'dlNumber': instance.dlNumber,
      'dlHolderName': instance.dlHolderName,
      'dlDob': instance.dlDob,
      'dlExpiry': instance.dlExpiry,
      'dlState': instance.dlState,
      'rcNumber': instance.rcNumber,
      'rcOwnerName': instance.rcOwnerName,
      'rcChassisNumber': instance.rcChassisNumber,
      'rcEngineNumber': instance.rcEngineNumber,
      'rcExpiry': instance.rcExpiry,
      'aadhaarNumber': instance.aadhaarNumber,
      'aadhaarName': instance.aadhaarName,
      'aadhaarDob': instance.aadhaarDob,
      'aadhaarGender': instance.aadhaarGender,
      'panNumber': instance.panNumber,
      'panName': instance.panName,
      'panFatherName': instance.panFatherName,
      'panDob': instance.panDob,
      'cardholderName': instance.cardholderName,
      'cardNumber': instance.cardNumber,
      'cardExpiry': instance.cardExpiry,
      'cardCvv': instance.cardCvv,
      'cardType': instance.cardType,
      'genericIdNumber': instance.genericIdNumber,
      'genericIdName': instance.genericIdName,
      'genericIdExpiry': instance.genericIdExpiry,
      'genericIdType': instance.genericIdType,
      'imagePath': instance.imagePath,
      'backImagePath': instance.backImagePath,
      'ocrText': instance.ocrText,
    };

const _$DocumentTypeEnumMap = {
  DocumentType.paymentCard: 'paymentCard',
  DocumentType.aadhaarCard: 'aadhaarCard',
  DocumentType.panCard: 'panCard',
  DocumentType.driversLicense: 'driversLicense',
  DocumentType.vehicleRc: 'vehicleRc',
  DocumentType.genericId: 'genericId',
};
