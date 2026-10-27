package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap;
import legacyconnector.v1_12_2.net.minecraftforge.client.event.TextureStitchEvent;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.server.packs.PackType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.TileEntityBridge1122;

/**
 * Client rendering hooks of the 1.12.2 shim: the block atlas (sprites registered in TextureStitchEvent.Pre become atlas sources; the
 * legacy sprites take their places after each build, then TextureStitchEvent.Post) and tile entity renderers.
 */
public final class Client1122 {
    private Client1122() {}

    public static void register(IEventBus modBus) {
        GeneratedPack.addContributor((type, out) -> {
            if (type != PackType.CLIENT_RESOURCES || !present()) return;
            try {
                MinecraftForge.EVENT_BUS.post(new TextureStitchEvent.Pre(TextureMap.mcc$blocks()));
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("posting TextureStitchEvent.Pre", e);
            }
            Sprites1122.contribute(out);
        });
        modBus.addListener((TextureAtlasStitchedEvent e) -> {
            if (!e.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS) || !present()) return;
            TextureMap.mcc$blocks().mcc$synchronize();
            try {
                MinecraftForge.EVENT_BUS.post(new TextureStitchEvent.Post(TextureMap.mcc$blocks()));
            } catch (RuntimeException | LinkageError ex) {
                RenderErrors1122.report("posting TextureStitchEvent.Post", ex);
            }
        });
        modBus.addListener(Client1122::onRegisterRenderers);
    }

    private static Boolean present;

    /** Whether any 1.12.2 mod is loaded (otherwise none of this runs). */
    static boolean present() {
        if (present == null) present = LegacyMods.all().stream().anyMatch(m -> "1.12.2".equals(m.version));
        return present;
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (TileEntityBridge1122.type() != null) event.registerBlockEntityRenderer(TileEntityBridge1122.type(), ctx -> new LegacyBlockEntityRenderer1122());
    }
}
