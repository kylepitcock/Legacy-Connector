package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemBlock;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockItem;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns 1.12.2 items into modern items. Subtypes (an item's metadata values, as its {@code getSubItems} lists them) become one
 * modern item each, as the 1.13 flattening did: {@code mod:gem}, {@code mod:gem_1}, ...
 */
public final class ItemBridge1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Items");
    private static final Map<Item, Map<Integer, net.minecraft.world.item.Item>> VARIANTS = new IdentityHashMap<>();
    private static final Map<Block, Item> BLOCK_ITEMS = new IdentityHashMap<>();
    private static final List<Item> MOD_ITEMS = new ArrayList<>();

    private ItemBridge1122() {}

    public static synchronized Item itemOf(Block block) {
        return BLOCK_ITEMS.get(block);
    }

    public static synchronized List<Item> modItems() {
        return new ArrayList<>(MOD_ITEMS);
    }

    /** The modern item of a legacy item and metadata (the base variant when that subtype was never listed). */
    public static synchronized net.minecraft.world.item.Item modern(Item item, int meta) {
        Map<Integer, net.minecraft.world.item.Item> v = VARIANTS.get(item);
        if (v == null) return item.mcc$vanillaModern;
        net.minecraft.world.item.Item i = v.get(meta);
        return i != null ? i : v.values().iterator().next();
    }

    static synchronized void register(Item item) {
        ResourceLocation base = BlockBridge1122.modernId(item.getRegistryName());
        if (item instanceof ItemBlock ib && ib.getBlock() != null && BlockBridge1122.modern(ib.getBlock()) != null) {
            var block = BlockBridge1122.modern(ib.getBlock());
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            var modern = new Legacy1122BlockItem(item, block, properties(item));
            LegacyRegistrar.register(BuiltInRegistries.ITEM, blockId, modern);
            item.mcc$modern = modern;
            BLOCK_ITEMS.put(ib.getBlock(), item);
            Map<Integer, net.minecraft.world.item.Item> v = new LinkedHashMap<>();
            v.put(0, modern);
            VARIANTS.put(item, v);
            MOD_ITEMS.add(item);
            return;
        }
        Map<Integer, net.minecraft.world.item.Item> variants = new LinkedHashMap<>();
        for (int meta : metas(item)) {
            var modern = new Legacy1122Item(item, meta, properties(item));
            ResourceLocation id = meta == 0 ? base : ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "_" + meta);
            LegacyRegistrar.register(BuiltInRegistries.ITEM, id, modern);
            variants.put(meta, modern);
        }
        item.mcc$modern = variants.values().iterator().next();
        VARIANTS.put(item, variants);
        MOD_ITEMS.add(item);
    }

    /** The metadata values the creative inventory lists for this item (at least 0). */
    private static List<Integer> metas(Item item) {
        List<Integer> out = new ArrayList<>();
        if (item.getHasSubtypes()) {
            try {
                NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> list = NonNullList.create();
                item.getSubItems(CreativeTabs.SEARCH, list);
                for (var s : list) if (!s.isEmpty() && s.getItem() == item && !out.contains(s.getMetadata())) out.add(s.getMetadata());
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Could not list the subtypes of {}: {}", item.getRegistryName(), e.toString());
            }
        }
        if (out.isEmpty()) out.add(0);
        return out;
    }

    private static net.minecraft.world.item.Item.Properties properties(Item item) {
        var p = new net.minecraft.world.item.Item.Properties();
        if (item.isDamageable()) p.durability(item.getMaxDamage());
        else p.stacksTo(Math.max(1, Math.min(99, item.getItemStackLimit())));
        return p;
    }
}
