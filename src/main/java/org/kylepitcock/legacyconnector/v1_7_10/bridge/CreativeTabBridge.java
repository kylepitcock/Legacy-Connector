package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
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

/** Mod-created legacy creative tabs become modern tabs; items in vanilla legacy tabs join the modern vanilla tabs. */
public final class CreativeTabBridge {
    private static final List<CreativeTabs> CUSTOM = new ArrayList<>();
    /** Modern id of the legacy mod that created each tab. */
    private static final java.util.Map<CreativeTabs, String> OWNER = new java.util.IdentityHashMap<>();

    private CreativeTabBridge() {}

    public static synchronized void onTabCreated(CreativeTabs tab) {
        CUSTOM.add(tab);
        org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710 owner = org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710.active();
        if (owner != null) OWNER.put(tab, owner.record().modernId);
    }

    /** Registers modern tabs for every legacy custom tab; runs at the end of legacy loading. */
    public static synchronized void registerTabs() {
        for (CreativeTabs tab : CUSTOM) {
            // A mod that failed to load would crash the creative screen through its tab's callbacks.
            if (org.kylepitcock.legacyconnector.core.LegacyLifecycle.isFailed(OWNER.get(tab))) continue;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_" + Ids.normalizePath(tab.getTabLabel()).replace('/', '_').replace('.', '_'));
            if (BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id)) continue;
            CreativeModeTab modern = CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + tab.getTabLabel()))
                    .icon(() -> icon(tab).copy())
                    .displayItems((params, output) -> {
                        for (net.minecraft.world.item.ItemStack s : contents(tab)) output.accept(s);
                    })
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .build();
            LegacyRegistrar.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, modern);
        }
    }

    /** One line per mod-created tab: how many stacks it lists (the creative screen shows exactly these). */
    public static synchronized String mcc$selfTest() {
        StringBuilder sb = new StringBuilder();
        for (CreativeTabs tab : CUSTOM) {
            int n;
            try {
                n = contents(tab).size();
            } catch (RuntimeException | LinkageError e) {
                sb.append("\n  ").append(tab.getTabLabel()).append(": threw ").append(e);
                continue;
            }
            sb.append("\n  ").append(tab.getTabLabel()).append(": ").append(n).append(" stacks, owner ").append(OWNER.get(tab));
        }
        return sb.toString();
    }

    /** The icon of every tab, worked out once: the creative screen asks for it every frame and the fallback lists the whole tab. */
    private static final java.util.Map<CreativeTabs, net.minecraft.world.item.ItemStack> ICONS = new java.util.IdentityHashMap<>();

    /** The legacy icon, or the tab's first item if the legacy code throws or has none. */
    private static synchronized net.minecraft.world.item.ItemStack icon(CreativeTabs tab) {
        net.minecraft.world.item.ItemStack cached = ICONS.get(tab);
        if (cached != null) return cached;
        net.minecraft.world.item.ItemStack icon = null;
        try {
            ItemStack legacy = tab.getIconItemStack();
            if (legacy != null && !legacy.mcc$modern().isEmpty()) icon = legacy.mcc$modern().copy();
        } catch (RuntimeException | LinkageError e) {
            // fall through
        }
        if (icon == null) {
            List<net.minecraft.world.item.ItemStack> items = contents(tab);
            icon = items.isEmpty() ? new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK) : items.get(0);
        }
        ICONS.put(tab, icon);
        return icon;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static List<net.minecraft.world.item.ItemStack> contents(CreativeTabs tab) {
        List raw = new ArrayList();
        try {
            tab.displayAllReleventItems(raw);
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Tabs").warn("Creative tab {} failed to list items", tab.getTabLabel(), e);
        }
        List<net.minecraft.world.item.ItemStack> out = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof ItemStack s && safeNonEmpty(s)) {
                net.minecraft.world.item.ItemStack m = s.mcc$modern().copy();
                m.setCount(1);
                if (out.stream().noneMatch(x -> net.minecraft.world.item.ItemStack.isSameItemSameComponents(x, m))) out.add(m);
            }
        }
        return out;
    }

    private static boolean safeNonEmpty(ItemStack s) {
        try {
            if (s.mcc$count() <= 0 || s.mcc$modern().isEmpty()) return false;
            return !org.kylepitcock.legacyconnector.core.LegacyLifecycle.isFailed(BuiltInRegistries.ITEM.getKey(s.mcc$modern().getItem()).getNamespace());
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** Adds legacy mod items whose legacy tab is a vanilla tab to the corresponding modern tab. */
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> key = event.getTabKey();
        for (Item item : ItemBridge.modItems()) {
            CreativeTabs tab;
            try {
                tab = item.getCreativeTab();
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            if (tab == null || tab.mcc$vanillaTab() == null) continue;
            ResourceKey<CreativeModeTab> target = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace(tab.mcc$vanillaTab()));
            if (!target.equals(key)) continue;
            List<Object> list = new ArrayList<>();
            try {
                item.getSubItems(item, tab, list);
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            for (Object o : list) {
                if (o instanceof ItemStack s && safeNonEmpty(s)) {
                    net.minecraft.world.item.ItemStack m = s.mcc$modern().copy();
                    m.setCount(1);
                    try {
                        event.accept(m);
                    } catch (IllegalStateException duplicate) {
                        // Already present (e.g. two legacy items flattened to the same modern stack).
                    }
                }
            }
        }
    }
}
