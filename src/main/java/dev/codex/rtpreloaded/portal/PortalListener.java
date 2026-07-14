package dev.codex.rtpreloaded.portal;

import dev.codex.rtpreloaded.RtpReloadedPlugin;
import dev.codex.rtpreloaded.message.MessageService;
import dev.codex.rtpreloaded.teleport.TeleportService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PortalListener implements Listener {
    private static final long PORTAL_COOLDOWN_MILLIS = 3_000L;

    private final RtpReloadedPlugin plugin;
    private final TeleportService teleportService;
    private final MessageService messages;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PortalListener(RtpReloadedPlugin plugin, TeleportService teleportService, MessageService messages) {
        this.plugin = plugin;
        this.teleportService = teleportService;
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!changedBlock(event.getFrom(), event.getTo())) {
            return;
        }

        Player player = event.getPlayer();
        PortalMatch portal = findPortal(player.getLocation());
        if (portal == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long nextAllowed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now < nextAllowed) {
            return;
        }
        cooldowns.put(player.getUniqueId(), now + PORTAL_COOLDOWN_MILLIS);

        if (!hasPermission(player, "use")) {
            messages.send(player, "no-permission");
            return;
        }

        World targetWorld = Bukkit.getWorld(portal.targetWorld());
        if (targetWorld == null) {
            messages.send(player, "world-not-found");
            plugin.debug("Portal " + portal.name() + " has missing target world " + portal.targetWorld());
            return;
        }

        plugin.debug("Player " + player.getName() + " entered portal " + portal.name() + " targeting " + targetWorld.getName());
        teleportService.teleportFromPortal(player, targetWorld);
    }

    private boolean hasPermission(Player player, String node) {
        return player.hasPermission("rtpreloaded." + node) || player.hasPermission("truertp." + node);
    }
    private PortalMatch findPortal(Location location) {
        ConfigurationSection portals = plugin.getConfig().getConfigurationSection("portals");
        if (portals == null) {
            return null;
        }

        for (String portalName : portals.getKeys(false)) {
            String path = "portals." + portalName;
            if (!plugin.getConfig().getBoolean(path + ".enabled", true)) {
                continue;
            }

            String portalWorld = plugin.getConfig().getString(path + ".portal-world", "");
            if (!location.getWorld().getName().equals(portalWorld)) {
                continue;
            }

            String targetWorld = plugin.getConfig().getString(path + ".target-world", "");
            if (targetWorld.isBlank()) {
                plugin.debug("Portal " + portalName + " ignored: missing target-world");
                continue;
            }

            Bounds bounds = loadBounds(path);
            if (bounds == null) {
                plugin.debug("Portal " + portalName + " ignored: missing min/max bounds");
                continue;
            }

            if (bounds.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
                return new PortalMatch(portalName, targetWorld);
            }
        }

        return null;
    }

    private Bounds loadBounds(String path) {
        String minPath = path + ".min";
        String maxPath = path + ".max";

        if (!hasPoint(minPath) || !hasPoint(maxPath)) {
            // Compatibility for the briefly-used location.min/location.max shape.
            minPath = path + ".location.min";
            maxPath = path + ".location.max";
            if (!hasPoint(minPath) || !hasPoint(maxPath)) {
                return null;
            }
        }

        int minX = Math.min(plugin.getConfig().getInt(minPath + ".x"), plugin.getConfig().getInt(maxPath + ".x"));
        int minY = Math.min(plugin.getConfig().getInt(minPath + ".y"), plugin.getConfig().getInt(maxPath + ".y"));
        int minZ = Math.min(plugin.getConfig().getInt(minPath + ".z"), plugin.getConfig().getInt(maxPath + ".z"));
        int maxX = Math.max(plugin.getConfig().getInt(minPath + ".x"), plugin.getConfig().getInt(maxPath + ".x"));
        int maxY = Math.max(plugin.getConfig().getInt(minPath + ".y"), plugin.getConfig().getInt(maxPath + ".y"));
        int maxZ = Math.max(plugin.getConfig().getInt(minPath + ".z"), plugin.getConfig().getInt(maxPath + ".z"));
        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private boolean hasPoint(String path) {
        return plugin.getConfig().isInt(path + ".x")
                && plugin.getConfig().isInt(path + ".y")
                && plugin.getConfig().isInt(path + ".z");
    }

    private boolean changedBlock(Location from, Location to) {
        if (to == null || !from.getWorld().equals(to.getWorld())) {
            return true;
        }
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }

    private record PortalMatch(String name, String targetWorld) {
    }

    private record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }
    }
}
