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
        Item item = BLOCK_ITEMS.get(block);
        if (item == null && block != null && block.getRegistryName() != null && "minecraft".equals(block.getRegistryName().getNamespace())) {
            // a vanilla block (Blocks.LOG): its item is the vanilla item of the same name
            Item vanilla = Registries1122.ITEMS.getValue(block.getRegistryName());
            if (vanilla != null && vanilla.mcc$vanillaModern != null) item = vanilla;
            else if (block.mcc$modern instanceof net.minecraft.world.level.block.Block mb && mb.asItem() != net.minecraft.world.item.Items.AIR) item = Vanilla1122.itemForModern(mb.asItem());
        }
        return item;
    }

    public static synchronized List<Item> modItems() {
        return new ArrayList<>(MOD_ITEMS);
    }

    /** The modern item of a legacy item and metadata (the base variant when that subtype was never listed). */
    /** The modern items of an item, by subtype (metadata). */
    public static synchronized Map<Integer, net.minecraft.world.item.Item> variants(Item item) {
        Map<Integer, net.minecraft.world.item.Item> v = VARIANTS.get(item);
        return v == null ? Map.of() : new java.util.LinkedHashMap<>(v);
    }

    public static synchronized net.minecraft.world.item.Item modern(Item item, int meta) {
        Map<Integer, net.minecraft.world.item.Item> v = VARIANTS.get(item);
        if (v == null) return item.mcc$vanillaModern == null ? null : Vanilla1122.modernVariant(item, meta);
        net.minecraft.world.item.Item i = v.get(meta);
        return i != null ? i : v.values().iterator().next();
    }

    static synchronized void register(Item item) {
        ResourceLocation base = BlockBridge1122.modernId(item.getRegistryName());
        if (item instanceof ItemBlock ib && ib.getBlock() != null && BlockBridge1122.modern(ib.getBlock()) != null) {
            var block = BlockBridge1122.modern(ib.getBlock());
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            // an ItemBlock registered under its own name keeps it (BuildCraft's pipes: one pipe_holder block, an item per pipe type);
            // under the block's id they all collided and every one but the first was dropped, leaving unregistered items in the tabs
            ResourceLocation itemId = base != null && !base.equals(blockId) ? base : blockId;
            // known before the subtypes are listed: a block's getSubBlocks builds stacks of the block (new ItemStack(block, 1, meta)), which resolve to
            // this item only through here (BuildCraft's engines listed nothing but the first)
            if (itemId.equals(blockId) || !BLOCK_ITEMS.containsKey(ib.getBlock())) BLOCK_ITEMS.put(ib.getBlock(), item);
            // an ItemBlock with subtypes (Candy World's gummy, chocolate and candy cane blocks) is one modern item per metadata, as other items are
            Map<Integer, net.minecraft.world.item.Item> v = new LinkedHashMap<>();
            List<Integer> metas = item.getHasSubtypes() ? metas(item) : List.of(0);
            for (int meta : metas) {
                var variant = new Legacy1122BlockItem(item, meta, block, properties(item));
                ResourceLocation id = v.isEmpty() ? itemId : ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), itemId.getPath() + "_" + meta);
                LegacyRegistrar.register(BuiltInRegistries.ITEM, id, variant);
                v.put(meta, variant);
            }
            var modern = v.values().iterator().next();
            // the block's own item is its first subtype (the last one registered won Item.byBlock)
            net.minecraft.world.item.Item.BY_BLOCK.put(block, modern);
            item.mcc$modern = modern;
            VARIANTS.put(item, v);
            MOD_ITEMS.add(item);
            Dispensers1122.onItemRegistered(item);
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
        Dispensers1122.onItemRegistered(item);
    }

    /**
     * Items are registered (and their subtypes listed) when Forge posts the item registry event, before mods' own registries: an item
     * whose {@code getSubItems} reads one (Electroblob's Wizardry spell books: one subtype per registered spell) listed nothing but meta 0
     * then. After postInit (when mods have filled their registries) the subtypes are listed again and the missing variants registered.
     */
    public static synchronized void completeSubtypes() {
        int added = 0;
        for (Item item : new ArrayList<>(MOD_ITEMS)) {
            if (!item.getHasSubtypes()) continue;
            Map<Integer, net.minecraft.world.item.Item> variants = VARIANTS.get(item);
            if (variants == null || item.getRegistryName() == null) continue;
            ResourceLocation base = BlockBridge1122.modernId(item.getRegistryName());
            var listed = metas(item);
            for (int meta : listed) {
                if (variants.containsKey(meta)) continue;
                net.minecraft.world.item.Item modern;
                ResourceLocation id;
                if (item instanceof ItemBlock) {
                    var first = variants.values().iterator().next();
                    if (!(first instanceof Legacy1122BlockItem firstItem)) continue;
                    ResourceLocation firstId = BuiltInRegistries.ITEM.getKey(first);
                    modern = new Legacy1122BlockItem(item, meta, firstItem.getBlock(), properties(item));
                    id = ResourceLocation.fromNamespaceAndPath(firstId.getNamespace(), firstId.getPath() + "_" + meta);
                } else {
                    modern = new Legacy1122Item(item, meta, properties(item));
                    id = meta == 0 ? base : ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "_" + meta);
                }
                LegacyRegistrar.register(BuiltInRegistries.ITEM, id, modern);
                variants.put(meta, modern);
                added++;
            }
        }
        if (added > 0) LOG.info("{} item subtype(s) that depend on mod registries registered after them", added);
    }

    /** The metadata values the creative inventory lists for this item (at least 0). */
    private static List<Integer> metas(Item item) {
        List<Integer> out = new ArrayList<>();
        if (item.getHasSubtypes()) {
            try {
                NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> list = NonNullList.create();
                item.getSubItems(CreativeTabs.SEARCH, list);
                // many items list their subtypes only for their own tab (Electroblob's Wizardry spell books: `if (tab == WizardryTabs.SPELLS)`)
                var own = item.getCreativeTab();
                if (own != null && own != CreativeTabs.SEARCH) item.getSubItems(own, list);
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
        if (item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemFood food) {
            // 1.12 saturation is nutrition * modifier * 2, as the modern modifier is
            try {
                var b = new net.minecraft.world.food.FoodProperties.Builder()
                        .nutrition(food.getHealAmount(legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY))
                        .saturationModifier(food.getSaturationModifier(legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY));
                if (food.mcc$alwaysEdible()) b.alwaysEdible();
                // soups (ItemSoup) leave their bowl
                if (food instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemSoup) b.usingConvertsTo(net.minecraft.world.item.Items.BOWL);
                var effect = food.mcc$potionEffect() == null ? null : food.mcc$potionEffect().mcc$toModern();
                if (effect != null) b.effect(() -> new net.minecraft.world.effect.MobEffectInstance(effect), food.mcc$potionEffectProbability());
                p.food(b.build());
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Could not read the food values of {}: {}", item.getRegistryName(), e.toString());
            }
        }
        return p;
    }
}
