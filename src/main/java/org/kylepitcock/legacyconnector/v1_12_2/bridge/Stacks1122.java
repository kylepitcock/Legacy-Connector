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
        ItemStack out = new ItemStack(legacy, stack.getCount(), meta);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) out.setTagCompound(new NBTTagCompound(data.copyTag()));
        var potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null && isPotionItem(stack.getItem())) potionFromModern(potion, out);
        return out;
    }
}
