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
        modBus.addListener(Client1710::onRegisterExtensions);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (TileEntityBridge.type() != null) event.registerBlockEntityRenderer(TileEntityBridge.type(), ctx -> new LegacyBlockEntityRenderer());
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
        for (Item item : ItemBridge.modItems()) {
            if (item instanceof ItemBlock ib && isChestStyle(ib.mcc$block())) {
                event.registerItem(chest, item.mcc$variants.values().toArray(new net.minecraft.world.item.Item[0]));
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
