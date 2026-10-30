package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockItem;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Item;

/** Converts between 1.12.2 stacks (item, count, metadata, tag) and modern ones (flattened item, count, components). */
public final class Stacks1122 {
    private Stacks1122() {}

    public static net.minecraft.world.item.ItemStack toModern(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
        Item item = stack.getItem();
        net.minecraft.world.item.Item modern = ItemBridge1122.modern(item, stack.getMetadata());
        if (modern == null) return net.minecraft.world.item.ItemStack.EMPTY;
        var out = new net.minecraft.world.item.ItemStack(modern, stack.getCount());
        if ((item.isDamageable() || item.mcc$vanillaModern != null && modern.components().has(DataComponents.MAX_DAMAGE)) && stack.getMetadata() > 0) {
            out.setDamageValue(stack.getMetadata());
        }
        // 1.12 kept a filled map's id in its metadata
        if (modern == net.minecraft.world.item.Items.FILLED_MAP) out.set(DataComponents.MAP_ID, new net.minecraft.world.level.saveddata.maps.MapId(stack.getMetadata()));
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            var data = tag.mcc$modern().copy();
            if (isPotionItem(modern)) potionToModern(data, out);
            if (!data.isEmpty()) out.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        return out;
    }

    private static boolean isPotionItem(net.minecraft.world.item.Item item) {
        return item == net.minecraft.world.item.Items.POTION || item == net.minecraft.world.item.Items.SPLASH_POTION
                || item == net.minecraft.world.item.Items.LINGERING_POTION || item == net.minecraft.world.item.Items.TIPPED_ARROW;
    }

