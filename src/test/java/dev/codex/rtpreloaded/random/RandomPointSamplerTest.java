package dev.codex.rtpreloaded.random;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

final class RandomPointSamplerTest {
    @Test
    void sampledPointsStayWithinConfiguredRadius() {
        RandomPointSampler sampler = new RandomPointSampler(RandomGenerator.of("L64X128MixRandom"));

        for (int i = 0; i < 10_000; i++) {
            RandomPoint point = sampler.sample(100, -100, 250, 750);
            double distance = Math.hypot(point.x() - 100, point.z() + 100);

            assertTrue(distance >= 249.0D, "point should respect min radius");
            assertTrue(distance <= 751.0D, "point should respect max radius");
        }
    }
}
