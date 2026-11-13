package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * The bucket of a 1.12.2 fluid that had Forge's universal bucket (or a fluid block) and no bucket item of its own mod: a plain modern
 * {@link BucketItem} (it places and picks up the fluid the vanilla way), named the way Forge's universal bucket was ("%s Bucket").
 */
public class Legacy1122BucketItem extends BucketItem {
    public Legacy1122BucketItem(Fluid content, Properties properties) {
        super(content, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatableWithFallback("item.forge.bucketFilled.name", "%s Bucket", content.getFluidType().getDescription());
    }
}
