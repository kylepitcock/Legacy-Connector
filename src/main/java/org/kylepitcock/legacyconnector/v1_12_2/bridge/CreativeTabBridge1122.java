package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;

/** Creative tabs of 1.12.2 mods become modern tabs; items in vanilla tabs are added to the matching modern tab. */
public final class CreativeTabBridge1122 {
    /** The tabs CreativeTabs declares itself (building blocks ... search, hotbar, inventory). */
    private static final int VANILLA_TABS = 12;

    private CreativeTabBridge1122() {}

    /** Registers a modern tab for every mod-created legacy tab; runs at the end of legacy loading. */
    private static final java.util.Map<CreativeTabs, ResourceLocation> TAB_IDS = new java.util.IdentityHashMap<>();

    /** The id of the modern creative tab of a legacy custom tab, or null for vanilla tabs and tabs not registered (yet). */
    public static synchronized ResourceLocation modernTabId(CreativeTabs tab) {
        return TAB_IDS.get(tab);
    }

    public static void registerTabs() {
        List<CreativeTabs> tabs;
        synchronized (CreativeTabs.mcc$TABS) {
            tabs = new ArrayList<>(CreativeTabs.mcc$TABS);
        }
        for (CreativeTabs tab : tabs) {
            if (tab.getTabIndex() < VANILLA_TABS) continue;
            String label = tab.getTabLabel() != null ? tab.getTabLabel() : "tab" + tab.getTabIndex();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy12_" + Ids.normalizePath(label).replace('/', '_').replace('.', '_'));
            synchronized (CreativeTabBridge1122.class) {
                TAB_IDS.put(tab, id);
            }
            if (BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id)) continue;
            CreativeModeTab modern = CreativeModeTab.builder()
                    .title(title(tab, label))
                    .icon(() -> icon(tab))
                    .displayItems((params, output) -> {
                        for (var s : contents(tab)) output.accept(s);
                    })
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .build();
            LegacyRegistrar.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, modern);
        }
    }

    /** As 1.12's creative screen drew it, {@code I18n.format(tab.getTranslatedTabLabel())}: mods override it with a plain or translated name. */
    private static Component title(CreativeTabs tab, String label) {
        String key = null;
        try {
            key = tab.getTranslatedTabLabel();
        } catch (RuntimeException | LinkageError e) {
            // a client-only override that needs the game: the base key
        }
        if (key == null || key.isEmpty() || key.equals("itemGroup.")) key = "itemGroup." + label;
        return Component.translatableWithFallback(key, key.startsWith("itemGroup.") ? label : key);
    }

    private static net.minecraft.world.item.ItemStack icon(CreativeTabs tab) {
        try {
            var s = Stacks1122.toModern(tab.getTabIconItem());
            if (!s.isEmpty()) return s;
        } catch (RuntimeException | LinkageError e) {
            // fall through to the first item
        }
        var all = contents(tab);
        return all.isEmpty() ? new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK) : all.get(0);
    }

    /** Each stack once: a tab rejects duplicates (several legacy stacks can become the same modern one, Charm lists its barrel twice). */
    private static List<net.minecraft.world.item.ItemStack> contents(CreativeTabs tab) {
        var out = net.minecraft.world.item.ItemStackLinkedSet.createTypeAndComponentsSet();
        NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> raw = NonNullList.create();
        for (Item item : ItemBridge1122.modItems()) {
            try {
                if (java.util.Arrays.asList(item.getCreativeTabs()).contains(tab)) item.getSubItems(tab, raw);
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Tabs").warn("Item {} failed to list its stacks", item.getRegistryName(), e);
            }
        }
        for (var s : raw) {
            var modern = Stacks1122.toModern(s);
            if (!modern.isEmpty() && registered(modern)) out.add(modern);
        }
        return new ArrayList<>(out);
    }

    /** A stack of an item that was never registered (a dropped duplicate) must stay out of the tabs: the search trees read each item's registry key. */
    private static boolean registered(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().isBound();
    }

    private static ResourceKey<CreativeModeTab> vanillaKey(String label) {
        return switch (label) {
            case "buildingBlocks" -> CreativeModeTabs.BUILDING_BLOCKS;
            case "decorations" -> CreativeModeTabs.FUNCTIONAL_BLOCKS;
            case "redstone", "transportation" -> CreativeModeTabs.REDSTONE_BLOCKS;
            case "food", "brewing" -> CreativeModeTabs.FOOD_AND_DRINKS;
            case "tools" -> CreativeModeTabs.TOOLS_AND_UTILITIES;
            case "combat" -> CreativeModeTabs.COMBAT;
            case "misc" -> CreativeModeTabs.INGREDIENTS;
            default -> null;
        };
    }

    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        // the buckets of 1.12.2 fluids (Forge listed its universal bucket's fluids in the misc tab; modern buckets are tools)
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            for (var bucket : FluidBridge1122.generatedBuckets().keySet()) {
                var stack = new net.minecraft.world.item.ItemStack(bucket);
                if (event.getParentEntries().contains(stack)) continue;
                try {
                    event.accept(stack);
                } catch (IllegalArgumentException | IllegalStateException duplicate) {
                    // already in the tab
                }
            }
        }
        for (Item item : ItemBridge1122.modItems()) {
            CreativeTabs tab;
            try {
                tab = item.getCreativeTab();
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            if (tab == null || tab.getTabIndex() >= VANILLA_TABS || !event.getTabKey().equals(vanillaKey(tab.getTabLabel()))) continue;
            NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> list = NonNullList.create();
            try {
                item.getSubItems(tab, list);
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            for (var s : list) {
                var modern = Stacks1122.toModern(s);
                if (modern.isEmpty() || !registered(modern) || event.getParentEntries().contains(modern) || event.getSearchEntries().contains(modern)) continue;
                try {
                    event.accept(modern);
                } catch (IllegalArgumentException | IllegalStateException duplicate) {
                    // already in the tab
                }
            }
        }
    }
}
