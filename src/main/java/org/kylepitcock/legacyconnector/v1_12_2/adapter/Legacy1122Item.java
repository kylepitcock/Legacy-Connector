package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The modern item for one metadata variant of a 1.12.2 item. */
public class Legacy1122Item extends net.minecraft.world.item.Item {
    private final Item legacy;
    private final int meta;

    public Legacy1122Item(Item legacy, int meta, Properties properties) {
        super(properties);
        this.legacy = legacy;
        this.meta = meta;
    }

    public Item legacy() {
        return legacy;
    }

    public int meta() {
        return meta;
    }

    @Override
    public String getDescriptionId() {
        return legacy.getUnlocalizedName() + ".name";
    }

    @Override
    public Component getName(ItemStack stack) {
        try {
            var view = new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(legacy, 1, meta);
            String name = legacy.getItemStackDisplayName(view);
            if (name != null) return Component.literal(name);
        } catch (RuntimeException | LinkageError e) {
            // fall back to the translation key
        }
        return super.getName(stack);
    }
}
