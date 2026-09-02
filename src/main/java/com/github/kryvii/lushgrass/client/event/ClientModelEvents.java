package com.github.kryvii.lushgrass.client.event;

import com.github.kryvii.lushgrass.LushGrass;
import com.github.kryvii.lushgrass.client.model.ConfigurableGrassBlockModel;
import com.github.kryvii.lushgrass.client.model.GrassBlockTuftModel;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class ClientModelEvents {
    private static final Identifier FULL_GRASS_BLOCK_MODEL =
            Identifier.fromNamespaceAndPath(LushGrass.MOD_ID, "block/grass_block_full");
    private static final Identifier FULL_SNOWY_GRASS_BLOCK_MODEL =
            Identifier.fromNamespaceAndPath(LushGrass.MOD_ID, "block/grass_block_snow_full");
    private static final Identifier GRASS_TUFT_MODEL =
            Identifier.fromNamespaceAndPath(LushGrass.MOD_ID, "block/grass_tuft");
    private static final Identifier SHORTER_GRASS_TUFT_MODEL =
            Identifier.fromNamespaceAndPath(LushGrass.MOD_ID, "block/grass_tuft_shorter");
    private static final Identifier SHORTEST_GRASS_TUFT_MODEL =
            Identifier.fromNamespaceAndPath(LushGrass.MOD_ID, "block/grass_tuft_shortest");

    private static final ExtraModelKey<BlockStateModel> FULL_GRASS_BLOCK_KEY =
            ExtraModelKey.create(FULL_GRASS_BLOCK_MODEL::toString);
    private static final ExtraModelKey<BlockStateModel> FULL_SNOWY_GRASS_BLOCK_KEY =
            ExtraModelKey.create(FULL_SNOWY_GRASS_BLOCK_MODEL::toString);
    private static final ExtraModelKey<BlockStateModel> GRASS_TUFT_KEY =
            ExtraModelKey.create(GRASS_TUFT_MODEL::toString);
    private static final ExtraModelKey<BlockStateModel> SHORTER_GRASS_TUFT_KEY =
            ExtraModelKey.create(SHORTER_GRASS_TUFT_MODEL::toString);
    private static final ExtraModelKey<BlockStateModel> SHORTEST_GRASS_TUFT_KEY =
            ExtraModelKey.create(SHORTEST_GRASS_TUFT_MODEL::toString);

    public static void register() {
        ModelLoadingPlugin.register(context -> {
            context.addModel(
                    FULL_GRASS_BLOCK_KEY,
                    SimpleUnbakedExtraModel.blockStateModel(FULL_GRASS_BLOCK_MODEL)
            );
            context.addModel(
                    FULL_SNOWY_GRASS_BLOCK_KEY,
                    SimpleUnbakedExtraModel.blockStateModel(FULL_SNOWY_GRASS_BLOCK_MODEL)
            );
            context.addModel(GRASS_TUFT_KEY, SimpleUnbakedExtraModel.blockStateModel(GRASS_TUFT_MODEL));
            context.addModel(
                    SHORTER_GRASS_TUFT_KEY,
                    SimpleUnbakedExtraModel.blockStateModel(SHORTER_GRASS_TUFT_MODEL)
            );
            context.addModel(
                    SHORTEST_GRASS_TUFT_KEY,
                    SimpleUnbakedExtraModel.blockStateModel(SHORTEST_GRASS_TUFT_MODEL)
            );
            context.modifyBlockModelAfterBake()
                    .register(ModelModifier.WRAP_PHASE, ClientModelEvents::modifyBakedModel);
        });
    }

    private static BlockStateModel modifyBakedModel(
            BlockStateModel model,
            ModelModifier.AfterBakeBlock.Context context
    ) {
        if (!context.state().is(Blocks.GRASS_BLOCK)) {
            return model;
        }

        if (context.state().getValue(BlockStateProperties.SNOWY)) {
            return new ConfigurableGrassBlockModel(
                    bakeModel(context, FULL_SNOWY_GRASS_BLOCK_MODEL),
                    model
            );
        }

        return new GrassBlockTuftModel(
                bakeModel(context, FULL_GRASS_BLOCK_MODEL),
                model,
                bakeModel(context, GRASS_TUFT_MODEL),
                bakeModel(context, SHORTER_GRASS_TUFT_MODEL),
                bakeModel(context, SHORTEST_GRASS_TUFT_MODEL)
        );
    }

    private static BlockStateModel bakeModel(
            ModelModifier.AfterBakeBlock.Context context,
            Identifier identifier
    ) {
        return new SingleVariant(new Variant(identifier).bake(context.baker()));
    }

    private ClientModelEvents() {
    }
}
