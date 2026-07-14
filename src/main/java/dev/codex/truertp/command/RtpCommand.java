package dev.codex.truertp.command;

import dev.codex.truertp.TrueRtpPlugin;
import dev.codex.truertp.message.MessageService;
import dev.codex.truertp.teleport.TeleportService;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class RtpCommand implements CommandExecutor, TabCompleter {
    private final TrueRtpPlugin plugin;
    private final TeleportService teleportService;
    private final MessageService messages;

    public RtpCommand(TrueRtpPlugin plugin, TeleportService teleportService, MessageService messages) {
        this.plugin = plugin;
        this.teleportService = teleportService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("truertp.use")) {
            messages.send(sender, "no-permission");
            return true;
        }

        Target target = resolveTarget(sender, args);
        if (target == null) {
            return true;
        }

        teleportService.teleport(target.player(), target.world());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            addEnabledWorlds(suggestions);
            if (sender.hasPermission("truertp.others")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }
            }
        } else if (args.length == 2 && sender.hasPermission("truertp.others")) {
            addEnabledWorlds(suggestions);
        }
        return suggestions;
    }

    private void addEnabledWorlds(List<String> suggestions) {
        for (World world : Bukkit.getWorlds()) {
            if (plugin.configService().worldSettings(world.getName()).enabled()) {
                suggestions.add(world.getName());
            }
        }
    }

    private Target resolveTarget(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "players-only");
                return null;
            }
            return new Target(player, null);
        }

        if (args.length == 1) {
            World world = Bukkit.getWorld(args[0]);
            if (world != null) {
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "players-only");
                    return null;
                }
                return new Target(player, world);
            }

            return resolveOtherPlayer(sender, args[0], null);
        }

        if (args.length == 2) {
            World world = Bukkit.getWorld(args[1]);
            if (world == null) {
                messages.send(sender, "world-not-found");
                return null;
            }
            return resolveOtherPlayer(sender, args[0], world);
        }

        messages.send(sender, "usage-rtp");
        return null;
    }

    private Target resolveOtherPlayer(CommandSender sender, String playerName, World world) {
        if (!sender.hasPermission("truertp.others")) {
            messages.send(sender, "no-permission");
            return null;
        }

        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            messages.send(sender, "player-not-found");
            return null;
        }
        return new Target(player, world);
    }

    private record Target(Player player, World world) {
    }
}
