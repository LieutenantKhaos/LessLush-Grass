package com.github.kryvii.lushgrass.client.world;

import com.github.kryvii.lushgrass.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects settled/developed-looking areas without changing world blocks.
 *
 * Development is scored by the strength and proximity of nearby indicators.
 * Strong indicators can suppress tufts on their own, while medium and weak
 * indicators need to build up into a cluster before they have a larger effect.
 *
 * The renderer can ask this class several times for the same grass block.
 * Results are therefore cached briefly to avoid repeatedly scanning hundreds
 * of surrounding blocks during chunk/model compilation.
 */
public final class DevelopedAreaDetector {
    private static final long CACHE_LIFETIME_NANOS = 750_000_000L;
    private static final int MAX_CACHE_ENTRIES = 65_536;

    /*
     * The score needed before an area is considered developed.
     *
     * A strong block is worth 3 points, so an important player-use block can
     * still suppress nearby grass by itself. Two medium indicators can also
     * establish a developed area, which is useful for things like a torch and
     * fence together.
     */
    private static final double DEVELOPED_SCORE_THRESHOLD = 3.0D;

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

        // Flowing water is an immediate-use environmental condition, so check it
        // before the developed-area cache. This also keeps water suppression
        // independent of the configurable developed-area radius.
        if (hasFlowingFluidTouching(world, origin)) {
            return true;
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

    /**
     * Checks the six blocks directly touching the grass block for flowing water
     * or flowing lava.
     *
     * Source water and source lava are ignored beside the grass, but a source
     * directly above the grass suppresses only the tuft underneath it. A
     * flowing fluid suppresses a tuft whenever it directly touches the grass.
     */
    private static boolean hasFlowingFluidTouching(BlockAndTintGetter world, BlockPos origin) {
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();

        // A source directly above the grass hides only the grass underneath it.
        check.set(origin.getX(), origin.getY() + 1, origin.getZ());
        BlockState above = world.getBlockState(check);
        if (isFluid(above)) {
            return true;
        }

        // Flowing fluid directly below the grass.
        check.set(origin.getX(), origin.getY() - 1, origin.getZ());
        if (isFlowingFluid(world.getBlockState(check))) {
            return true;
        }

        // Four horizontal neighbors. Only flowing fluid counts here.
        check.set(origin.getX() + 1, origin.getY(), origin.getZ());
        if (isFlowingFluid(world.getBlockState(check))) {
            return true;
        }

        check.set(origin.getX() - 1, origin.getY(), origin.getZ());
        if (isFlowingFluid(world.getBlockState(check))) {
            return true;
        }

        check.set(origin.getX(), origin.getY(), origin.getZ() + 1);
        if (isFlowingFluid(world.getBlockState(check))) {
            return true;
        }

        check.set(origin.getX(), origin.getY(), origin.getZ() - 1);
        return isFlowingFluid(world.getBlockState(check));
    }

    private static boolean isFluid(BlockState state) {
        return state.is(Blocks.WATER) || state.is(Blocks.LAVA);
    }

    private static boolean isFlowingFluid(BlockState state) {
        return isFluid(state) && state.getValue(BlockStateProperties.LEVEL) > 0;
    }

    /**
     * Scores developed indicators around the grass block.
     *
     * Distance matters: indicators close to the grass have their full value,
     * while indicators near the edge of the configured radius have less
     * influence. This means a dense cluster can influence a wider area without
     * making every isolated block create a full suppression circle.
     */
    private static boolean calculate(BlockAndTintGetter world, BlockPos origin, int radius) {
        int scanRadius = radius;
        double score = 0.0D;

        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();

        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                int horizontalSquared = dx * dx + dz * dz;
                if (horizontalSquared > scanRadius * scanRadius) {
                    continue;
                }

                double distance = Math.sqrt(horizontalSquared);
                double distanceFactor = developmentDistanceFactor(distance, radius);

                // Ignore blocks whose influence has fallen to zero.
                if (distanceFactor <= 0.0D) {
                    continue;
                }

                for (int dy = -1; dy <= 2; dy++) {
                    check.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);

                    double strength = developedStrength(world.getBlockState(check));
                    if (strength <= 0.0D) {
                        continue;
                    }

                    score += strength * distanceFactor;

                    // A strong indicator immediately beside the grass should not
                    // be made weaker just because other indicators are absent.
                    if (horizontalSquared == 0 && strength >= 3.0D) {
                        return true;
                    }
                }
            }
        }

        if (score < DEVELOPED_SCORE_THRESHOLD) {
            return false;
        }

        /*
         * Keep the existing natural, irregular boundary. Noise is deliberately
         * applied only after the development score has established that this is
         * actually a developed area.
         */
        double noise = developmentNoise(origin.getX(), origin.getZ());
        double boundaryBias = 0.88D + noise * 0.24D;
        return score >= DEVELOPED_SCORE_THRESHOLD * boundaryBias;
    }

    private static double developmentDistanceFactor(double distance, int radius) {
        if (radius <= 0 || distance > radius) {
            return 0.0D;
        }

        /*
         * Full strength through the inner half of the radius, then a smooth
         * falloff toward the configured edge.
         */
        double innerRadius = Math.max(1.0D, radius * 0.5D);
        if (distance <= innerRadius) {
            return 1.0D;
        }

        double outerProgress = (distance - innerRadius) / Math.max(1.0D, radius - innerRadius);
        double falloff = 1.0D - outerProgress;
        return falloff * falloff;
    }

    /**
     * Strong indicators represent blocks that very clearly imply active player
     * use or a settled structure.
     */
    private static final Set<Block> STRONG_DEVELOPED_BLOCKS = Set.of(
            Blocks.DIRT_PATH,
            Blocks.FARMLAND,
            Blocks.HAY_BLOCK,

            Blocks.CRAFTING_TABLE,
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.BARREL,
            Blocks.FURNACE,
            Blocks.BLAST_FURNACE,
            Blocks.SMOKER,
            Blocks.LOOM,
            Blocks.CARTOGRAPHY_TABLE,
            Blocks.FLETCHING_TABLE,
            Blocks.COMPOSTER,
            Blocks.STONECUTTER,
            Blocks.GRINDSTONE,
            Blocks.SMITHING_TABLE,
            Blocks.LECTERN,
            Blocks.BREWING_STAND,
            Blocks.ENCHANTING_TABLE,

            Blocks.ANVIL,
            Blocks.CHIPPED_ANVIL,
            Blocks.DAMAGED_ANVIL,
            Blocks.BELL,

            Blocks.BEEHIVE,

            Blocks.CAMPFIRE,
            Blocks.SOUL_CAMPFIRE,

            Blocks.OAK_DOOR,
            Blocks.SPRUCE_DOOR,
            Blocks.BIRCH_DOOR,
            Blocks.JUNGLE_DOOR,
            Blocks.ACACIA_DOOR,
            Blocks.DARK_OAK_DOOR,
            Blocks.MANGROVE_DOOR,
            Blocks.CHERRY_DOOR,
            Blocks.BAMBOO_DOOR
    );

    /**
     * Medium indicators are useful evidence of development, but are common
     * enough that one isolated block should not clear a large area.
     *
     * Torches deliberately live here: they are player-placed and meaningful,
     * but a lone torch can also be temporary or exploratory.
     */
    private static final Set<Block> MEDIUM_DEVELOPED_BLOCKS = Set.of(
            Blocks.TORCH,
            Blocks.WALL_TORCH,

            Blocks.CAULDRON,
            Blocks.LADDER,
            Blocks.IRON_BARS,
            Blocks.GLASS_PANE,

            Blocks.OAK_TRAPDOOR,
            Blocks.SPRUCE_TRAPDOOR,
            Blocks.BIRCH_TRAPDOOR,
            Blocks.JUNGLE_TRAPDOOR,
            Blocks.ACACIA_TRAPDOOR,
            Blocks.DARK_OAK_TRAPDOOR,
            Blocks.MANGROVE_TRAPDOOR,
            Blocks.CHERRY_TRAPDOOR,
            Blocks.BAMBOO_TRAPDOOR,

            Blocks.OAK_FENCE_GATE,
            Blocks.SPRUCE_FENCE_GATE,
            Blocks.BIRCH_FENCE_GATE,
            Blocks.JUNGLE_FENCE_GATE,
            Blocks.ACACIA_FENCE_GATE,
            Blocks.DARK_OAK_FENCE_GATE,
            Blocks.MANGROVE_FENCE_GATE,
            Blocks.CHERRY_FENCE_GATE,
            Blocks.BAMBOO_FENCE_GATE,

            Blocks.CAMPFIRE,
            Blocks.SOUL_CAMPFIRE,
            Blocks.LANTERN,
            Blocks.SOUL_LANTERN,

            Blocks.OAK_FENCE,
            Blocks.SPRUCE_FENCE,
            Blocks.BIRCH_FENCE,
            Blocks.JUNGLE_FENCE,
            Blocks.ACACIA_FENCE,
            Blocks.DARK_OAK_FENCE,
            Blocks.MANGROVE_FENCE,
            Blocks.CHERRY_FENCE,
            Blocks.BAMBOO_FENCE
    );

    /**
     * Weak indicators are intentionally limited in the first 1.4.0 pass.
     * They provide supporting evidence but cannot establish a developed area
     * on their own.
     */
    private static final Set<Block> WEAK_DEVELOPED_BLOCKS = Set.of();

    private static double developedStrength(BlockState state) {
        if (state.is(BlockTags.BEDS)) {
            return 3.0D;
        }
        if (STRONG_DEVELOPED_BLOCKS.contains(state.getBlock())) {
            return 3.0D;
        }
        if (MEDIUM_DEVELOPED_BLOCKS.contains(state.getBlock())) {
            return 2.0D;
        }
        if (WEAK_DEVELOPED_BLOCKS.contains(state.getBlock())) {
            return 1.0D;
        }
        return 0.0D;
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

    private record CacheEntry(boolean developed, long expiresAtNanos) {
    }
}
