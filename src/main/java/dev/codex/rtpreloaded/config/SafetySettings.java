package dev.codex.rtpreloaded.config;

import java.util.Set;
import org.bukkit.Material;

public record SafetySettings(
        boolean avoidLiquids,
        boolean avoidLava,
        boolean allowLeaves,
        Set<Material> unsafeGround
) {
}
