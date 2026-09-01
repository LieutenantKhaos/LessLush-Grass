package com.github.kryvii.lushgrass.client.compat.iris;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.world.level.block.Blocks;

public final class IrisMaterialBridge {
    private final Class<?> rendererClass;
    private final Method overrideBlock;
    private final Method restoreBlock;
    private final Object settings;
    private final Method getBlockStateIds;

    public static IrisMaterialBridge create(Class<?> rendererClass) throws ReflectiveOperationException {
        Class<?> settingsClass = Class.forName(
                "net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings"
        );
        Field settingsInstance = settingsClass.getField("INSTANCE");
        return new IrisMaterialBridge(
                rendererClass,
                rendererClass.getMethod("overrideBlock", int.class),
                rendererClass.getMethod("restoreBlock"),
                settingsInstance.get(null),
                settingsClass.getMethod("getBlockStateIds")
        );
    }

    private IrisMaterialBridge(
            Class<?> rendererClass,
            Method overrideBlock,
            Method restoreBlock,
            Object settings,
            Method getBlockStateIds
    ) {
        this.rendererClass = rendererClass;
        this.overrideBlock = overrideBlock;
        this.restoreBlock = restoreBlock;
        this.settings = settings;
        this.getBlockStateIds = getBlockStateIds;
    }

    public boolean supports(Class<?> candidate) {
        return this.rendererClass == candidate;
    }

    public boolean overrideShortGrass(Object renderer) throws ReflectiveOperationException {
        int materialId = this.shortGrassMaterialId();
        if (materialId < 0) {
            return false;
        }
        this.overrideBlock.invoke(renderer, materialId);
        return true;
    }

    public void restore(Object renderer) throws ReflectiveOperationException {
        this.restoreBlock.invoke(renderer);
    }

    private int shortGrassMaterialId() throws ReflectiveOperationException {
        Object blockStateIds = this.getBlockStateIds.invoke(this.settings);
        if (!(blockStateIds instanceof Map<?, ?> materialIds)) {
            return -1;
        }
        Object value = materialIds.get(Blocks.SHORT_GRASS.defaultBlockState());
        return value instanceof Integer materialId ? materialId : -1;
    }
}
