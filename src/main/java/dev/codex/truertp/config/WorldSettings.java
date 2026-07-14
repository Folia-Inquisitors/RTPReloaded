package dev.codex.truertp.config;

public record WorldSettings(
        String worldName,
        boolean enabled,
        int centerX,
        int centerZ,
        int minRadius,
        int maxRadius,
        int minY,
        int maxY
) {
}
