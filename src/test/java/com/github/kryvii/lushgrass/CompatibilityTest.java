package com.github.kryvii.lushgrass;

import com.github.kryvii.lushgrass.client.model.ConfigurableGrassBlockModel;
import com.github.kryvii.lushgrass.config.ClientConfig;
import com.google.gson.JsonParser;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.GameProvider;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilityTest {
    @BeforeAll
    static void initializeConfig(@TempDir Path directory) {
        GameProvider provider = (GameProvider) Proxy.newProxyInstance(
                GameProvider.class.getClassLoader(), new Class<?>[]{GameProvider.class},
                (proxy, method, args) -> method.getName().equals("getLaunchDirectory") ? directory : null);
        FabricLoaderImpl.INSTANCE.setGameProvider(provider);
    }

    @Test
    void voxyStaticBakeTracksCoverageAndDoesNotAppendOtherParts() {
        CountingModel full = new CountingModel();
        CountingModel vanilla = new CountingModel();
        var model = new ConfigurableGrassBlockModel(full, vanilla);
        List<BlockStateModelPart> parts = new ArrayList<>();
        ClientConfig.update(true, 2, 1, true, 12);
        model.collectParts(null, parts);
        assertEquals(1, full.calls);
        assertEquals(0, vanilla.calls);
        ClientConfig.update(false, 2, 1, true, 12);
        model.collectParts(null, parts);
        assertEquals(1, full.calls);
        assertEquals(1, vanilla.calls);
        assertTrue(parts.isEmpty());
    }

    @Test
    void numericConfigRejectsFractionsOverflowAndOutOfRangeValues() throws Exception {
        var read = ClientConfig.class.getDeclaredMethod("readInt", com.google.gson.JsonObject.class,
                String.class, int.class, int.class, int.class);
        read.setAccessible(true);
        for (String invalid : new String[]{"1.5", "4294967296", "-1", "5", "\"2\""}) {
            var object = JsonParser.parseString("{\"density\":" + invalid + "}").getAsJsonObject();
            assertThrows(InvocationTargetException.class, () -> read.invoke(null, object, "density", 2, 0, 4));
        }
        var object = JsonParser.parseString("{\"density\":4}").getAsJsonObject();
        assertEquals(4, read.invoke(null, object, "density", 2, 0, 4));
    }

    private static final class CountingModel implements BlockStateModel {
        int calls;
        public void collectParts(RandomSource random, List<BlockStateModelPart> parts) { calls++; }
        public Material.Baked particleMaterial() { return null; }
        public int materialFlags() { return 0; }
    }
}
