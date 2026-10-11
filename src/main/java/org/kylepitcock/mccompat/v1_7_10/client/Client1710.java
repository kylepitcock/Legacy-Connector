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
        modBus.addListener(Client1710::onRegisterRenderers);
        LegacyParticles.register();
        ClientSelfTest.register();
        modBus.addListener(Client1710::onRegisterExtensions);
        modBus.addListener(Client1710::onRegisterItemColors);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (TileEntityBridge.type() != null) event.registerBlockEntityRenderer(TileEntityBridge.type(), ctx -> new LegacyBlockEntityRenderer());
        for (var type : org.kylepitcock.mccompat.v1_7_10.entity.LegacyHosts.allTypes()) {
            @SuppressWarnings("unchecked")
            var t = (net.minecraft.world.entity.EntityType<net.minecraft.world.entity.Entity>) type;
            event.registerEntityRenderer(t, LegacyEntityRenderer::new);
        }
    }

    /** Legacy items tinted per render pass (spawn eggs, dyed items): layer N of the item model is pass N. */
    private static void onRegisterItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        for (Item item : ItemBridge.modItems()) {
            if (!org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockAdapter.overrides(item, "getColorFromItemStack",
                    mccompat.v1_7_10.net.minecraft.item.ItemStack.class, int.class)) continue;
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
            if (item instanceof ItemBlock ib && isChestStyle(ib.mcc$block())) {
                event.registerItem(chest, variants);
            } else {
                // Only used where the generated model is builtin/entity: items with a Forge IItemRenderer, which mods
                // may register after this event (the renderer is looked up per stack).
                event.registerItem(custom, variants);
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
