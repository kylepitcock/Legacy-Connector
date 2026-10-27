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
 * legacy sprites take their places after each build, then TextureStitchEvent.Post), tile entity renderers, and the non-rendering client
 * registrations: container screens, key bindings, particles and resource reload listeners.
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
            try {
                CustomModels1122.registerTextures();
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("listing the textures of custom-loader models", e);
            }
            // textures of OBJ materials (Assets1122 ran first and collected them)
            for (String texture : org.kylepitcock.legacyconnector.v1_12_2.ObjAssets1122.TEXTURES) Sprites1122.register(texture);
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
        // layers mods add to the legacy player renderers run on the modern ones
        modBus.addListener((EntityRenderersEvent.AddLayers e) -> {
            if (present()) LegacyPlayerLayer1122.addTo(e);
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block e) -> {
            if (present()) Colors1122.onBlockColors(e);
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e) -> {
            if (present()) Colors1122.onItemColors(e);
        });
        modBus.addListener(Client1122::onModifyBaking);
        // plain model locations mods asked ModelLoaderRegistry for are baked as standalone models
        modBus.addListener((net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e) -> {
            if (!present()) return;
            for (var location : legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoaderRegistry.mcc$requested()) {
                e.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(location.mcc$modern()));
            }
        });
        // screens, particles, key bindings, resource reloads
        modBus.addListener(ClientGui1122::onRegisterScreens);
        LegacyParticles1122.register();
        ParticleDraw1122.install();
        ScreenEvents1122.register();
        ClientEvents1122.register();
        ClientCommands1122.register();
        // 1.12 told its resource reload listeners again after every resource reload
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent e) -> e.registerReloadListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> {
                    if (legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().getResourceManager()
                            instanceof legacyconnector.v1_12_2.net.minecraft.client.resources.SimpleReloadableResourceManager m) m.mcc$reloaded();
                }));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent e) -> {
            for (Object key : legacyconnector.v1_12_2.net.minecraft.client.settings.KeyBinding.mcc$modernMappings()) {
                try {
                    e.register((net.minecraft.client.KeyMapping) key);
                } catch (RuntimeException ex) {
                    org.slf4j.LoggerFactory.getLogger("LegacyConnector/Keys").warn("Could not register key binding {}", ((net.minecraft.client.KeyMapping) key).getName(), ex);
                }
            }
        });
    }

    /** ModelBakeEvent: mods look up and replace baked models by legacy location. */
    private static void onModifyBaking(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event) {
        if (!present()) return;
        Models1122.invalidate();
        CustomModels1122.setBaking(event.getModels());
        try {
            CustomModels1122.bakeInto(event.getModels());
        } catch (RuntimeException | LinkageError e) {
            RenderErrors1122.report("baking custom-loader models", e);
        }
        BakedRegistry1122 registry = new BakedRegistry1122(event.getModels());
        try {
            MinecraftForge.EVENT_BUS.post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.ModelBakeEvent(
                    legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelManager.mcc$instance(), registry,
                    new legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader()));
        } catch (RuntimeException | LinkageError e) {
            RenderErrors1122.report("posting ModelBakeEvent", e);
        }
        CustomModels1122.setBaking(null);
        if (registry.replaced > 0) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Models").info("1.12.2 mods replaced {} baked model(s)", registry.replaced);
    }

    private static Boolean present;

    /** Whether any 1.12.2 mod is loaded (otherwise none of this runs). */
    static boolean present() {
        if (present == null) present = LegacyMods.all().stream().anyMatch(m -> "1.12.2".equals(m.version));
        return present;
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (TileEntityBridge1122.type() != null) event.registerBlockEntityRenderer(TileEntityBridge1122.type(), ctx -> new LegacyBlockEntityRenderer1122());
        // 1.12.2 mod entities: drawn by the legacy renderer their mod registered (RenderingRegistry)
        for (var type : org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.allTypes()) {
            @SuppressWarnings("unchecked")
            var t = (net.minecraft.world.entity.EntityType<net.minecraft.world.entity.Entity>) type;
            event.registerEntityRenderer(t, LegacyEntityRenderer1122::new);
        }
    }
}
