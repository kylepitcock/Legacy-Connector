package org.kylepitcock.legacyconnector.v1_7_10.client;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemBlock;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.TileEntityBridge;

/** Client-only registrations for 1.7.10 content (screens, tile entity renderers, special item renderers). */
public final class Client1710 {
    private Client1710() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientGui::onRegisterScreens);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent e) -> {
            for (var key : legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding.mcc$registered()) {
                try {
                    e.register(key.mcc$modern());
                } catch (RuntimeException ex) {
                    org.slf4j.LoggerFactory.getLogger("LegacyConnector/Keys").warn("Could not register key binding {}", key.getKeyDescription(), ex);
                }
            }
        });
        modBus.addListener(Client1710::onRegisterRenderers);
        modBus.addListener((net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent e) -> {
            // 1.7.10 fired SoundSetupEvent and SoundLoadEvent each time its sound system (re)started
            LegacySoundSources.engineReloaded();
            boolean loaded = LegacySoundSources.engineLoaded();
            var manager = legacyconnector.v1_7_10.net.minecraft.client.audio.SoundManager.INSTANCE;
            manager.mcc$engineLoaded(loaded);
            if (!loaded) return;
            var bus = legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge.EVENT_BUS;
            try {
                bus.post(new legacyconnector.v1_7_10.net.minecraftforge.client.event.sound.SoundSetupEvent(manager));
                bus.post(new legacyconnector.v1_7_10.net.minecraftforge.client.event.sound.SoundLoadEvent(manager));
            } catch (RuntimeException | LinkageError ex) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/SoundSystem").error("A legacy sound load handler failed", ex);
            }
        });
        LegacyParticles.register();
        LegacyMusic.register();
        ClientSelfTest.register();
        WorldScreenshotTest.register();
        ScreenEvents1710.register();
        RenderEvents1710.register();
        FirstPersonPose.register();
        LegacyKeyConflicts.register();
        LegacyArmorLayer.registerOverlayHiding();
        HudEvents1710.register();
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent e) ->
                e.register(org.kylepitcock.legacyconnector.v1_7_10.dimension.LegacyDimensions.EFFECTS, new LegacyDimensionEffects()));
        modBus.addListener((net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent e) -> {
            if (!e.getAtlas().location().equals(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)) return;
            AtlasUV.invalidate();
            org.kylepitcock.legacyconnector.v1_7_10.Assets1710.postStitched();
        });
        modBus.addListener(Client1710::onRegisterExtensions);
        modBus.addListener(Client1710::onRegisterItemColors);
        modBus.addListener(Client1710::onRegisterBlockColors);
        modBus.addListener(Client1710::onModifyBaking);
        modBus.addListener(Client1710::onAddLayers);
    }

    /** Legacy armor is drawn by its own layer on players and humanoid mobs. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer player) {
                player.addLayer(new LegacyArmorLayer(player));
                LegacyArmorLayer.hideFromHeadLayer(player);
            }
        }
        for (var type : event.getEntityTypes()) {
            var renderer = event.getRenderer((net.minecraft.world.entity.EntityType) type);
            if (renderer instanceof net.minecraft.client.renderer.entity.HumanoidMobRenderer humanoid) {
                humanoid.addLayer(new LegacyArmorLayer(humanoid));
                LegacyArmorLayer.hideFromHeadLayer(humanoid);
            } else if (renderer instanceof net.minecraft.client.renderer.entity.ArmorStandRenderer stand) {
                // armor stands show legacy armor too (1.7.10 had none, but players put legacy suits and armor on them)
                stand.addLayer(new LegacyArmorLayer(stand));
                LegacyArmorLayer.hideFromHeadLayer(stand);
            }
        }
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (TileEntityBridge.type() != null) event.registerBlockEntityRenderer(TileEntityBridge.type(), ctx -> new LegacyBlockEntityRenderer());
        for (var type : org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts.allTypes()) {
            @SuppressWarnings("unchecked")
            var t = (net.minecraft.world.entity.EntityType<net.minecraft.world.entity.Entity>) type;
            event.registerEntityRenderer(t, LegacyEntityRenderer::new);
        }
    }

    /** Blocks drawn by a legacy ISimpleBlockRenderingHandler get a model that runs it (see IsbrhBlockModel). */
    private static void onModifyBaking(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        int replaced = 0;
        for (Block block : org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.modBlocks()) {
            if (block.mcc$modern == null) continue;
            int renderId;
            try {
                renderId = block.getRenderType();
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            // a mod's handler, or 1.7.10's own renderer of a vanilla render type that no static model can show (panes)
            var builtIn = legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.mcc$blockHandler(renderId) == null
                    ? VanillaBlockRenderers.forRenderType(renderId) : null;
            if (builtIn == null && legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.mcc$blockHandler(renderId) == null) continue;
            for (var state : block.mcc$modern.getStateDefinition().getPossibleStates()) {
                var location = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state);
                var original = models.get(location);
                if (original == null || original instanceof IsbrhBlockModel) continue;
                models.put(location, new IsbrhBlockModel(original, block, renderId, builtIn));
                replaced++;
            }
        }
        if (replaced > 0) org.slf4j.LoggerFactory.getLogger("LegacyConnector/ISBRH").info("{} block states are drawn by legacy block renderers", replaced);
    }

    /** Blocks that colour themselves (leaves, grass, anything overriding colorMultiplier/getRenderColor) tint index 0 of their model. */
    private static void onRegisterBlockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        for (legacyconnector.v1_7_10.net.minecraft.block.Block block : org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.modBlocks()) {
            if (block.mcc$modern == null || !org.kylepitcock.legacyconnector.v1_7_10.Assets1710.tinted(block)) continue;
            event.register((state, level, pos, tint) -> {
                try {
                    if (level != null && pos != null) {
                        return 0xFF000000 | block.colorMultiplier(org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockAccessView.of(level), pos.getX(), pos.getY(), pos.getZ());
                    }
                    return 0xFF000000 | block.getRenderColor(org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.legacyMeta(state));
                } catch (RuntimeException | LinkageError e) {
                    return 0xFFFFFFFF;
                }
            }, block.mcc$modern);
        }
    }

    /** Legacy items tinted per render pass (spawn eggs, dyed items): layer N of the item model is pass N. */
    private static void onRegisterItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        for (Item item : ItemBridge.modItems()) {
            try {
                if (!org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter.overrides(item, "getColorFromItemStack",
                        legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class, int.class)) continue;
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            event.register((stack, tint) -> {
                try {
                    return 0xFF000000 | item.getColorFromItemStack(legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(stack), tint);
                } catch (RuntimeException | LinkageError e) {
                    return 0xFFFFFFFF;
                }
            }, item.mcc$variants.values().toArray(new net.minecraft.world.item.Item[0]));
        }
    }

    private static void onRegisterExtensions(RegisterClientExtensionsEvent event) {
        LegacyEffectIcons.register(event);
        IClientItemExtensions chest = new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new LegacyChestItemRenderer();
                return renderer;
            }
        };
        IClientItemExtensions custom = new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new LegacyItemRenderer();
                return renderer;
            }
        };
        for (Item item : ItemBridge.modItems()) {
            var variants = item.mcc$variants.values().toArray(new net.minecraft.world.item.Item[0]);
            try {
                if (item instanceof ItemBlock ib && isChestStyle(ib.mcc$block())) {
                    event.registerItem(chest, variants);
                } else {
                    // Only used where the generated model is builtin/entity: items with a Forge IItemRenderer, which mods
                    // may register after this event (the renderer is looked up per stack).
                    event.registerItem(custom, variants);
                }
            } catch (RuntimeException | LinkageError e) {
                // A mod whose classes failed to initialise must not take the whole game down at start-up.
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Client").debug("Skipping client extensions of {}: {}", item, e.toString());
                continue;
            }
        }
    }

    public static boolean isChestStyle(Block block) {
        try {
            return block.getRenderType() == 22;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
