package dev.codex.truertp.message;

import dev.codex.truertp.TrueRtpPlugin;
import java.io.File;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandSender;

public final class MessageService {
    private final TrueRtpPlugin plugin;
    private FileConfiguration messages;

    public MessageService(TrueRtpPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(format(key, placeholders));
    }

    public String format(String key, Map<String, String> placeholders) {
        String raw = messages.getString(key, key);
        String prefix = messages.getString("prefix", "");
        String formatted = raw.replace("{prefix}", prefix);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return ChatColor.translateAlternateColorCodes('&', formatted);
    }
}
