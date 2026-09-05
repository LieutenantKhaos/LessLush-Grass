package com.github.kryvii.lushgrass.client.model;

import com.github.kryvii.lushgrass.client.world.DevelopedAreaDetector;
import com.github.kryvii.lushgrass.config.ClientConfig;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class GrassBlockTuftModel extends ConfigurableGrassBlockModel {
    public static final int TUFT_MATERIAL_TAG = 0x4C47;

    private static final int TUFT_OFFSET_SEED_X = 17;
    private static final int TUFT_OFFSET_SEED_Z = 31;
    private static final float TUFT_HORIZONTAL_SCALE = 1.0F;
    private static final float TUFT_VERTICAL_SCALE = 1.0F;
    private static final BlockState SHORT_GRASS_STATE = Blocks.SHORT_GRASS.defaultBlockState();

    private final BlockStateModel tuftModel;
    private final BlockStateModel shorterTuftModel;
    private final BlockStateModel shortestTuftModel;

    public GrassBlockTuftModel(
            BlockStateModel fullCoverageModel,
            BlockStateModel vanillaModel,
            BlockStateModel tuftModel,
            BlockStateModel shorterTuftModel,
            BlockStateModel shortestTuftModel
    ) {
        super(fullCoverageModel, vanillaModel);
        this.tuftModel = tuftModel;
        this.shorterTuftModel = shorterTuftModel;
        this.shortestTuftModel = shortestTuftModel;
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter blockView,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest
    ) {
    fabricModel(this.activeModel()).emitQuads(emitter, blockView, pos, state, random, cullTest);

        // Fabric uses BlockAndTintGetter.EMPTY when rebuilding model geometry
        // for the block-breaking overlay. Keep decorative tufts out of that pass.
        if (blockView == BlockAndTintGetter.EMPTY || !shouldRenderTuft(blockView, state, pos)) {
       return;
    }

        BlockPos tuftPos = pos.above();
        Vec3 rawOffset = SHORT_GRASS_STATE.getOffset(
                tuftPos.offset(TUFT_OFFSET_SEED_X, 0, TUFT_OFFSET_SEED_Z)
        );
        final Vec3 offset = new Vec3(rawOffset.x, 0.0D, rawOffset.z);
        int packedLight = LightCoordsUtil.getLightCoords(blockView, tuftPos);
        int rotation = tuftRotation(pos);

        int variant = tuftVariant(pos);
        BlockStateModel selectedTuftModel = switch (variant) {
            case 1 -> this.shorterTuftModel;
            case 2 -> this.shortestTuftModel;
            default -> this.tuftModel;
        };

        emitter.pushTransform(quad -> transformTuftQuad(quad, offset, packedLight, rotation));
        try {
            random.setSeed(42L);
            selectedTuftModel.emitQuads(emitter, blockView, tuftPos, SHORT_GRASS_STATE, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    @Override
    public @Nullable Object createGeometryKey(
            BlockAndTintGetter blockView,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        Object baseKey = super.createGeometryKey(blockView, pos, state, random);
        if (baseKey == null) {
            return null;
        }
        if (!shouldRenderTuft(blockView, state, pos)) {
            return new GeometryKey(baseKey, false, Vec3.ZERO, 0, 0, 0);
        }

        BlockPos tuftPos = pos.above();
        Vec3 offset = SHORT_GRASS_STATE.getOffset(
                tuftPos.offset(TUFT_OFFSET_SEED_X, 0, TUFT_OFFSET_SEED_Z)
        );
        offset = new Vec3(offset.x, 0.0D, offset.z);
        int packedLight = LightCoordsUtil.getLightCoords(blockView, tuftPos);
        return new GeometryKey(baseKey, true, offset, packedLight, tuftRotation(pos), tuftVariant(pos));
    }

    @Override
    public int materialFlags() {
        return super.materialFlags()
                | this.tuftModel.materialFlags()
                | this.shorterTuftModel.materialFlags()
                | this.shortestTuftModel.materialFlags();
    }

    @Override
    public int materialFlags(
            BlockAndTintGetter blockView,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        int flags = super.materialFlags(blockView, pos, state, random);
        return shouldRenderTuft(blockView, state, pos)
                ? flags
                | this.tuftModel.materialFlags()
                | this.shorterTuftModel.materialFlags()
                | this.shortestTuftModel.materialFlags()
                : flags;
    }

    private static boolean shouldRenderTuft(BlockAndTintGetter blockView, BlockState state, BlockPos pos) {
        if (!state.is(Blocks.GRASS_BLOCK)
                || state.getValue(BlockStateProperties.SNOWY)
                || DevelopedAreaDetector.isNearDevelopedArea(blockView, pos)
                || !hasNaturalTuft(pos)) {
            return false;
        }

        BlockPos tuftPos = pos.above();
        BlockState aboveState = blockView.getBlockState(tuftPos);
        return !Block.isShapeFullBlock(aboveState.getCollisionShape(blockView, tuftPos));
    }

    /**
     * Gives the decorative tufts a natural, patchy distribution. The result is
     * deterministic from the block position, so the pattern does not flicker
     * when chunks rebuild. Broad noise creates grassy/non-grassy patches, while
     * a position hash makes the edge of those patches irregular.
     */
    private static boolean hasNaturalTuft(BlockPos pos) {
        double broad = tuftValueNoise(pos.getX() * 0.16D, pos.getZ() * 0.16D);
        double medium = tuftValueNoise(pos.getX() * 0.34D + 91.0D, pos.getZ() * 0.34D - 47.0D);
        double patchNoise = broad * 0.70D + medium * 0.30D;

        // Most wilderness remains lush, but some patches are deliberately open.
        // The threshold is modulated by the noise, producing connected patches
        // instead of simply removing every Nth grass block.
        double density = 0.42D + patchNoise * 0.48D;
        return tuftHash(pos.getX(), pos.getZ()) < density;
    }

    private static double tuftValueNoise(double x, double z) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double tx = smoothStep(x - x0);
        double tz = smoothStep(z - z0);

        double n00 = tuftLatticeNoise(x0, z0);
        double n10 = tuftLatticeNoise(x0 + 1, z0);
        double n01 = tuftLatticeNoise(x0, z0 + 1);
        double n11 = tuftLatticeNoise(x0 + 1, z0 + 1);

        double nx0 = lerp(n00, n10, tx);
        double nx1 = lerp(n01, n11, tx);
        return lerp(nx0, nx1, tz);
    }

    private static double tuftLatticeNoise(int x, int z) {
        long hash = 0x9E3779B97F4A7C15L;
        hash ^= (long) x * 0xBF58476D1CE4E5B9L;
        hash ^= (long) z * 0x94D049BB133111EBL;
        hash = (hash ^ (hash >>> 30)) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ (hash >>> 27)) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return (hash >>> 11) * 0x1.0p-53;
    }

    private static double tuftHash(int x, int z) {
        long hash = 0xD1B54A32D192ED03L;
        hash ^= (long) x * 0x9E3779B97F4A7C15L;
        hash ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        hash ^= hash >>> 33;
        hash *= 0xFF51AFD7ED558CCDL;
        hash ^= hash >>> 33;
        hash *= 0xC4CEB9FE1A85EC53L;
        hash ^= hash >>> 33;
        return (hash >>> 11) * 0x1.0p-53;
    }

    private static double smoothStep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int tuftRotation(BlockPos pos) {
        return (int) (tuftHash(
                pos.getX() + 137,
                pos.getZ() - 211
        ) * 360.0D);
    }

    private static int tuftVariant(BlockPos pos) {
        double value = tuftHash(
                pos.getX() - 503,
                pos.getZ() + 719
        );

        return switch (ClientConfig.tuftVariation()) {
            case 0 -> {
                // Fast: 75% tall, 25% medium, 0% shortest.
                yield value < 0.75D ? 0 : 1;
            }

            case 2 -> {
                // Fancy: 50% tall, 35% medium, 15% shortest.
                if (value < 0.50D) {
                    yield 0;
                } else if (value < 0.85D) {
                    yield 1;
                } else {
                    yield 2;
                }
            }

            default -> {
                // Normal: 60% tall, 30% medium, 10% shortest.
                if (value < 0.60D) {
                    yield 0;
                } else if (value < 0.90D) {
                    yield 1;
                } else {
                    yield 2;
                }
            }
        };
    }

    private static boolean transformTuftQuad(
            MutableQuadView quad,
            Vec3 offset,
            int packedLight,
            int rotationDegrees
    ) {
        double radians = Math.toRadians(rotationDegrees);
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);

        for (int vertex = 0; vertex < 4; vertex++) {
            float x = (quad.x(vertex) - 0.5F) * TUFT_HORIZONTAL_SCALE;
            float z = (quad.z(vertex) - 0.5F) * TUFT_HORIZONTAL_SCALE;

            float rotatedX = x * cos - z * sin;
            float rotatedZ = x * sin + z * cos;

            quad.pos(
                    vertex,
                    rotatedX + 0.5F + (float) offset.x,
                    quad.y(vertex) * TUFT_VERTICAL_SCALE + (float) (1.0D + offset.y),
                    rotatedZ + 0.5F + (float) offset.z
            );
            quad.lightmap(vertex, packedLight);
        }

        quad.tintIndex(0);
        quad.tag(TUFT_MATERIAL_TAG);
        return true;
    }

    private record GeometryKey(
            Object baseKey,
            boolean tuft,
            Vec3 offset,
            int packedLight,
            int rotationDegrees,
            int variant
    ) {
    }
}