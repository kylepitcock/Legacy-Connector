package org.kylepitcock.mccompat.v1_7_10.client;

import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemBlock;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.kylepitcock.mccompat.v1_7_10.bridge.ItemBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.TileEntityBridge;

/** Client-only registrations for 1.7.10 content (screens, tile entity renderers, special item renderers). */
public final class Client1710 {
    private Client1710() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientGui::onRegisterScreens);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent e) -> {
            for (var key : mccompat.v1_7_10.net.minecraft.client.settings.KeyBinding.mcc$registered()) {
                try {
                    e.register(key.mcc$modern());
                } catch (RuntimeException ex) {
                    org.slf4j.LoggerFactory.getLogger("Retrofit/Keys").warn("Could not register key binding {}", key.getKeyDescription(), ex);
                }
            }
        });
        modBus.addListener(Client1710::onRegisterRenderers);
        LegacyParticles.register();
        ClientSelfTest.register();
        WorldScreenshotTest.register();
        modBus.addListener(Client1710::onRegisterExtensions);
        modBus.addListener(Client1710::onRegisterItemColors);
        modBus.addListener(Client1710::onModifyBaking);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (TileEntityBridge.type() != null) event.registerBlockEntityRenderer(TileEntityBridge.type(), ctx -> new LegacyBlockEntityRenderer());
        for (var type : org.kylepitcock.mccompat.v1_7_10.entity.LegacyHosts.allTypes()) {
            @SuppressWarnings("unchecked")
            var t = (net.minecraft.world.entity.EntityType<net.minecraft.world.entity.Entity>) type;
            event.registerEntityRenderer(t, LegacyEntityRenderer::new);
        }
    }

    /** Blocks drawn by a legacy ISimpleBlockRenderingHandler get a model that runs it (see IsbrhBlockModel). */
    private static void onModifyBaking(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        int replaced = 0;
        for (Block block : org.kylepitcock.mccompat.v1_7_10.bridge.BlockBridge.modBlocks()) {
            if (block.mcc$modern == null) continue;
            int renderId;
            try {
                renderId = block.getRenderType();
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            if (mccompat.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.mcc$blockHandler(renderId) == null) continue;
            for (var state : block.mcc$modern.getStateDefinition().getPossibleStates()) {
                var location = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state);
                var original = models.get(location);
                if (original == null || original instanceof IsbrhBlockModel) continue;
                models.put(location, new IsbrhBlockModel(original, block, renderId));
                replaced++;
            }
        }
        if (replaced > 0) org.slf4j.LoggerFactory.getLogger("Retrofit/ISBRH").info("{} block states are drawn by legacy block renderers", replaced);
    }

    /** Legacy items tinted per render pass (spawn eggs, dyed items): layer N of the item model is pass N. */
    private static void onRegisterItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        for (Item item : ItemBridge.modItems()) {
            try {
                if (!org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockAdapter.overrides(item, "getColorFromItemStack",
                        mccompat.v1_7_10.net.minecraft.item.ItemStack.class, int.class)) continue;
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            event.register((stack, tint) -> {
                try {
                    return 0xFF000000 | item.getColorFromItemStack(mccompat.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(stack), tint);
                } catch (RuntimeException | LinkageError e) {
                    return 0xFFFFFFFF;
                }
            }, item.mcc$variants.values().toArray(new net.minecraft.world.item.Item[0]));
        }
    }

    private static void onRegisterExtensions(RegisterClientExtensionsEvent event) {
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
                org.slf4j.LoggerFactory.getLogger("Retrofit/Client").debug("Skipping client extensions of {}: {}", item, e.toString());
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
