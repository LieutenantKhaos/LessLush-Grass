package com.github.kryvii.lushgrass.client;

import com.github.kryvii.lushgrass.client.event.ClientModelEvents;
import com.github.kryvii.lushgrass.config.ClientConfig;
import net.fabricmc.api.ClientModInitializer;

public final class LushGrassClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientConfig.load();
        ClientModelEvents.register();
    }
}
