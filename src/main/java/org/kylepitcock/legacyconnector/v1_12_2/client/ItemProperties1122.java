package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;

/**
 * Item model properties of mod items ({@code Item.addPropertyOverride}): the model JSON's {@code overrides} predicates select on properties
 * the game must know, so each getter a mod item registered is registered with the modern {@link ItemProperties} for every modern item of the
 * legacy item (its subtypes), calling the legacy getter with the legacy view of the stack, world and entity.
 */
final class ItemProperties1122 {
    private ItemProperties1122() {}

    static void register() {
        int count = 0;
        for (var item : ItemBridge1122.modItems()) {
            java.util.Map<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, legacyconnector.v1_12_2.net.minecraft.item.IItemPropertyGetter> props;
            try {
                props = item.mcc$properties();
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            if (props == null || props.isEmpty()) continue;
            java.util.Set<net.minecraft.world.item.Item> done = new java.util.HashSet<>();
            for (int meta = 0; meta < 16; meta++) {
                var modern = ItemBridge1122.modern(item, meta);
                if (modern == null || !done.add(modern)) continue;
                for (var entry : props.entrySet()) {
                    var getter = entry.getValue();
                    ClampedItemPropertyFunction function = new ClampedItemPropertyFunction() {
                        @Override
                        public float unclampedCall(net.minecraft.world.item.ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                            try {
                                EntityLivingBase living = entity == null ? null : EntityBridge1122.wrap(entity) instanceof EntityLivingBase l ? l : null;
                                return getter.apply(Stacks1122.fromModern(stack), level == null ? null : WorldBridge1122.wrap(level), living);
                            } catch (RuntimeException | LinkageError e) {
                                RenderErrors1122.report("an item model property of " + item.getClass().getName(), e);
                                return 0.0F;
                            }
                        }
                    };
                    ItemProperties.register(modern, entry.getKey().mcc$modern(), function);
                    count++;
                }
            }
        }
        if (count > 0) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Models1122").info("{} 1.12.2 item model properties registered", count);
    }
}
