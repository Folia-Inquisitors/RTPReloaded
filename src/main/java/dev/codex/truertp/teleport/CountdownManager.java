package dev.codex.truertp.teleport;

import dev.codex.truertp.TrueRtpPlugin;
import dev.codex.truertp.message.MessageService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class CountdownManager {
    private final TrueRtpPlugin plugin;
    private final MessageService messages;
    private final Map<UUID, ScheduledTask> activeTasks = new HashMap<>();

    public CountdownManager(TrueRtpPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void start(Player player, int seconds, boolean cancelOnMove, Runnable onComplete) {
        if (cancel(player.getUniqueId())) {
            messages.send(player, "already-counting");
        }

        if (seconds <= 0) {
            onComplete.run();
            return;
        }

        Location startingBlock = player.getLocation().getBlock().getLocation();
        final int[] remaining = {seconds};
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (!player.isOnline()) {
                CountdownManager.this.cancel(player.getUniqueId());
                return;
            }

            if (cancelOnMove && movedBlock(player, startingBlock)) {
                messages.send(player, "countdown-cancelled-move");
                CountdownManager.this.cancel(player.getUniqueId());
                return;
            }

            if (remaining[0] <= 0) {
                CountdownManager.this.cancel(player.getUniqueId());
                onComplete.run();
                return;
            }

            messages.send(player, "countdown", Map.of("seconds", Integer.toString(remaining[0])));
            remaining[0]--;
        }, null, 1L, 20L);

        activeTasks.put(player.getUniqueId(), task);
    }

    public void cancelAll() {
        for (ScheduledTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
    }

    private boolean cancel(UUID uuid) {
        ScheduledTask existing = activeTasks.remove(uuid);
        if (existing != null) {
            existing.cancel();
            return true;
        }
        return false;
    }

    private boolean movedBlock(Player player, Location startingBlock) {
        Location current = player.getLocation();
        return !current.getWorld().equals(startingBlock.getWorld())
                || current.getBlockX() != startingBlock.getBlockX()
                || current.getBlockY() != startingBlock.getBlockY()
                || current.getBlockZ() != startingBlock.getBlockZ();
    }
}
