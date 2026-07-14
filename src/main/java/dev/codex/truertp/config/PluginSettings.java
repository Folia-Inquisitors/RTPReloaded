package dev.codex.truertp.config;

import java.util.Map;
import java.util.Set;

public record PluginSettings(
        boolean debug,
        int attempts,
        int delaySeconds,
        boolean cancelOnMove,
        long chunkLoadTimeoutTicks,
        boolean useCurrentWorldFirst,
        String defaultWorld,
        SafetySettings safety,
        WorldSettings defaults,
        Set<String> disabledWorlds,
        Map<String, WorldSettings> worlds
) {
    public boolean isWorldDisabled(String worldName) {
        return disabledWorlds.contains(worldName);
    }

    public Set<String> enabledWorldNames() {
        return worlds.entrySet().stream()
                .filter(entry -> entry.getValue().enabled())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
