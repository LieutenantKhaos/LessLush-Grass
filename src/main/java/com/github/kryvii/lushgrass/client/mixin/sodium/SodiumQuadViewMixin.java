package com.github.kryvii.lushgrass.client.mixin.sodium;

import com.github.kryvii.lushgrass.client.compat.sodium.SodiumQuadViewAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.QuadViewImpl", remap = false)
public abstract class SodiumQuadViewMixin implements SodiumQuadViewAccess {
    @Shadow(remap = false)
    public abstract int getTag();

    @Override
    public int lushGrass$getTag() {
        return this.getTag();
    }
}
