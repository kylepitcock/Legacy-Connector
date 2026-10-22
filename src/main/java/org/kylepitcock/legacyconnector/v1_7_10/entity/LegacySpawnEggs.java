package org.kylepitcock.legacyconnector.v1_7_10.entity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;

/**
 * FML global entity eggs ({@code registerGlobalEntityID} with colours) as modern spawn eggs: one item per legacy mob,
 * with the mod's colours, in the spawn egg tab.
 */
public final class LegacySpawnEggs {
    private static final List<ResourceLocation> EGGS = new CopyOnWriteArrayList<>();

    private LegacySpawnEggs() {}

    @SuppressWarnings("unchecked")
    public static synchronized void register(LegacyHosts.Info info, int background, int foreground) {
        if (info == null || info.kind() == LegacyHosts.Kind.ENTITY) return;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(info.id().getNamespace(), info.id().getPath() + "_spawn_egg");
        if (BuiltInRegistries.ITEM.containsKey(id)) return;
        EntityType<? extends Mob> type = (EntityType<? extends Mob>) info.type();
        Supplier<EntityType<? extends Mob>> supplier = () -> type;
        LegacyRegistrar.register(BuiltInRegistries.ITEM, id, new Egg(supplier, background, foreground, type));
        EGGS.add(id);
    }

    public static List<ResourceLocation> ids() {
        return List.copyOf(EGGS);
    }

    public static void addToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.SPAWN_EGGS) return;
        for (ResourceLocation id : EGGS) event.accept(BuiltInRegistries.ITEM.get(id));
    }

    /** Generated item models (the vanilla spawn egg template; NeoForge tints its two layers). */
    public static void contributeModels(Map<String, Supplier<byte[]>> out) {
        byte[] model = "{\"parent\":\"minecraft:item/template_spawn_egg\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (ResourceLocation id : EGGS) out.put("assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json", () -> model);
    }

    private static final class Egg extends DeferredSpawnEggItem {
        private final EntityType<?> type;

        Egg(Supplier<EntityType<? extends Mob>> supplier, int background, int foreground, EntityType<?> type) {
            super(supplier, background, foreground, new Item.Properties());
            this.type = type;
        }

        @Override
        public Component getName(ItemStack stack) {
            Component entity = LegacyHosts.displayName(type);
            return Component.translatable("legacyconnector.spawn_egg", entity != null ? entity : type.getDescription());
        }
    }
}
