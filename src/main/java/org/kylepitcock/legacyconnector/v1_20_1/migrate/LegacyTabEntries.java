package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.MutableHashedLinkedMap;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * {@code BuildCreativeModeTabContentsEvent.getEntries()} of Forge 1.20.1: a mutable ordered map from stack to visibility. NeoForge 1.21.1 dropped
 * it for insert methods on the event and two entry sets; this view keeps the old type and forwards every change to the event.
 */
public final class LegacyTabEntries extends MutableHashedLinkedMap<ItemStack, TabVisibility> {
    private final BuildCreativeModeTabContentsEvent event;

    public LegacyTabEntries(BuildCreativeModeTabContentsEvent event) {
        this.event = event;
    }

    private TabVisibility visibility(ItemStack stack) {
        boolean parent = event.getParentEntries().contains(stack);
        boolean search = event.getSearchEntries().contains(stack);
        if (parent && search) return TabVisibility.PARENT_AND_SEARCH_TABS;
        return parent ? TabVisibility.PARENT_TAB_ONLY : search ? TabVisibility.SEARCH_TAB_ONLY : null;
    }

    @Override
    public TabVisibility put(ItemStack key, TabVisibility value) {
        TabVisibility old = visibility(key);
        if (old == null) event.accept(key, value);
        return old;
    }

    @Override
    public TabVisibility putFirst(ItemStack key, TabVisibility value) {
        TabVisibility old = visibility(key);
        if (old == null) event.insertFirst(key, value);
        return old;
    }

    @Override
    public TabVisibility putAfter(ItemStack existing, ItemStack key, TabVisibility value) {
        TabVisibility old = visibility(key);
        if (old == null) event.insertAfter(existing, key, value);
        return old;
    }

    @Override
    public TabVisibility putBefore(ItemStack existing, ItemStack key, TabVisibility value) {
        TabVisibility old = visibility(key);
        if (old == null) event.insertBefore(existing, key, value);
        return old;
    }

    @Override
    public TabVisibility remove(ItemStack key) {
        TabVisibility old = visibility(key);
        if (old != null) event.remove(key, old);
        return old;
    }

    @Override
    public boolean contains(ItemStack key) {
        return visibility(key) != null;
    }

    @Override
    public TabVisibility get(ItemStack key) {
        return visibility(key);
    }

    @Override
    public boolean isEmpty() {
        return event.getParentEntries().isEmpty() && event.getSearchEntries().isEmpty();
    }

    @Override
    public Iterator<Map.Entry<ItemStack, TabVisibility>> iterator() {
        List<Map.Entry<ItemStack, TabVisibility>> out = new ArrayList<>();
        for (ItemStack s : event.getParentEntries()) out.add(Map.entry(s, visibility(s)));
        for (ItemStack s : event.getSearchEntries()) if (!event.getParentEntries().contains(s)) out.add(Map.entry(s, TabVisibility.SEARCH_TAB_ONLY));
        return out.iterator();
    }
}
