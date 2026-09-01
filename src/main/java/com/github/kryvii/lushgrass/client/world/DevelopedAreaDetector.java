package com.github.kryvii.lushgrass.client.world;

import com.github.kryvii.lushgrass.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects settled/developed-looking areas without changing world blocks.
 *
 * The renderer can ask this class several times for the same grass block.
 * Results are therefore cached briefly to avoid repeatedly scanning hundreds
 * of surrounding blocks during chunk/model compilation.
 */

    /*
     * Armor stands are entities rather than blocks. They are intentionally not
     * queried from this block-rendering path because model baking/chunk rebuilds
     * can run off the client thread. They can be added safely later through a
     * render-thread-maintained entity-position cache.
     */

public final class DevelopedAreaDetector {
    private static final long CACHE_LIFETIME_NANOS = 750_000_000L;
    private static final int MAX_CACHE_ENTRIES = 65_536;

    private static final ConcurrentHashMap<Long, CacheEntry> RESULT_CACHE = new ConcurrentHashMap<>();
    private static volatile BlockAndTintGetter cachedWorld;
    private static volatile int cachedRadius = -1;

    private DevelopedAreaDetector() {
    }

    public static boolean isNearDevelopedArea(BlockAndTintGetter world, BlockPos origin) {
        // Never do a world scan while the client is shutting down.
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.isRunning()) {
            return false;
        }

        int radius = ClientConfig.developedAreaRadius();
        if (!ClientConfig.suppressTuftsInDevelopedAreas() || radius <= 0) {
            return false;
        }

        ensureCacheForWorld(world, radius);

        long key = origin.asLong();
        long now = System.nanoTime();
        CacheEntry cached = RESULT_CACHE.get(key);
        if (cached != null && cached.expiresAtNanos > now) {
            return cached.developed;
        }

        boolean developed = calculate(world, origin, radius);
        RESULT_CACHE.put(key, new CacheEntry(developed, now + CACHE_LIFETIME_NANOS));

        // Keep the cache bounded. A new cache is preferable to unbounded memory
        // growth during long sessions with lots of explored terrain.
        if (RESULT_CACHE.size() > MAX_CACHE_ENTRIES) {
            RESULT_CACHE.clear();
        }

