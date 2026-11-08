package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;

/** JEI 15 (1.20.1) calls against JEI 19 (1.21.1): fluids are holders with component patches, item NBT is the custom data component. */
public final class JeiCompat {
    private JeiCompat() {
    }

    /** {@code IPlatformFluidHelper.create(Fluid, long)} took the fluid; the new one takes its holder. */
    public static Object createFluid(IPlatformFluidHelper<?> helper, Fluid fluid, long amount) {
        return helper.create(fluid.builtInRegistryHolder(), amount);
    }

    /** The tag version: the NBT becomes the stack's custom data. */
    public static Object createFluid(IPlatformFluidHelper<?> helper, Fluid fluid, long amount, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return helper.create(fluid.builtInRegistryHolder(), amount);
        DataComponentPatch patch = DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy())).build();
        return helper.create(fluid.builtInRegistryHolder(), amount, patch);
    }

    /** {@code ISubtypeRegistration.useNbtForSubtypes(Item...)}: the subtype of these items is their NBT, i.e. the custom data component. */
    public static void useNbtForSubtypes(ISubtypeRegistration registration, Item[] items) {
        for (Item item : items) {
            registration.registerSubtypeInterpreter(item, new ISubtypeInterpreter<ItemStack>() {
                @Override
                public Object getSubtypeData(ItemStack stack, UidContext context) {
                    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                    return data == null || data.isEmpty() ? null : data;
                }

                @Override
                public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                    return data == null || data.isEmpty() ? "" : data.copyTag().toString();
                }
            });
        }
    }
}
