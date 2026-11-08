package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components of the layer for legacy items: the fluid a 1.20.1 {@code FluidHandlerItemStack} keeps in the stack's NBT is a component in 1.21.1, and the
 * handler needs a type to store it under.
 */
public final class LegacyComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "legacyconnector");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT = COMPONENTS.registerComponentType("fluid_content",
            builder -> builder.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC).cacheEncoding());

    private LegacyComponents() {}

    /** First argument of the 1.21.1 {@code FluidHandlerItemStack} constructor, made from the container the 1.20.1 one took. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Supplier fluidContent(ItemStack container) {
        return FLUID_CONTENT;
    }
}
