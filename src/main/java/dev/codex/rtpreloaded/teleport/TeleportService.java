package dev.codex.rtpreloaded.teleport;

import dev.codex.rtpreloaded.RtpReloadedPlugin;
import dev.codex.rtpreloaded.config.ConfigService;
import dev.codex.rtpreloaded.config.PluginSettings;
import dev.codex.rtpreloaded.config.WorldSettings;
import dev.codex.rtpreloaded.message.MessageService;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class TeleportService {
    private final RtpReloadedPlugin plugin;
    private final ConfigService configService;
    private final MessageService messages;
    private final CountdownManager countdownManager;
    private final SafeLocationFinder locationFinder;

    public TeleportService(
            RtpReloadedPlugin plugin,
            ConfigService configService,
            MessageService messages,
            CountdownManager countdownManager
    ) {
        this.plugin = plugin;
        this.configService = configService;
        this.messages = messages;
        this.countdownManager = countdownManager;
        this.locationFinder = new SafeLocationFinder(plugin);
    }

    public void teleport(Player player, World requestedWorld) {
        teleport(player, requestedWorld, configService.settings().cancelOnMove(), true);
    }

    public void teleportFromPortal(Player player, World requestedWorld) {
        // Portal RTP is already triggered by walking into the region, so skip delay and movement cancellation.
        teleport(player, requestedWorld, false, false);
    }

    private void teleport(Player player, World requestedWorld, boolean cancelOnMove, boolean useCountdown) {
        PluginSettings settings = configService.settings();
        World world = chooseWorld(player, requestedWorld, settings);
        if (world == null) {
            messages.send(player, "world-not-found");
            return;
        }

        WorldSettings worldSettings = configService.worldSettings(world.getName());
        if (!worldSettings.enabled()) {
            messages.send(player, "world-disabled", Map.of("world", world.getName()));
            return;
        }

        if (useCountdown) {
            countdownManager.start(player, settings.delaySeconds(), cancelOnMove, () -> searchAndTeleport(player, world, worldSettings));
            return;
        }

        searchAndTeleport(player, world, worldSettings);
    }

    private void searchAndTeleport(Player player, World world, WorldSettings worldSettings) {
        PluginSettings settings = configService.settings();
        messages.send(player, "searching", Map.of("world", world.getName()));

        CompletableFuture<Location> locationFuture = locationFinder.find(
                world,
                worldSettings,
                settings.safety(),
                settings.attempts(),
                settings.chunkLoadTimeoutTicks()
        );

        locationFuture.whenComplete((location, throwable) -> runForPlayer(player, () -> {
            if (!player.isOnline()) {
                return;
            }

            if (throwable != null) {
                plugin.debug("Safe-location search failed", throwable);
                messages.send(player, "teleport-error");
                return;
            }

            if (location == null) {
                messages.send(player, "teleport-failed", Map.of("attempts", Integer.toString(settings.attempts())));
                return;
            }

            finishTeleport(player, location);
        }));
    }

    private void finishTeleport(Player player, Location location) {
        player.teleportAsync(location).whenComplete((success, throwable) -> runForPlayer(player, () -> {
            if (throwable != null || !Boolean.TRUE.equals(success)) {
                plugin.debug("Paper teleportAsync failed", throwable);
                messages.send(player, "teleport-error");
                return;
            }

            messages.send(player, "teleport-success", Map.of(
                    "x", Integer.toString(location.getBlockX()),
                    "y", Integer.toString(location.getBlockY()),
                    "z", Integer.toString(location.getBlockZ()),
                    "world", location.getWorld().getName()
            ));
        }));
    }

    private void runForPlayer(Player player, Runnable task) {
        boolean scheduled = player.getScheduler().execute(plugin, task, null, 1L);
        if (!scheduled) {
            plugin.debug("Could not schedule task for player " + player.getName() + "; entity is no longer available.");
        }
    }

    private World chooseWorld(Player player, World requestedWorld, PluginSettings settings) {
        if (requestedWorld != null) {
            return requestedWorld;
        }

        if (settings.useCurrentWorldFirst()) {
            World current = player.getWorld();
            if (configService.worldSettings(current.getName()).enabled()) {
                return current;
            }
        }

        return Bukkit.getWorld(settings.defaultWorld());
    }
}
