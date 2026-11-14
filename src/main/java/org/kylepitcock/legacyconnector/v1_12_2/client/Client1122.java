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
        // late: after Assets1122, whose OBJ models collect the material textures registered here
        GeneratedPack.addLateContributor((type, out) -> {
            if (type != PackType.CLIENT_RESOURCES || !present()) return;
            // a handler reading a resource while the pack is being built asks for the pack again: that nested run does not repeat the event
            boolean nested = Thread.holdsLock(STITCH_PRE);
            synchronized (STITCH_PRE) {
                if (!nested) {
                    // mods' resource listeners come first in a reload (Forge 1.12 registered them before the game's own)
                    try {
                        if (legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().getResourceManager()
                                instanceof legacyconnector.v1_12_2.net.minecraft.client.resources.SimpleReloadableResourceManager m) m.mcc$reloading();
                    } catch (RuntimeException | LinkageError e) {
                        RenderErrors1122.report("telling the resource reload listeners a reload starts", e);
                    }
                    // Forge loaded the models (and so ran the custom loaders) before it posted the event: Pre handlers ask the registry for models
                    // that must already be cached (Tinkers' Construct walks the textures its loaders registered)
                    try {
                        CustomModels1122.registerTextures();
                    } catch (RuntimeException | LinkageError e) {
                        RenderErrors1122.report("loading the custom-loader models", e);
                    }
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
                    // sprites that make their own pixels (Tinkers' Construct's material textures) become texture files of the pack
                    try {
                        CustomSprites1122.generate(out);
                    } catch (RuntimeException | LinkageError e) {
                        RenderErrors1122.report("generating the sprites that make their own pixels", e);
                    }
                }
            }
            // textures of OBJ materials (Assets1122 ran first and collected them)
            for (String texture : org.kylepitcock.legacyconnector.v1_12_2.ObjAssets1122.TEXTURES) Sprites1122.register(texture);
            for (String texture : org.kylepitcock.legacyconnector.v1_12_2.Assets1122.MODEL_TEXTURES) Sprites1122.register(texture);
            try {
                // textures the models name in folders the atlas does not scan (textures/living/x.png)
                org.kylepitcock.legacyconnector.v1_12_2.Assets1122.registerModelTextures(out);
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("listing the model textures outside blocks/ and items/", e);
            }
            try {
                // the fluids' textures (Forge 1.12 stitched every fluid's) and the generated buckets' item models
                Fluids1122.contribute(out);
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("adding the fluid textures and bucket models", e);
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
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent e) ->
                e.register(org.kylepitcock.legacyconnector.v1_12_2.dimension.Dimensions1122.EFFECTS, new LegacyDimensionEffects1122()));
        // layers mods add to the legacy player renderers run on the modern ones
        modBus.addListener((EntityRenderersEvent.AddLayers e) -> {
            if (present()) LegacyPlayerLayer1122.addTo(e);
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block e) -> {
            if (present()) Colors1122.onBlockColors(e);
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e) -> {
            if (present()) Colors1122.onItemColors(e);
            if (present()) Fluids1122.onItemColors(e);
        });
        // 1.12.2 fluids: textures and colours of their fluid types, render layers of those placed in the world
        modBus.addListener((net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e) -> {
            if (present()) Fluids1122.onRegisterClientExtensions(e);
        });
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            if (present()) Fluids1122.setRenderLayers();
        }));
        modBus.addListener(Client1122::onModifyBaking);
        // the models of states beyond the metadata are blockstate variants baked on their own (rotation with uvlock)
        modBus.addListener((net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders e) ->
                e.register(VariantModel1122.ID, VariantModel1122.Loader.INSTANCE));
        // model predicates of mod items (Item.addPropertyOverride)
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            if (present()) ItemProperties1122.register();
        }));
        // status icons of mod potions (the modern effect sprites do not exist for them)
        modBus.addListener((net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e) -> {
            if (present()) EffectIcons1122.register(e);
        });
        // plain model locations mods asked ModelLoaderRegistry for are baked as standalone models
        modBus.addListener((net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e) -> {
            if (!present()) return;
            for (var location : legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoaderRegistry.mcc$requested()) {
                e.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(location.mcc$modern()));
            }
            // models an item registered beside its own (ModelBakery.registerItemVariants: Botania's gun and its clip)
            for (var model : legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$allItemVariantModels().values()) {
                e.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(model));
            }
            // the models of block states beyond their metadata (their variants have no place in the modern blockstate file)
            for (var model : org.kylepitcock.legacyconnector.v1_12_2.Assets1122.stateModelIds()) {
                e.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(model));
            }
        });
        // screens, particles, key bindings, resource reloads
        modBus.addListener(ClientGui1122::onRegisterScreens);
        LegacyParticles1122.register();
        ParticleDraw1122.install();
        ScreenEvents1122.register();
        ClientEvents1122.register();
        modBus.addListener(ClientEvents1122::onSoundEngineLoad);
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
        var textureGetter = event.getTextureGetter();
        Sprites1122.setBaking(id -> textureGetter.apply(new net.minecraft.client.resources.model.Material(TextureAtlas.LOCATION_BLOCKS, id)));
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
        Sprites1122.setBaking(null);
        try {
            ActualStateModels1122.wrap(event.getModels());
        } catch (RuntimeException | LinkageError e) {
            RenderErrors1122.report("choosing models by actual state", e);
        }
        try {
            CustomRenderTypeModels1122.wrap(event.getModels());
        } catch (RuntimeException | LinkageError e) {
            RenderErrors1122.report("wrapping the models of custom render types", e);
        }
        if (registry.replaced > 0) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Models").info("1.12.2 mods replaced {} baked model(s)", registry.replaced);
    }

    private static Boolean present;

    /**
     * Forge 1.12 stitched the block atlas (TextureStitchEvent.Pre) between preInit and init, so mods took their sprites from the event
     * and used them in init and postInit (Advent of Ascension's particle atlas). The game stitches after the mods load: the same
     * event goes out once here, the sprites are placed by the real stitch later (the handlers run again then).
     */
    public static void earlyStitchPre() {
        GeneratedPack.whileBuilding(() -> {
            synchronized (STITCH_PRE) {
                try {
                    MinecraftForge.EVENT_BUS.post(new TextureStitchEvent.Pre(TextureMap.mcc$blocks()));
                } catch (RuntimeException | LinkageError e) {
                    RenderErrors1122.report("posting the early TextureStitchEvent.Pre", e);
                }
            }
        });
    }

    /**
     * The mods load on a worker thread while the main thread opens the generated pack (which posts the real Pre event): the two runs of a mod's
     * Pre handler share its state (Tinkers' Construct iterates the base textures its model loaders add to), so only one runs at a time.
     */
    private static final Object STITCH_PRE = new Object();

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
