package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.item.BlockItem;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;

/**
 * Beta 1.7.3 had no creative inventory, so its mods name no tabs: each mod with content gets one tab, its blocks then its items in the
 * order the mod made them. A block item with subtypes lists every metadata whose sprites differ from the ones before (wool-like blocks).
 */
public final class CreativeTabsB173 {
    private CreativeTabsB173() {}

    /** The modern tab id of every Beta mod that has one (modern id of the mod -> tab id). */
    private static final Map<String, ResourceLocation> TABS = new java.util.concurrent.ConcurrentHashMap<>();

    /** The id of the creative tab made for the mod with this modern id (Beta has no tab objects: one tab per mod), or null. */
    public static ResourceLocation tabOfMod(String modernModId) {
        return TABS.get(modernModId);
    }

    public static void register() {
        Map<LegacyModRecord, List<Object>> byMod = new LinkedHashMap<>();
        for (Block b : ContentB173.blocks()) {
            if (b.mcc$modern != null && b.mcc$owner instanceof LegacyModRecord m && !BridgeB173.isVanillaReplacement(b.mcc$modern)) {
                byMod.computeIfAbsent(m, k -> new ArrayList<>()).add(b);
            }
        }
        for (Item i : ContentB173.items()) {
            if (i.mcc$modern != null && !(i instanceof BlockItem) && i.mcc$owner instanceof LegacyModRecord m
                    && !BridgeB173.isVanillaReplacement(i.mcc$modern)) {
                byMod.computeIfAbsent(m, k -> new ArrayList<>()).add(i);
            }
        }
        for (var e : byMod.entrySet()) {
            LegacyModRecord mod = e.getKey();
            List<Object> content = e.getValue();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("legacyconnector", mod.modernId.startsWith("b173_") ? mod.modernId : "b173_" + mod.modernId);
            TABS.put(mod.modernId, id);
            if (BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id)) continue;
            CreativeModeTab tab = CreativeModeTab.builder()
                    .title(Component.literal(mod.legacyId))
                    .icon(() -> {
                        var all = stacks(content);
                        return all.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.BOOK) : all.get(0);
                    })
                    .displayItems((params, output) -> {
                        for (ItemStack s : stacks(content)) output.accept(s);
                    })
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .build();
            LegacyRegistrar.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab);
        }
    }

    private static List<ItemStack> stacks(List<Object> content) {
        var out = net.minecraft.world.item.ItemStackLinkedSet.createTypeAndComponentsSet();
        for (Object o : content) {
            if (o instanceof Block b) {
                var item = b.mcc$modern.asItem();
                if (item == net.minecraft.world.item.Items.AIR) continue;
                out.add(new ItemStack(item));
                Item legacyItem = b.id >= 0 && b.id < Item.ITEMS.length ? Item.ITEMS[b.id] : null;
                if (legacyItem == null || !legacyItem.hasSubtypes()) continue;
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (int meta = 0; meta < 16; meta++) {
                    String sig;
                    try {
                        sig = b.getTexture(0, meta) + "," + b.getTexture(1, meta) + "," + b.getTexture(2, meta);
                    } catch (RuntimeException | LinkageError ex) {
                        break;
                    }
                    if (!seen.add(sig) || meta == 0) continue;
                    ItemStack s = new ItemStack(item);
                    s.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(LegacyB173Block.META, meta));
                    out.add(s);
                }
            } else if (o instanceof Item i) {
                out.add(new ItemStack(i.mcc$modern));
            }
        }
        return new ArrayList<>(out);
    }
}
