package org.kylepitcock.mccompat.v1_7_10.bridge;

import java.util.ArrayList;
import java.util.List;
import mccompat.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.kylepitcock.mccompat.core.Ids;
import org.kylepitcock.mccompat.core.registry.LegacyRegistrar;

/** Mod-created legacy creative tabs become modern tabs; items in vanilla legacy tabs join the modern vanilla tabs. */
public final class CreativeTabBridge {
    private static final List<CreativeTabs> CUSTOM = new ArrayList<>();

    private CreativeTabBridge() {}

    public static synchronized void onTabCreated(CreativeTabs tab) {
        CUSTOM.add(tab);
    }

    /** Registers modern tabs for every legacy custom tab; runs at the end of legacy loading. */
    public static synchronized void registerTabs() {
        for (CreativeTabs tab : CUSTOM) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("mccompat", "legacy_" + Ids.normalizePath(tab.getTabLabel()).replace('/', '_').replace('.', '_'));
            if (BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id)) continue;
            CreativeModeTab modern = CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + tab.getTabLabel()))
                    .icon(() -> {
                        ItemStack icon = tab.getIconItemStack();
                        return icon == null ? net.minecraft.world.item.ItemStack.EMPTY : icon.mcc$modern().copy();
                    })
                    .displayItems((params, output) -> {
                        for (net.minecraft.world.item.ItemStack s : contents(tab)) output.accept(s);
                    })
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .build();
            LegacyRegistrar.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, modern);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static List<net.minecraft.world.item.ItemStack> contents(CreativeTabs tab) {
        List raw = new ArrayList();
        try {
            tab.displayAllReleventItems(raw);
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("MCCompat/Tabs").warn("Creative tab {} failed to list items", tab.getTabLabel(), e);
        }
        List<net.minecraft.world.item.ItemStack> out = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof ItemStack s && s.mcc$count() > 0 && !s.mcc$modern().isEmpty()) {
                net.minecraft.world.item.ItemStack m = s.mcc$modern().copy();
                m.setCount(1);
                if (out.stream().noneMatch(x -> net.minecraft.world.item.ItemStack.isSameItemSameComponents(x, m))) out.add(m);
            }
        }
        return out;
    }

    /** Adds legacy mod items whose legacy tab is a vanilla tab to the corresponding modern tab. */
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> key = event.getTabKey();
        for (Item item : ItemBridge.modItems()) {
            CreativeTabs tab = item.getCreativeTab();
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
                if (o instanceof ItemStack s && !s.mcc$modern().isEmpty()) {
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
