package dev.codex.rtpreloaded.config;

import dev.codex.rtpreloaded.RtpReloadedPlugin;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigService {
    private final RtpReloadedPlugin plugin;
    private PluginSettings settings;

    public ConfigService(RtpReloadedPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();

        SafetySettings safety = loadSafety(config);
        WorldSettings defaults = normalizeRadius(new WorldSettings(
                "__global__",
                true,
                getInt(config, "global.center-x", "random.center-x", 0),
                getInt(config, "global.center-z", "random.center-z", 0),
                Math.max(0, getInt(config, "global.min-radius", "random.min-radius", 0)),
                Math.max(1, getInt(config, "global.max-radius", "random.max-radius", 5000)),
                getInt(config, "global.min-y", "random.min-y", -64),
                getInt(config, "global.max-y", "random.max-y", 320)
        ));

        Set<String> disabledWorlds = loadDisabledWorlds(config);
        Map<String, WorldSettings> worlds = loadWorlds(config, defaults, disabledWorlds);

        this.settings = new PluginSettings(
                config.getBoolean("debug", false),
                Math.max(1, config.getInt("teleport.attempts", 64)),
                Math.max(0, config.getInt("teleport.delay-seconds", 5)),
                config.getBoolean("teleport.cancel-on-move", true),
                Math.max(20L, config.getLong("teleport.chunk-load-timeout-ticks", 100L)),
                config.getBoolean("teleport.use-current-world-first", true),
                config.getString("teleport.default-world", "world"),
                safety,
                defaults,
                disabledWorlds,
                worlds
        );
    }

    public PluginSettings settings() {
        return settings;
    }

    public WorldSettings worldSettings(String worldName) {
        WorldSettings configured = settings.worlds().get(worldName);
        if (configured != null) {
            return configured;
        }

        WorldSettings defaults = settings.defaults();
        return new WorldSettings(
                worldName,
                !settings.isWorldDisabled(worldName),
                defaults.centerX(),
                defaults.centerZ(),
                defaults.minRadius(),
                defaults.maxRadius(),
                defaults.minY(),
                defaults.maxY()
        );
    }

    public void setDebug(boolean enabled) {
        plugin.getConfig().set("debug", enabled);
        plugin.saveConfig();
        reload();
    }

    private Map<String, WorldSettings> loadWorlds(FileConfiguration config, WorldSettings defaults, Set<String> disabledWorlds) {
        Map<String, WorldSettings> worlds = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("worlds");
        if (section == null) {
            return Map.copyOf(worlds);
        }

        for (String worldName : section.getKeys(false)) {
            String path = "worlds." + worldName + ".";
            boolean legacyEnabled = config.getBoolean(path + "enabled", true);
            boolean enabled = legacyEnabled && !disabledWorlds.contains(worldName);

            WorldSettings loaded = new WorldSettings(
                    worldName,
                    enabled,
                    config.getInt(path + "center-x", defaults.centerX()),
                    config.getInt(path + "center-z", defaults.centerZ()),
                    Math.max(0, config.getInt(path + "min-radius", defaults.minRadius())),
                    Math.max(1, config.getInt(path + "max-radius", defaults.maxRadius())),
                    config.getInt(path + "min-y", defaults.minY()),
                    config.getInt(path + "max-y", defaults.maxY())
            );
            worlds.put(worldName, normalizeRadius(loaded));
        }

        return Map.copyOf(worlds);
    }

    private Set<String> loadDisabledWorlds(FileConfiguration config) {
        Set<String> disabledWorlds = new HashSet<>(config.getStringList("disabled-worlds"));

        // Backward compatibility for older configs that used worlds.<name>.enabled: false.
        ConfigurationSection section = config.getConfigurationSection("worlds");
        if (section != null) {
            for (String worldName : section.getKeys(false)) {
                String path = "worlds." + worldName + ".enabled";
                if (config.isBoolean(path) && !config.getBoolean(path)) {
                    disabledWorlds.add(worldName);
                }
            }
        }

        return Set.copyOf(disabledWorlds);
    }

    private SafetySettings loadSafety(FileConfiguration config) {
        Set<Material> unsafeGround = new HashSet<>();
        for (String materialName : config.getStringList("safety.unsafe-ground")) {
            Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
            if (material == null) {
                plugin.getLogger().warning("Unknown material in safety.unsafe-ground: " + materialName);
                continue;
            }
            unsafeGround.add(material);
        }

        return new SafetySettings(
                config.getBoolean("safety.avoid-liquids", true),
                config.getBoolean("safety.avoid-lava", true),
                config.getBoolean("safety.allow-leaves", false),
                Set.copyOf(unsafeGround)
        );
    }

    private int getInt(FileConfiguration config, String primaryPath, String legacyPath, int fallback) {
        if (config.isInt(primaryPath)) {
            return config.getInt(primaryPath);
        }
        return config.getInt(legacyPath, fallback);
    }

    private WorldSettings normalizeRadius(WorldSettings settings) {
        if (settings.maxRadius() >= settings.minRadius()) {
            return settings;
        }

        return new WorldSettings(
                settings.worldName(),
                settings.enabled(),
                settings.centerX(),
                settings.centerZ(),
                settings.maxRadius(),
                settings.minRadius(),
                settings.minY(),
                settings.maxY()
        );
    }
}
