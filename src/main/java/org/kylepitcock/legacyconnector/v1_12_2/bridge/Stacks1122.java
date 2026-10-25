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
        if (item.isDamageable() && stack.getMetadata() > 0) out.setDamageValue(stack.getMetadata());
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) out.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.mcc$modern().copy()));
        return out;
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
            legacy = Vanilla1122.legacyOf(stack.getItem());
            if (legacy == null) return ItemStack.EMPTY;
            meta = 0;
        }
        if (legacy.isDamageable()) meta = stack.getDamageValue();
        ItemStack out = new ItemStack(legacy, stack.getCount(), meta);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) out.setTagCompound(new NBTTagCompound(data.copyTag()));
        return out;
    }
}
