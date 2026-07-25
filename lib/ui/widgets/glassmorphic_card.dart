import 'dart:ui';
import 'package:flutter/material.dart';
import 'apple_touchable.dart';

class GlassmorphicCard extends StatelessWidget {
  final List<Color> gradient;
  final Color glowColor;
  final Widget child;
  final double borderRadius;
  final EdgeInsetsGeometry? padding;
  final VoidCallback? onTap;
  final VoidCallback? onLongPress;

  const GlassmorphicCard({
    super.key,
    required this.gradient,
    required this.glowColor,
    required this.child,
    this.borderRadius = 20,
    this.padding,
    this.onTap,
    this.onLongPress,
  });

  @override
  Widget build(BuildContext context) {
    final borderRad = BorderRadius.circular(borderRadius);

    return AppleTouchable(
      onTap: onTap,
      onLongPress: onLongPress,
      scaleFactor: 0.97,
      child: Container(
        decoration: BoxDecoration(
          borderRadius: borderRad,
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: gradient,
          ),
          border: Border.all(
            color: Colors.white.withAlpha(46), // Subtle Apple specular highlight
            width: 1,
          ),
          boxShadow: [
            BoxShadow(
              color: glowColor.withAlpha(40),
              blurRadius: 24,
              spreadRadius: 0,
              offset: const Offset(0, 8),
            ),
            BoxShadow(
              color: Colors.black.withAlpha(140),
              blurRadius: 16,
              spreadRadius: 0,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: ClipRRect(
          borderRadius: borderRad,
          child: Stack(
            children: [
              // Translucent frosted glass effect
              Positioned.fill(
                child: BackdropFilter(
                  filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
                  child: Container(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [
                          Colors.white.withAlpha(20),
                          Colors.white.withAlpha(5),
                          Colors.transparent,
                        ],
                        stops: const [0.0, 0.4, 1.0],
                      ),
                    ),
                  ),
                ),
              ),
              // Content padding
              Padding(
                padding: padding ?? const EdgeInsets.all(20),
                child: child,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
