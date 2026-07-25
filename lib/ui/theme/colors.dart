import 'package:flutter/material.dart';

// Apple Dark Theme Palette (iOS / macOS Human Interface Guidelines)
const applePitchBlack = Color(0xFF000000);
const appleGroupedBackground = Color(0xFF000000);
const appleCardBackground = Color(0xFF1C1C1E);
const appleCardSecondary = Color(0xFF2C2C2E);
const appleCardTertiary = Color(0xFF3A3A3C);

const appleBorderStroke = Color(0x1FFFFFFF);
const appleBorderHighlight = Color(0x3DFFFFFF);

// Apple System Text Colors
const textPrimary = Color(0xFFFFFFFF);
const textSecondary = Color(0x99EBEBF5); // 60% white
const textMuted = Color(0x4DEBEBF5);     // 30% white

// Apple Vibrant System Accent Colors
const appleBlue = Color(0xFF0A84FF);
const appleIndigo = Color(0xFF5E5CE6);
const applePurple = Color(0xFFBF5AF2);
const applePink = Color(0xFFFF375F);
const appleRed = Color(0xFFFF453A);
const appleOrange = Color(0xFFFF9F0C);
const appleYellow = Color(0xFFFFD60A);
const appleGreen = Color(0xFF30D158);
const appleTeal = Color(0xFF64D2FF);

// Backward compatible aliases
const cinemaDeep = applePitchBlack;
const cinemaBase = appleGroupedBackground;
const cinemaElevated = appleCardBackground;
const cinemaSurface = appleCardSecondary;
const cinemaStroke = appleBorderStroke;
const accentIndigo = appleBlue;
const accentGlow = Color(0x330A84FF);
const accentEmerald = appleGreen;
const accentRed = appleRed;
const accentOrange = appleOrange;
const accentPurple = applePurple;

// Apple Wallet Metallic / Premium Card Gradients
const List<List<Color>> cardGradients = [
  [Color(0xFF1C1C30), Color(0xFF0A84FF), Color(0xFF0040DD)], // Titanium Blue
  [Color(0xFF0D2818), Color(0xFF30D158), Color(0xFF056526)], // Emerald Green
  [Color(0xFF331400), Color(0xFFFF9F0C), Color(0xFFB35900)], // Burnt Copper
  [Color(0xFF24102C), Color(0xFFBF5AF2), Color(0xFF751E9B)], // Deep Violet
  [Color(0xFF1C252E), Color(0xFF64D2FF), Color(0xFF0077A3)], // Cyan Obsidian
  [Color(0xFF1F1F24), Color(0xFF3A3A40), Color(0xFF141417)], // Space Gray Titanium
];

const List<Color> accentForGradients = [
  appleBlue,
  appleGreen,
  appleOrange,
  applePurple,
  appleTeal,
  Color(0xFFE5E5EA),
];

List<Color> gradientForDoc(int colorIndex) {
  final idx = colorIndex.abs() % cardGradients.length;
  return cardGradients[idx];
}

Color accentForDoc(int colorIndex) {
  final idx = colorIndex.abs() % accentForGradients.length;
  return accentForGradients[idx];
}
