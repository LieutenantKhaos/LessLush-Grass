package com.github.kryvii.lushgrass.client.mixin.sodium;

import com.github.kryvii.lushgrass.LushGrass;
import com.github.kryvii.lushgrass.client.compat.iris.IrisMaterialBridge;
import com.github.kryvii.lushgrass.client.compat.sodium.SodiumQuadViewAccess;
import com.github.kryvii.lushgrass.client.model.GrassBlockTuftModel;
import java.util.concurrent.atomic.AtomicBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class SodiumBlockRendererMixin {
    @Unique
    private static final AtomicBoolean lushGrass$enabledLogged = new AtomicBoolean();

    @Unique
    private static final AtomicBoolean lushGrass$failureLogged = new AtomicBoolean();

    @Unique
    private static volatile IrisMaterialBridge lushGrass$materialBridge;

    @Unique
    private boolean lushGrass$tuftMaterialActive;

    @Inject(method = "processQuad", at = @At("HEAD"))
    private void lushGrass$beginTuftMaterial(@Coerce Object quad, CallbackInfo callback) {
        if (this.lushGrass$tuftMaterialActive
                || !(quad instanceof SodiumQuadViewAccess access)
                || access.lushGrass$getTag() != GrassBlockTuftModel.TUFT_MATERIAL_TAG) {
            return;
        }

        try {
            IrisMaterialBridge bridge = lushGrass$getMaterialBridge(this.getClass());
            if (!bridge.overrideShortGrass(this)) {
                return;
            }
            this.lushGrass$tuftMaterialActive = true;
            if (lushGrass$enabledLogged.compareAndSet(false, true)) {
                LushGrass.LOGGER.info("Enabled Iris short-grass materials for Lush Grass tufts.");
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            lushGrass$logFailure(exception);
        }
    }

    @Inject(method = "renderModel", at = @At("RETURN"))
    private void lushGrass$endTuftMaterial(CallbackInfo callback) {
        if (!this.lushGrass$tuftMaterialActive) {
            return;
        }

        try {
            lushGrass$getMaterialBridge(this.getClass()).restore(this);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            lushGrass$logFailure(exception);
        } finally {
            this.lushGrass$tuftMaterialActive = false;
        }
    }

    @Unique
    private static IrisMaterialBridge lushGrass$getMaterialBridge(Class<?> rendererClass)
            throws ReflectiveOperationException {
        IrisMaterialBridge bridge = lushGrass$materialBridge;
        if (bridge != null && bridge.supports(rendererClass)) {
            return bridge;
        }

        bridge = IrisMaterialBridge.create(rendererClass);
        lushGrass$materialBridge = bridge;
        return bridge;
    }

    @Unique
    private static void lushGrass$logFailure(Throwable exception) {
        if (lushGrass$failureLogged.compareAndSet(false, true)) {
            LushGrass.LOGGER.warn(
                    "Could not apply the Iris short-grass material to Lush Grass tufts.",
                    exception
            );
        }
    }

}