    /** 1.12's potion tags (Potion, CustomPotionColor, CustomPotionEffects) as the modern potion contents; unknown potions stay in the tag. */
    private static void potionToModern(net.minecraft.nbt.CompoundTag data, net.minecraft.world.item.ItemStack out) {
        java.util.Optional<net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion>> potion = java.util.Optional.empty();
        if (data.contains("Potion", 8)) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(data.getString("Potion"));
            var holder = id == null ? java.util.Optional.<net.minecraft.core.Holder.Reference<net.minecraft.world.item.alchemy.Potion>>empty()
                    : net.minecraft.core.registries.BuiltInRegistries.POTION.getHolder(id);
            if (holder.isPresent()) {
                potion = java.util.Optional.of(holder.get());
                data.remove("Potion");
            }
        }
        java.util.Optional<Integer> color = java.util.Optional.empty();
        if (data.contains("CustomPotionColor", 99)) {
            color = java.util.Optional.of(data.getInt("CustomPotionColor"));
            data.remove("CustomPotionColor");
        }
        java.util.List<net.minecraft.world.effect.MobEffectInstance> effects = new java.util.ArrayList<>();
        if (data.contains("CustomPotionEffects", 9)) {
            var list = data.getList("CustomPotionEffects", 10);
            boolean all = true;
            for (int i = 0; i < list.size(); i++) {
                var legacy = legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect.readCustomPotionEffectFromNBT(new NBTTagCompound(list.getCompound(i)));
                var effect = legacy == null ? null : legacy.mcc$toModern();
                if (effect != null) effects.add(effect);
                else all = false;
            }
            if (all) data.remove("CustomPotionEffects");
        }
        if (potion.isPresent() || color.isPresent() || !effects.isEmpty()) {
            out.set(DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(potion, color, effects));
        }
    }

    /** The modern potion contents as 1.12's potion tags. */
    private static void potionFromModern(net.minecraft.world.item.alchemy.PotionContents contents, ItemStack out) {
        NBTTagCompound tag = out.hasTagCompound() ? out.getTagCompound() : new NBTTagCompound();
        contents.potion().flatMap(net.minecraft.core.Holder::unwrapKey).ifPresent(k -> tag.setString("Potion", k.location().toString()));
        contents.customColor().ifPresent(c -> tag.setInteger("CustomPotionColor", c));
        if (!contents.customEffects().isEmpty()) {
            var list = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList();
            for (var e : contents.customEffects()) {
                var legacy = legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect.mcc$fromModern(e);
                if (legacy != null) list.appendTag(legacy.writeCustomPotionEffectToNBT(new NBTTagCompound()));
            }
            if (list.tagCount() > 0) tag.setTag("CustomPotionEffects", list);
        }
        if (!tag.isEmpty()) out.setTagCompound(tag);
    }

    /** Live views by the modern stack they stand for (modern stacks compare by identity). */
    private static final java.util.Map<net.minecraft.world.item.ItemStack, ItemStack> VIEWS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    /** Views whose tag legacy code may have changed since the last flush. */
    private static final java.util.Set<ItemStack> TOUCHED = java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    /**
     * A live view of a modern stack: the same legacy object for the same modern stack, so a mod that changes the count, damage or tag
     * of a held or stored stack changes the real one (counts and damage at once, tags at the next {@link #flushViews}).
     */
    public static ItemStack view(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack view = VIEWS.get(stack);
        if (view != null && view.getItem() == legacyOf(stack.getItem())) return view;
        view = fromModern(stack);
        if (view.isEmpty()) return view;
        view.mcc$bind(stack);
        VIEWS.put(stack, view);
        return view;
    }

    /** The legacy item a modern item stands for (null when it has none). */
    private static Item legacyOf(net.minecraft.world.item.Item item) {
        if (item instanceof Legacy1122Item i) return i.legacy();
        if (item instanceof Legacy1122BlockItem b) return b.legacy();
        Object[] variant = Vanilla1122.legacyVariant(item);
        return variant != null ? (Item) variant[0] : Vanilla1122.legacyOf(item);
    }

    /** The modern stack for a legacy one: a view's own modern stack (its tag written back first), a new stack otherwise. */
    public static net.minecraft.world.item.ItemStack toModernLive(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
        var backing = stack.mcc$backing();
        if (backing != null && !backing.isEmpty()) {
            stack.mcc$commit();
            return backing;
        }
        return toModern(stack);
    }

    public static void touched(ItemStack view) {
        TOUCHED.add(view);
    }

    /** Writes the tags of the views legacy code read since the last flush back into their modern stacks. */
    public static void flushViews() {
        if (TOUCHED.isEmpty()) return;
        ItemStack[] views;
        synchronized (TOUCHED) {
            views = TOUCHED.toArray(new ItemStack[0]);
            TOUCHED.clear();
        }
        for (ItemStack v : views) {
            try {
                v.mcc$commit();
            } catch (RuntimeException e) {
                // a tag a modern component cannot hold stays with the legacy view
            }
        }
    }

    public static ItemStack fromModern(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        Item legacy;
        int meta;
        if (stack.getItem() instanceof Legacy1122Item i) {
            legacy = i.legacy();
            meta = i.meta();
        } else if (stack.getItem() instanceof Legacy1122BlockItem b) {
            legacy = b.legacy();
            meta = 0;
        } else {
            Object[] variant = Vanilla1122.legacyVariant(stack.getItem());
            legacy = variant != null ? (Item) variant[0] : Vanilla1122.legacyOf(stack.getItem());
            if (legacy == null) return ItemStack.EMPTY;
            meta = variant != null ? (Integer) variant[1] : 0;
        }
        if (legacy.isDamageable() || stack.isDamageableItem()) meta = stack.getDamageValue();
        var mapId = stack.get(DataComponents.MAP_ID);
        if (mapId != null) meta = mapId.id();
        ItemStack out = new ItemStack(legacy, stack.getCount(), meta);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) out.setTagCompound(new NBTTagCompound(data.copyTag()));
        var potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null && isPotionItem(stack.getItem())) potionFromModern(potion, out);
        return out;
    }
}
