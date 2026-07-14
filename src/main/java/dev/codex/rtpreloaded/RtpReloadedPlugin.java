package dev.codex.rtpreloaded;

import dev.codex.rtpreloaded.command.CommandAliasNormalizer;
import dev.codex.rtpreloaded.command.RtpCommand;
import dev.codex.rtpreloaded.command.RtpReloadedCommand;
import dev.codex.rtpreloaded.config.ConfigService;
import dev.codex.rtpreloaded.message.MessageService;
import dev.codex.rtpreloaded.portal.PortalListener;
import dev.codex.rtpreloaded.teleport.CountdownManager;
import dev.codex.rtpreloaded.teleport.TeleportService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class RtpReloadedPlugin extends JavaPlugin {
    private ConfigService configService;
    private MessageService messageService;
    private CountdownManager countdownManager;
    private TeleportService teleportService;

    @Override
    public void onEnable() {
        migrateLegacyDataFolder();
        saveDefaultConfig();

        this.messageService = new MessageService(this);
        this.configService = new ConfigService(this);
        this.countdownManager = new CountdownManager(this, messageService);
        this.teleportService = new TeleportService(this, configService, messageService, countdownManager);

        reloadPluginConfig();
        registerCommands();
        getServer().getPluginManager().registerEvents(new CommandAliasNormalizer(), this);
        getServer().getPluginManager().registerEvents(new PortalListener(this, teleportService, messageService), this);

        getLogger().info("RTPReloaded enabled.");
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


    private void migrateLegacyDataFolder() {
        File newFolder = getDataFolder();
        File pluginsFolder = newFolder.getParentFile();
        if (pluginsFolder == null) {
            return;
        }

        File legacyFolder = new File(pluginsFolder, "TrueRTP");
        if (!legacyFolder.isDirectory() || legacyFolder.equals(newFolder)) {
            return;
        }

        copyLegacyFile(legacyFolder, newFolder, "config.yml");
        copyLegacyFile(legacyFolder, newFolder, "messages.yml");
    }

    private void copyLegacyFile(File legacyFolder, File newFolder, String fileName) {
        File source = new File(legacyFolder, fileName);
        File destination = new File(newFolder, fileName);
        if (!source.isFile() || destination.exists()) {
            return;
        }

        try {
            Files.createDirectories(newFolder.toPath());
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            getLogger().info("Migrated legacy TrueRTP " + fileName + " to RTPReloaded.");
        } catch (IOException exception) {
            getLogger().log(Level.WARNING, "Could not migrate legacy TrueRTP " + fileName + ".", exception);
        }
    }

    private void registerCommands() {
        RtpCommand rtpCommand = new RtpCommand(this, teleportService, messageService);
        PluginCommand rtp = getCommand("rtp");
        if (rtp != null) {
            rtp.setExecutor(rtpCommand);
            rtp.setTabCompleter(rtpCommand);
        }

        RtpReloadedCommand adminCommand = new RtpReloadedCommand(this, messageService);
        PluginCommand trueRtp = getCommand("rtpreloaded");
        if (trueRtp != null) {
            trueRtp.setExecutor(adminCommand);
            trueRtp.setTabCompleter(adminCommand);
        }
    }
}




