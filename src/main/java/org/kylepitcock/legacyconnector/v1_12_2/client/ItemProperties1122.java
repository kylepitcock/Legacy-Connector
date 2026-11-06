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
            count += registerMesh(item);
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

    /**
     * An item with an {@code ItemMeshDefinition} that picks a model per stack: the generated model (Assets1122.meshModel) selects its variants
     * on this property, the 1-based position in the item's registered variants of the model the mesh definition returns for the stack.
     */
    private static int registerMesh(legacyconnector.v1_12_2.net.minecraft.item.Item item) {
        var mesh = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$mesh(item);
        var variants = legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$variantList(item);
        if (mesh == null || variants.isEmpty()) return 0;
        java.util.Map<String, Integer> index = new java.util.HashMap<>();
        for (int i = 0; i < variants.size(); i++) {
            var v = variants.get(i);
            index.putIfAbsent(org.kylepitcock.legacyconnector.core.Ids.namespace(v.getNamespace()) + ":" + org.kylepitcock.legacyconnector.core.Ids.normalizePath(v.getPath()), i + 1);
        }
        int count = 0;
        java.util.Set<net.minecraft.world.item.Item> done = new java.util.HashSet<>();
        for (var entry : ItemBridge1122.variants(item).entrySet()) {
            var modern = entry.getValue();
            if (modern == null || !done.add(modern)) continue;
            ClampedItemPropertyFunction function = new ClampedItemPropertyFunction() {
                /** The position is the value (the clamped default call would cut it to 1). */
                @Override
                public float call(net.minecraft.world.item.ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                    return unclampedCall(stack, level, entity, seed);
                }

                @Override
                public float unclampedCall(net.minecraft.world.item.ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                    try {
                        var location = mesh.getModelLocation(Stacks1122.fromModern(stack));
                        if (location == null) return 0.0F;
                        Integer position = index.get(org.kylepitcock.legacyconnector.core.Ids.namespace(location.getNamespace()) + ":"
                                + org.kylepitcock.legacyconnector.core.Ids.normalizePath(location.getPath()));
                        return position == null ? 0.0F : position;
                    } catch (RuntimeException | LinkageError e) {
                        RenderErrors1122.report("the item mesh definition of " + item.getClass().getName(), e);
                        return 0.0F;
                    }
                }
            };
            ItemProperties.register(modern, org.kylepitcock.legacyconnector.v1_12_2.Assets1122.MESH_PROPERTY, function);
            count++;
        }
        return count;
    }
}
