package com.github.kryvii.lushgrass.client.event;

import net.minecraft.client.Minecraft;

public final class ClientConfigEvents {
    public static void refreshRenderer() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.level != null) {
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
