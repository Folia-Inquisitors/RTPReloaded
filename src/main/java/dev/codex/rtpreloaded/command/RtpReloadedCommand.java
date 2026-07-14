package dev.codex.rtpreloaded.command;

import dev.codex.rtpreloaded.RtpReloadedPlugin;
import dev.codex.rtpreloaded.message.MessageService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class RtpReloadedCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ROOT_COMMANDS = List.of("reload", "debug", "portal");
    private static final List<String> PORTAL_COMMANDS = List.of("create", "pos1", "pos2", "delete", "list", "enable", "disable", "settarget");

    private final RtpReloadedPlugin plugin;
    private final MessageService messages;
    private final Map<UUID, PortalSelection> pendingSelections = new HashMap<>();

    public RtpReloadedCommand(RtpReloadedPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "usage-admin");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!hasPermission(sender, "reload")) {
                messages.send(sender, "no-permission");
                return true;
            }

            plugin.reloadPluginConfig();
            messages.send(sender, "reload-complete");
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            if (!hasPermission(sender, "debug")) {
                messages.send(sender, "no-permission");
                return true;
            }

            boolean enabled = !plugin.configService().settings().debug();
            plugin.configService().setDebug(enabled);
            messages.send(sender, enabled ? "debug-enabled" : "debug-disabled");
            return true;
        }

        if (args[0].equalsIgnoreCase("portal")) {
            return handlePortal(sender, args);
        }

        messages.send(sender, "usage-admin");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(ROOT_COMMANDS, args[0]);
        }

        if (!args[0].equalsIgnoreCase("portal")) {
            return List.of();
        }

        if (args.length == 2) {
            return filter(PORTAL_COMMANDS, args[1]);
        }

        String action = args[1].toLowerCase(java.util.Locale.ROOT);
        if (args.length == 3) {
            if (action.equals("create")) {
                return List.of();
            }
            if (action.equals("pos1") || action.equals("pos2")
                    || action.equals("delete") || action.equals("enable") || action.equals("disable")
                    || action.equals("settarget")) {
                return filter(portalNames(), args[2]);
            }
        }

        if (args.length == 4 && (action.equals("create") || action.equals("settarget"))) {
            return filter(worldNames(), args[3]);
        }

        return List.of();
    }

    private boolean handlePortal(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "portal")) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            messages.send(sender, "usage-portal");
            return true;
        }

        String action = args[1].toLowerCase(java.util.Locale.ROOT);
        return switch (action) {
            case "create" -> createPortal(sender, args);
            case "pos1" -> setPortalPosition(sender, args, true);
            case "pos2" -> setPortalPosition(sender, args, false);
            case "delete" -> deletePortal(sender, args);
            case "list" -> listPortals(sender);
            case "enable" -> setPortalEnabled(sender, args, true);
            case "disable" -> setPortalEnabled(sender, args, false);
            case "settarget" -> setPortalTarget(sender, args);
            default -> {
                messages.send(sender, "usage-portal");
                yield true;
            }
        };
    }

    private boolean createPortal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (args.length != 4) {
            messages.send(sender, "usage-portal");
            return true;
        }

        String name = args[2];
        World targetWorld = Bukkit.getWorld(args[3]);
        if (targetWorld == null) {
            messages.send(sender, "world-not-found");
            return true;
        }

        String path = portalPath(name);
        if (portalExists(name) && portalHasBothCorners(path)) {
            plugin.getConfig().set(path + ".enabled", true);
            plugin.getConfig().set(path + ".target-world", targetWorld.getName());
            saveAndReload();
            messages.send(sender, "portal-ready", Map.of("portal", name));
            return true;
        }

        PortalSelection selection = pendingSelections.get(player.getUniqueId());
        plugin.getConfig().set(path + ".enabled", true);
        plugin.getConfig().set(path + ".target-world", targetWorld.getName());

        if (selection != null && selection.hasBothCorners()) {
            Location first = selection.pos1();
            Location second = selection.pos2();
            if (!first.getWorld().equals(second.getWorld())) {
                messages.send(sender, "portal-selection-world-mismatch");
                return true;
            }
            plugin.getConfig().set(path + ".portal-world", first.getWorld().getName());
            writeBounds(path, first, second);
            pendingSelections.remove(player.getUniqueId());
            saveAndReload();
            messages.send(sender, "portal-ready", Map.of("portal", name));
            return true;
        }

        plugin.getConfig().set(path + ".portal-world", player.getWorld().getName());
        saveAndReload();
        messages.send(sender, "portal-created", Map.of(
                "portal", name,
                "portal_world", player.getWorld().getName(),
                "target_world", targetWorld.getName()
        ));
        return true;
    }

    private boolean setPortalPosition(CommandSender sender, String[] args, boolean firstCorner) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (args.length != 2 && args.length != 3) {
            messages.send(sender, "usage-portal");
            return true;
        }

        Location current = selectedPortalLocation(player);
        showCornerMarker(player, current);

        if (args.length == 2) {
            PortalSelection selection = pendingSelections.getOrDefault(player.getUniqueId(), PortalSelection.empty());
            selection = firstCorner ? selection.withPos1(current) : selection.withPos2(current);
            pendingSelections.put(player.getUniqueId(), selection);

            messages.send(sender, firstCorner ? "portal-selection-pos1-set" : "portal-selection-pos2-set", pointPlaceholders(current));
            if (selection.hasBothCorners()) {
                messages.send(sender, "portal-selection-ready");
            }
            return true;
        }

        String name = args[2];
        if (!portalExists(name)) {
            messages.send(sender, "portal-not-found-use-create", Map.of("portal", name));
            return true;
        }

        String path = portalPath(name);
        String otherPath = path + (firstCorner ? ".max" : ".min");

        if (hasPoint(otherPath)) {
            int otherX = plugin.getConfig().getInt(otherPath + ".x");
            int otherY = plugin.getConfig().getInt(otherPath + ".y");
            int otherZ = plugin.getConfig().getInt(otherPath + ".z");
            writePoint(path + ".min", Math.min(current.getBlockX(), otherX), Math.min(current.getBlockY(), otherY), Math.min(current.getBlockZ(), otherZ));
            writePoint(path + ".max", Math.max(current.getBlockX(), otherX), Math.max(current.getBlockY(), otherY), Math.max(current.getBlockZ(), otherZ));
        } else {
            writePoint(path + (firstCorner ? ".min" : ".max"), current.getBlockX(), current.getBlockY(), current.getBlockZ());
        }

        plugin.getConfig().set(path + ".portal-world", player.getWorld().getName());
        saveAndReload();

        Map<String, String> placeholders = new HashMap<>(pointPlaceholders(current));
        placeholders.put("portal", name);
        messages.send(sender, firstCorner ? "portal-pos1-set" : "portal-pos2-set", placeholders);
        if (portalReady(path)) {
            messages.send(sender, "portal-ready", Map.of("portal", name));
        }
        return true;
    }

    private boolean deletePortal(CommandSender sender, String[] args) {
        if (args.length != 3) {
            messages.send(sender, "usage-portal");
            return true;
        }

        String name = args[2];
        if (!portalExists(name)) {
            messages.send(sender, "portal-not-found", Map.of("portal", name));
            return true;
        }

        plugin.getConfig().set(portalPath(name), null);
        saveAndReload();
        messages.send(sender, "portal-deleted", Map.of("portal", name));
        return true;
    }

    private boolean listPortals(CommandSender sender) {
        List<String> names = portalNames();
        if (names.isEmpty()) {
            messages.send(sender, "portal-list-empty");
            return true;
        }

        messages.send(sender, "portal-list", Map.of("portals", String.join(", ", names)));
        return true;
    }

    private boolean setPortalEnabled(CommandSender sender, String[] args, boolean enabled) {
        if (args.length != 3) {
            messages.send(sender, "usage-portal");
            return true;
        }

        String name = args[2];
        if (!portalExists(name)) {
            messages.send(sender, "portal-not-found", Map.of("portal", name));
            return true;
        }

        plugin.getConfig().set(portalPath(name) + ".enabled", enabled);
        saveAndReload();
        messages.send(sender, enabled ? "portal-enabled" : "portal-disabled", Map.of("portal", name));
        return true;
    }

    private boolean setPortalTarget(CommandSender sender, String[] args) {
        if (args.length != 4) {
            messages.send(sender, "usage-portal");
            return true;
        }

        String name = args[2];
        if (!portalExists(name)) {
            messages.send(sender, "portal-not-found", Map.of("portal", name));
            return true;
        }

        World targetWorld = Bukkit.getWorld(args[3]);
        if (targetWorld == null) {
            messages.send(sender, "world-not-found");
            return true;
        }

        String path = portalPath(name);
        plugin.getConfig().set(path + ".target-world", targetWorld.getName());
        saveAndReload();
        messages.send(sender, "portal-target-set", Map.of("portal", name, "target_world", targetWorld.getName()));
        if (portalReady(path)) {
            messages.send(sender, "portal-ready", Map.of("portal", name));
        }
        return true;
    }

    private Location selectedPortalLocation(Player player) {
        Block target = player.getTargetBlockExact(6);
        if (target != null) {
            return target.getLocation();
        }
        return player.getLocation().getBlock().getLocation();
    }

    private void showCornerMarker(Player player, Location selectedLocation) {
        Location markerLocation = selectedLocation.getBlock().getLocation();
        BlockData original = markerLocation.getBlock().getBlockData();
        player.sendBlockChange(markerLocation, Material.GLOWSTONE.createBlockData());
        player.getScheduler().runDelayed(plugin, task -> {
            if (player.isOnline()) {
                player.sendBlockChange(markerLocation, original);
            }
        }, null, 20L * 20L);
    }

    private void writeBounds(String path, Location first, Location second) {
        writePoint(path + ".min", Math.min(first.getBlockX(), second.getBlockX()), Math.min(first.getBlockY(), second.getBlockY()), Math.min(first.getBlockZ(), second.getBlockZ()));
        writePoint(path + ".max", Math.max(first.getBlockX(), second.getBlockX()), Math.max(first.getBlockY(), second.getBlockY()), Math.max(first.getBlockZ(), second.getBlockZ()));
    }

    private void writePoint(String path, int x, int y, int z) {
        plugin.getConfig().set(path + ".x", x);
        plugin.getConfig().set(path + ".y", y);
        plugin.getConfig().set(path + ".z", z);
    }

    private Map<String, String> pointPlaceholders(Location location) {
        return Map.of(
                "world", location.getWorld().getName(),
                "x", Integer.toString(location.getBlockX()),
                "y", Integer.toString(location.getBlockY()),
                "z", Integer.toString(location.getBlockZ())
        );
    }

    private boolean portalReady(String path) {
        return plugin.getConfig().isString(path + ".portal-world")
                && plugin.getConfig().isString(path + ".target-world")
                && portalHasBothCorners(path);
    }

    private boolean portalHasBothCorners(String path) {
        return hasPoint(path + ".min") && hasPoint(path + ".max");
    }

    private boolean hasPoint(String path) {
        return plugin.getConfig().isInt(path + ".x")
                && plugin.getConfig().isInt(path + ".y")
                && plugin.getConfig().isInt(path + ".z");
    }

    private boolean portalExists(String name) {
        return plugin.getConfig().isConfigurationSection(portalPath(name));
    }

    private String portalPath(String name) {
        return "portals." + name;
    }

    private List<String> portalNames() {
        ConfigurationSection portals = plugin.getConfig().getConfigurationSection("portals");
        if (portals == null) {
            return List.of();
        }
        return new ArrayList<>(portals.getKeys(false));
    }

    private List<String> worldNames() {
        List<String> names = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            names.add(world.getName());
        }
        return names;
    }

    private List<String> filter(List<String> values, String prefix) {
        String lowerPrefix = prefix.toLowerCase(java.util.Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(java.util.Locale.ROOT).startsWith(lowerPrefix))
                .toList();
    }

    private void saveAndReload() {
        plugin.saveConfig();
        plugin.reloadPluginConfig();
    }

    private boolean hasPermission(CommandSender sender, String node) {
        return sender.hasPermission("rtpreloaded." + node) || sender.hasPermission("truertp." + node);
    }
    private record PortalSelection(Location pos1, Location pos2) {
        static PortalSelection empty() {
            return new PortalSelection(null, null);
        }

        PortalSelection withPos1(Location location) {
            return new PortalSelection(location, pos2);
        }

        PortalSelection withPos2(Location location) {
            return new PortalSelection(pos1, location);
        }

        boolean hasBothCorners() {
            return pos1 != null && pos2 != null;
        }
    }
}






