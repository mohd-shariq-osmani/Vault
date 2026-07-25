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

// Apple Wallet Metallic / Premium Card Gradients & Swatches
const List<Map<String, dynamic>> cardColorThemes = [
  {
    'name': 'Titanium Blue',
    'gradient': [Color(0xFF0F172A), Color(0xFF0A84FF), Color(0xFF0284C7)],
    'accent': appleBlue,
  },
  {
    'name': 'Emerald Green',
    'gradient': [Color(0xFF064E3B), Color(0xFF30D158), Color(0xFF059669)],
    'accent': appleGreen,
  },
  {
    'name': 'Sunset Copper',
    'gradient': [Color(0xFF451A03), Color(0xFFFF9F0C), Color(0xFFD97706)],
    'accent': appleOrange,
  },
  {
    'name': 'Deep Violet',
    'gradient': [Color(0xFF31103F), Color(0xFFBF5AF2), Color(0xFF9333EA)],
    'accent': applePurple,
  },
  {
    'name': 'Cyan Obsidian',
    'gradient': [Color(0xFF082F49), Color(0xFF64D2FF), Color(0xFF0EA5E9)],
    'accent': appleTeal,
  },
  {
    'name': 'Crimson Rose',
    'gradient': [Color(0xFF4C0519), Color(0xFFFF375F), Color(0xFFE11D48)],
    'accent': applePink,
  },
  {
    'name': 'Space Gray',
    'gradient': [Color(0xFF18181B), Color(0xFF3F3F46), Color(0xFF27272A)],
    'accent': Color(0xFFE5E5EA),
  },
  {
    'name': 'Midnight Gold',
    'gradient': [Color(0xFF2E2000), Color(0xFFFFD60A), Color(0xFFCA8A04)],
    'accent': appleYellow,
  },
];

List<Color> gradientForDoc(int colorIndex) {
  final idx = colorIndex.abs() % cardColorThemes.length;
  return cardColorThemes[idx]['gradient'] as List<Color>;
}

Color accentForDoc(int colorIndex) {
  final idx = colorIndex.abs() % cardColorThemes.length;
  return cardColorThemes[idx]['accent'] as Color;
}
