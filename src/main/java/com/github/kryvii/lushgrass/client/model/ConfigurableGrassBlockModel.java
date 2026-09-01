package com.github.kryvii.lushgrass.client.model;

import com.github.kryvii.lushgrass.config.ClientConfig;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ConfigurableGrassBlockModel implements BlockStateModel, FabricBlockStateModel {
    private final BlockStateModel fullCoverageModel;
    private final BlockStateModel vanillaModel;

    public ConfigurableGrassBlockModel(BlockStateModel fullCoverageModel, BlockStateModel vanillaModel) {
        this.fullCoverageModel = fullCoverageModel;
        this.vanillaModel = vanillaModel;
    }

    protected final BlockStateModel activeModel() {
        return ClientConfig.fullGrassBlockCoverage() ? this.fullCoverageModel : this.vanillaModel;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        this.activeModel().collectParts(random, parts);
    }

    @Override
    public Material.Baked particleMaterial() {
        return this.activeModel().particleMaterial();
    }

    @Override
    public int materialFlags() {
        return this.fullCoverageModel.materialFlags() | this.vanillaModel.materialFlags();
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
    }

    @Override
    public @Nullable Object createGeometryKey(
            BlockAndTintGetter blockView,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        boolean fullCoverage = ClientConfig.fullGrassBlockCoverage();
        Object modelKey = fabricModel(fullCoverage ? this.fullCoverageModel : this.vanillaModel)
                .createGeometryKey(blockView, pos, state, random);
        return modelKey == null ? null : new GeometryKey(fullCoverage, modelKey);
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter blockView, BlockPos pos, BlockState state) {
        return fabricModel(this.activeModel()).particleMaterial(blockView, pos, state);
    }

    @Override
    public int materialFlags(
            BlockAndTintGetter blockView,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        return fabricModel(this.activeModel()).materialFlags(blockView, pos, state, random);
    }

    protected static FabricBlockStateModel fabricModel(BlockStateModel model) {
        return (FabricBlockStateModel) model;
    }

    private record GeometryKey(boolean fullCoverage, Object modelKey) {
    }
}
