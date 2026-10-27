package com.github.kryvii.lushgrass.client.event;

import com.github.kryvii.lushgrass.client.world.DevelopedAreaDetector;
import net.minecraft.client.Minecraft;
import net.fabricmc.loader.api.FabricLoader;

public final class ClientConfigEvents {
    public static void refreshRenderer(boolean coverageChanged) {
        DevelopedAreaDetector.clearCache();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.level != null) {
                // Voxy 0.2.18 hooks allChanged to rebake its static model atlas.
                if (coverageChanged && FabricLoader.getInstance().isModLoaded("voxy")) {
                    minecraft.levelExtractor.allChanged();
                    return;
                }
                minecraft.levelRenderer.invalidateCompiledGeometry(
                        minecraft.level,
                        minecraft.options,
                        minecraft.gameRenderer.mainCamera(),
                        minecraft.getBlockColors()
                );
            }
        });
    }

    private ClientConfigEvents() {
    }
}
