import 'package:flutter/material.dart';

// Frontend Design Pro: Obsidian Cyber-Vault Color System
const pitchBlack = Color(0xFF000000);
const vaultBackground = Color(0xFF050508);
const vaultSurfaceElevated = Color(0xFF101018);
const vaultSurfaceSecondary = Color(0xFF1B1B26);
const vaultSurfaceTertiary = Color(0xFF282838);

// Borders & Glass Accents
const vaultStroke = Color(0x24FFFFFF);
const vaultStrokeActive = Color(0x52FFFFFF);
const vaultGlowCyan = Color(0x3364D2FF);

// Text Hierarchy
const textPrimary = Color(0xFFFFFFFF);
const textSecondary = Color(0x99EBEBF5); // 60% white
const textMuted = Color(0x4DEBEBF5);     // 30% white

// System Accent Colors
const accentCyan = Color(0xFF64D2FF);
const accentBlue = Color(0xFF0A84FF);
const accentIndigo = Color(0xFF5E5CE6);
const accentPurple = Color(0xFFBF5AF2);
const accentPink = Color(0xFFFF375F);
const accentRed = Color(0xFFFF453A);
const accentOrange = Color(0xFFFF9F0C);
const accentYellow = Color(0xFFFFD60A);
const accentEmerald = Color(0xFF30D158);

// Backward compatibility aliases
const cinemaDeep = pitchBlack;
const cinemaBase = vaultBackground;
const cinemaElevated = vaultSurfaceElevated;
const cinemaSurface = vaultSurfaceSecondary;
const cinemaStroke = vaultStroke;
const applePitchBlack = pitchBlack;
const appleGroupedBackground = vaultBackground;
const appleCardBackground = vaultSurfaceElevated;
const appleCardSecondary = vaultSurfaceSecondary;
const appleCardTertiary = vaultSurfaceTertiary;
const appleBorderStroke = vaultStroke;
const appleBorderHighlight = vaultStrokeActive;
const appleBlue = accentBlue;
const appleIndigo = accentIndigo;
const applePurple = accentPurple;
const applePink = accentPink;
const appleRed = accentRed;
const appleOrange = accentOrange;
const appleYellow = accentYellow;
const appleGreen = accentEmerald;
const appleTeal = accentCyan;
const accentGlow = vaultGlowCyan;

// 8 Premium Obsidian Metallic Card Themes
const List<Map<String, dynamic>> cardColorThemes = [
  {
    'name': 'Titanium Blue',
    'gradient': [Color(0xFF0F172A), Color(0xFF0A84FF), Color(0xFF0284C7)],
    'accent': accentBlue,
  },
  {
    'name': 'Emerald Cyber',
    'gradient': [Color(0xFF064E3B), Color(0xFF30D158), Color(0xFF059669)],
    'accent': accentEmerald,
  },
  {
    'name': 'Sunset Gold',
    'gradient': [Color(0xFF451A03), Color(0xFFFF9F0C), Color(0xFFD97706)],
    'accent': accentOrange,
  },
  {
    'name': 'Vivid Violet',
    'gradient': [Color(0xFF31103F), Color(0xFFBF5AF2), Color(0xFF9333EA)],
    'accent': accentPurple,
  },
  {
    'name': 'Cyan Neon',
    'gradient': [Color(0xFF082F49), Color(0xFF64D2FF), Color(0xFF0EA5E9)],
    'accent': accentCyan,
  },
  {
    'name': 'Crimson Rose',
    'gradient': [Color(0xFF4C0519), Color(0xFFFF375F), Color(0xFFE11D48)],
    'accent': accentPink,
  },
  {
    'name': 'Space Gray',
    'gradient': [Color(0xFF18181B), Color(0xFF3F3F46), Color(0xFF27272A)],
    'accent': Color(0xFFE5E5EA),
  },
  {
    'name': 'Obsidian Gold',
    'gradient': [Color(0xFF2E2000), Color(0xFFFFD60A), Color(0xFFCA8A04)],
    'accent': accentYellow,
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
