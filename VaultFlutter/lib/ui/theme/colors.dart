import 'package:flutter/material.dart';

const cinemaDeep = Color(0xFF020203);
const cinemaBase = Color(0xFF080810);
const cinemaElevated = Color(0xFF0E0E1A);
const cinemaSurface = Color(0xFF14142A);
const cinemaStroke = Color(0x1AFFFFFF);
const textPrimary = Color(0xFFEDEDEF);
const textSecondary = Color(0xFF8A8F98);
const textMuted = Color(0xFF50535C);
const accentIndigo = Color(0xFF6C8EFF);
const accentGlow = Color(0x336C8EFF);
const accentEmerald = Color(0xFF05C989);
const accentRed = Color(0xFFFF4757);
const accentOrange = Color(0xFFFF9F00);
const accentPurple = Color(0xFF9D6FFF);

const List<List<Color>> cardGradients = [
  [Color(0xFF1A1A4E), Color(0xFF2D2B8A), Color(0xFF1E3A8A)],
  [Color(0xFF064E3B), Color(0xFF065F46), Color(0xFF0D4F3C)],
  [Color(0xFF7C2D12), Color(0xFF92400E), Color(0xFF6B1E0A)],
  [Color(0xFF0F0F2E), Color(0xFF1A1A4E), Color(0xFF0D1635)],
  [Color(0xFF2D1B69), Color(0xFF4C1D95), Color(0xFF1E0F3B)],
];

const List<Color> accentForGradients = [
  accentIndigo,
  accentEmerald,
  accentOrange,
  accentIndigo,
  accentPurple,
];

List<Color> gradientForDoc(int colorIndex) {
  final idx = colorIndex.abs() % cardGradients.length;
  return cardGradients[idx];
}

Color accentForDoc(int colorIndex) {
  final idx = colorIndex.abs() % accentForGradients.length;
  return accentForGradients[idx];
}
