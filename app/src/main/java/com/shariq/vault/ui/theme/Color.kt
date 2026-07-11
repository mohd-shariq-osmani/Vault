package com.shariq.vault.ui.theme

import androidx.compose.ui.graphics.Color

// ── Cinema Dark Palette ─────────────────────────────────────────────────────
val CinemaDeep     = Color(0xFF020203)   // Deepest background (OLED-safe, not pure black)
val CinemaBase     = Color(0xFF080810)   // Base screen background
val CinemaElevated = Color(0xFF0E0E1A)   // Cards, modals, elevated surfaces
val CinemaSurface  = Color(0xFF14142A)   // Input fields, list item rows
val CinemaStroke   = Color(0x1AFFFFFF)   // Hairline borders (rgba white 10%)

// ── Text Hierarchy ───────────────────────────────────────────────────────────
val TextPrimary    = Color(0xFFEDEDEF)   // Primary readable text
val TextSecondary  = Color(0xFF8A8F98)   // Muted / label text
val TextMuted      = Color(0xFF50535C)   // Placeholder / disabled

// ── Accent Colors ────────────────────────────────────────────────────────────
val AccentIndigo   = Color(0xFF6C8EFF)   // Primary accent – electric indigo (trust/premium)
val AccentGlow     = Color(0x336C8EFF)   // Indigo glow (20% alpha) for shadows/halos
val AccentEmerald  = Color(0xFF05C989)   // Success/secure indicators
val AccentRed      = Color(0xFFFF4757)   // Destructive actions

// ── Legacy compat aliases (kept so existing references compile) ──────────────
val ObsidianBlack  = CinemaBase
val DarkSurface    = CinemaElevated
val BorderGray     = CinemaStroke
val CyberCyan      = AccentIndigo
val NeonPurple     = Color(0xFF9D6FFF)
val MintGreen      = AccentEmerald
val CyberPink      = AccentRed
val NeonOrange     = Color(0xFFFF9F00)

// ── Document Card Gradients (per type) ──────────────────────────────────────
val CardGradientBluePurple  = listOf(Color(0xFF1A1A4E), Color(0xFF2D2B8A), Color(0xFF1E3A8A))
val CardGradientEmerald     = listOf(Color(0xFF064E3B), Color(0xFF065F46), Color(0xFF0D4F3C))
val CardGradientSunset      = listOf(Color(0xFF7C2D12), Color(0xFF92400E), Color(0xFF6B1E0A))
val CardGradientDeepSpace   = listOf(Color(0xFF0F0F2E), Color(0xFF1A1A4E), Color(0xFF0D1635))
val CardGradientDarkPurple  = listOf(Color(0xFF2D1B69), Color(0xFF4C1D95), Color(0xFF1E0F3B))
val CardGradientNeonVibe    = listOf(Color(0xFF1A0533), Color(0xFF3B0764), Color(0xFF1E0A2E))
val CardGradientCarbon      = listOf(Color(0xFF111118), Color(0xFF1C1C28), Color(0xFF111118))
