package dev.codex.rtpreloaded.teleport;

import dev.codex.rtpreloaded.RtpReloadedPlugin;
import dev.codex.rtpreloaded.config.SafetySettings;
import dev.codex.rtpreloaded.config.WorldSettings;
import dev.codex.rtpreloaded.random.RandomPoint;
import dev.codex.rtpreloaded.random.RandomPointSampler;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SafeLocationFinder {
    private final RtpReloadedPlugin plugin;
    private final RandomPointSampler sampler = new RandomPointSampler();

    public SafeLocationFinder(RtpReloadedPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Location> find(
            World world,
            WorldSettings settings,
            SafetySettings safety,
            int maxAttempts,
            long chunkTimeoutTicks
    ) {
        CompletableFuture<Location> result = new CompletableFuture<>();
        tryCandidate(world, settings, safety, maxAttempts, chunkTimeoutTicks, 1, new HashSet<>(), result);
        return result;
    }

    private void tryCandidate(
            World world,
            WorldSettings settings,
            SafetySettings safety,
            int maxAttempts,
            long chunkTimeoutTicks,
            int attempt,
            Set<RandomPoint> seen,
            CompletableFuture<Location> result
    ) {
        if (result.isDone()) {
            return;
        }

        if (attempt > maxAttempts) {
            result.complete(null);
            return;
        }

        RandomPoint point = nextUniquePoint(settings, seen);
        int chunkX = Math.floorDiv(point.x(), 16);
        int chunkZ = Math.floorDiv(point.z(), 16);

        plugin.debug("Attempt " + attempt + ": loading chunk " + chunkX + "," + chunkZ
                + " for candidate " + point.x() + "," + point.z());

        CompletableFuture<Chunk> chunkFuture = world.getChunkAtAsync(chunkX, chunkZ, true);
        chunkFuture.orTimeout(chunkTimeoutTicks * 50L, TimeUnit.MILLISECONDS)
                .whenComplete((chunk, throwable) -> plugin.getServer().getRegionScheduler().execute(plugin, world, chunkX, chunkZ, () -> {
                    if (throwable != null) {
                        plugin.debug("Chunk load failed for " + chunkX + "," + chunkZ, throwable);
                        tryCandidate(world, settings, safety, maxAttempts, chunkTimeoutTicks, attempt + 1, seen, result);
                        return;
                    }

                    Location safeLocation = evaluate(world, point, settings, safety);
                    if (safeLocation != null) {
                        result.complete(safeLocation);
                        return;
                    }

                    tryCandidate(world, settings, safety, maxAttempts, chunkTimeoutTicks, attempt + 1, seen, result);
                }));
    }

    private RandomPoint nextUniquePoint(WorldSettings settings, Set<RandomPoint> seen) {
        RandomPoint point;
        do {
            point = sampler.sample(settings.centerX(), settings.centerZ(), settings.minRadius(), settings.maxRadius());
        } while (!seen.add(point));
        return point;
    }

    private Location evaluate(World world, RandomPoint point, WorldSettings settings, SafetySettings safety) {
        int groundY = world.getHighestBlockYAt(point.x(), point.z(), HeightMap.MOTION_BLOCKING_NO_LEAVES);
        int feetY = groundY + 1;

        if (feetY < settings.minY() || feetY > settings.maxY()) {
            plugin.debug("Rejected " + point.x() + "," + point.z() + ": y " + feetY
                    + " outside " + settings.minY() + "-" + settings.maxY());
            return null;
        }

        Block ground = world.getBlockAt(point.x(), groundY, point.z());
        Block feet = world.getBlockAt(point.x(), feetY, point.z());
        Block head = world.getBlockAt(point.x(), feetY + 1, point.z());

        if (!isSafeGround(ground, safety)) {
            plugin.debug("Rejected " + point.x() + "," + point.z() + ": unsafe ground "
                    + ground.getType().name());
            return null;
        }

        if (!isPassable(feet, safety) || !isPassable(head, safety)) {
            plugin.debug("Rejected " + point.x() + "," + point.z() + ": blocked body space");
            return null;
        }

        return new Location(world, point.x() + 0.5D, feetY, point.z() + 0.5D, 0.0F, 0.0F);
    }

    private boolean isSafeGround(Block block, SafetySettings safety) {
        Material type = block.getType();
        if (!type.isSolid()) {
            return false;
        }
        if (safety.unsafeGround().contains(type)) {
            return false;
        }
        if (!safety.allowLeaves() && type.name().endsWith("_LEAVES")) {
            return false;
        }
        if (safety.avoidLiquids() && block.isLiquid()) {
            return false;
        }
        return !safety.avoidLava() || type != Material.LAVA;
    }

    private boolean isPassable(Block block, SafetySettings safety) {
        if (safety.avoidLiquids() && block.isLiquid()) {
            return false;
        }
        return block.isPassable();
    }
}
