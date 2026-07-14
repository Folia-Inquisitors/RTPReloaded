package dev.codex.truertp.random;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class RandomPointSampler {
    private static final double FULL_CIRCLE = Math.PI * 2.0D;

    private final RandomGenerator random;

    public RandomPointSampler() {
        this(new SecureRandom());
    }

    RandomPointSampler(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public RandomPoint sample(int centerX, int centerZ, int minRadius, int maxRadius) {
        int safeMin = Math.max(0, minRadius);
        int safeMax = Math.max(safeMin, maxRadius);

        double minSquared = (double) safeMin * safeMin;
        double maxSquared = (double) safeMax * safeMax;

        // sqrt keeps the distribution uniform by area across the configured ring.
        double radius = Math.sqrt(minSquared + random.nextDouble() * (maxSquared - minSquared));
        double angle = random.nextDouble() * FULL_CIRCLE;

        int x = centerX + (int) Math.round(Math.cos(angle) * radius);
        int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
        return new RandomPoint(x, z);
    }
}