        return developed;
    }

    private static void ensureCacheForWorld(BlockAndTintGetter world, int radius) {
        if (cachedWorld == world && cachedRadius == radius) {
            return;
        }

        synchronized (DevelopedAreaDetector.class) {
            if (cachedWorld != world || cachedRadius != radius) {
                RESULT_CACHE.clear();
                cachedWorld = world;
                cachedRadius = radius;
            }
        }
    }

    private static boolean calculate(BlockAndTintGetter world, BlockPos origin, int radius) {
        int scanRadius = radius + 2;
        double nearestDistance = findNearestDevelopedDistance(world, origin, scanRadius);
        if (nearestDistance > radius + 1.5D) {
            return false;
        }

        double solidRadius = Math.max(0.0D, radius - 3.0D);
        if (nearestDistance <= solidRadius) {
            return true;
        }

        double noise = developmentNoise(origin.getX(), origin.getZ());
        double noisyBoundary = (radius - 2.0D) + (noise * 4.0D);
        return nearestDistance <= noisyBoundary;
    }

    private static double findNearestDevelopedDistance(
            BlockAndTintGetter world,
            BlockPos origin,
            int scanRadius
    ) {
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();
        double nearestSquared = Double.POSITIVE_INFINITY;

        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                int horizontalSquared = dx * dx + dz * dz;
                if (horizontalSquared > scanRadius * scanRadius || horizontalSquared >= nearestSquared) {
                    continue;
                }

                if (!isDevelopedAround(world, origin, check, dx, dz)) {
                    continue;
                }

                nearestSquared = horizontalSquared;
            }
        }

        return nearestSquared == Double.POSITIVE_INFINITY
                ? Double.POSITIVE_INFINITY
                : Math.sqrt(nearestSquared);
    }

    private static boolean isDevelopedAround(
            BlockAndTintGetter world,
            BlockPos origin,
            BlockPos.MutableBlockPos check,
            int dx,
            int dz
    ) {
        for (int dy = -1; dy <= 2; dy++) {
            check.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
            if (isDevelopedBlock(world.getBlockState(check))) {
                return true;
            }
        }
        return false;
    }

    private static double developmentNoise(int blockX, int blockZ) {
        double broad = valueNoise(blockX * 0.085D, blockZ * 0.085D);
        double medium = valueNoise(blockX * 0.21D + 37.0D, blockZ * 0.21D - 19.0D);
        double detail = valueNoise(blockX * 0.62D - 71.0D, blockZ * 0.62D + 53.0D);
        return broad * 0.38D + medium * 0.37D + detail * 0.25D;
    }

    private static double valueNoise(double x, double z) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double tx = smooth(x - x0);
        double tz = smooth(z - z0);

        double n00 = latticeNoise(x0, z0);
        double n10 = latticeNoise(x0 + 1, z0);
        double n01 = latticeNoise(x0, z0 + 1);
        double n11 = latticeNoise(x0 + 1, z0 + 1);

        double nx0 = lerp(n00, n10, tx);
        double nx1 = lerp(n01, n11, tx);
        return lerp(nx0, nx1, tz);
    }

    private static double latticeNoise(int x, int z) {
        long hash = 0x9E3779B97F4A7C15L;
        hash ^= (long) x * 0xBF58476D1CE4E5B9L;
        hash ^= (long) z * 0x94D049BB133111EBL;
        hash = (hash ^ (hash >>> 30)) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ (hash >>> 27)) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return (hash >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Active-use blocks. Generic building materials are deliberately excluded
     * so abandoned structures can remain naturally overgrown.
     */
    private static final Set<net.minecraft.world.level.block.Block> DEVELOPED_BLOCKS = Set.of(
            Blocks.DIRT_PATH, Blocks.FARMLAND, Blocks.HAY_BLOCK,

            Blocks.CRAFTING_TABLE, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL,
            Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
            Blocks.LOOM, Blocks.CARTOGRAPHY_TABLE, Blocks.FLETCHING_TABLE,
            Blocks.COMPOSTER, Blocks.STONECUTTER, Blocks.GRINDSTONE,
            Blocks.SMITHING_TABLE, Blocks.LECTERN, Blocks.BREWING_STAND,
            Blocks.ENCHANTING_TABLE, Blocks.ANVIL, Blocks.CHIPPED_ANVIL,
            Blocks.DAMAGED_ANVIL, Blocks.BELL,

            Blocks.BEEHIVE,

            // Strong signs of active player use.
            Blocks.CAULDRON, Blocks.LADDER, Blocks.IRON_BARS,

            // Player-built infrastructure and decoration.
            Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.BIRCH_TRAPDOOR,
            Blocks.JUNGLE_TRAPDOOR, Blocks.ACACIA_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR,
            Blocks.MANGROVE_TRAPDOOR, Blocks.CHERRY_TRAPDOOR, Blocks.BAMBOO_TRAPDOOR,
            Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE, Blocks.BIRCH_FENCE_GATE,
            Blocks.JUNGLE_FENCE_GATE, Blocks.ACACIA_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE,
            Blocks.MANGROVE_FENCE_GATE, Blocks.CHERRY_FENCE_GATE, Blocks.BAMBOO_FENCE_GATE,
            Blocks.GLASS_PANE,

            Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.LANTERN, Blocks.SOUL_LANTERN,

            Blocks.OAK_FENCE, Blocks.SPRUCE_FENCE, Blocks.BIRCH_FENCE,
            Blocks.JUNGLE_FENCE, Blocks.ACACIA_FENCE, Blocks.DARK_OAK_FENCE,
            Blocks.MANGROVE_FENCE, Blocks.CHERRY_FENCE, Blocks.BAMBOO_FENCE,
            Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR,
            Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR, Blocks.MANGROVE_DOOR,
            Blocks.CHERRY_DOOR, Blocks.BAMBOO_DOOR
    );

    private static boolean isDevelopedBlock(BlockState state) {
        return state.is(BlockTags.BEDS) || DEVELOPED_BLOCKS.contains(state.getBlock());
    }

    private record CacheEntry(boolean developed, long expiresAtNanos) {
    }
}
