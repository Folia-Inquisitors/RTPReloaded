package dev.codex.truertp;

import dev.codex.truertp.command.RtpCommand;
import dev.codex.truertp.command.TrueRtpCommand;
import dev.codex.truertp.config.ConfigService;
import dev.codex.truertp.message.MessageService;
import dev.codex.truertp.portal.PortalListener;
import dev.codex.truertp.teleport.CountdownManager;
import dev.codex.truertp.teleport.TeleportService;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class TrueRtpPlugin extends JavaPlugin {
    private ConfigService configService;
    private MessageService messageService;
    private CountdownManager countdownManager;
    private TeleportService teleportService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messageService = new MessageService(this);
        this.configService = new ConfigService(this);
        this.countdownManager = new CountdownManager(this, messageService);
        this.teleportService = new TeleportService(this, configService, messageService, countdownManager);

        reloadPluginConfig();
        registerCommands();
        getServer().getPluginManager().registerEvents(new PortalListener(this, teleportService, messageService), this);

        getLogger().info("TrueRTP enabled.");
    }

    @Override
    public void onDisable() {
        if (countdownManager != null) {
            countdownManager.cancelAll();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        configService.reload();
        messageService.reload();
    }

    public void debug(String message) {
        if (configService != null && configService.settings().debug()) {
            getLogger().info("[Debug] " + message);
        }
    }

    public void debug(String message, Throwable throwable) {
        if (configService != null && configService.settings().debug()) {
            getLogger().log(Level.WARNING, "[Debug] " + message, throwable);
        }
    }

    public ConfigService configService() {
        return configService;
    }

    public MessageService messageService() {
        return messageService;
    }

    private void registerCommands() {
        RtpCommand rtpCommand = new RtpCommand(this, teleportService, messageService);
        PluginCommand rtp = getCommand("rtp");
        if (rtp != null) {
            rtp.setExecutor(rtpCommand);
            rtp.setTabCompleter(rtpCommand);
        }

        TrueRtpCommand adminCommand = new TrueRtpCommand(this, messageService);
        PluginCommand trueRtp = getCommand("truertp");
        if (trueRtp != null) {
            trueRtp.setExecutor(adminCommand);
            trueRtp.setTabCompleter(adminCommand);
        }
    }
}




