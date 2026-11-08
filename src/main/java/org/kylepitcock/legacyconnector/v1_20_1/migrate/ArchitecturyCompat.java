package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.lang.reflect.Method;
import net.minecraft.world.level.storage.loot.LootPool;

/**
 * Architectury 9 (1.20.1) calls that changed in Architectury 13 (1.21.1). Architectury is not a compile dependency of the layer: its types arrive
 * as {@code Object} and are reached through reflection (the rewritten call sites pass the original receiver).
 */
public final class ArchitecturyCompat {
    private ArchitecturyCompat() {
    }

    /** {@code LootTableModificationContext.addPool(LootPool)}: 13 takes the builder. */
    public static void addPool(Object context, LootPool pool) {
        try {
            Method m = context.getClass().getMethod("addPool", LootPool.Builder.class);
            m.setAccessible(true);
            m.invoke(context, new PrebuiltPool(pool));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Architectury LootTableModificationContext.addPool is not available", e);
        }
    }

    /** {@code FluidStack.getTag()}: the stack's NBT is the custom data component of its patch (null without one). */
    public static net.minecraft.nbt.CompoundTag fluidGetTag(Object stack) {
        try {
            var patch = (net.minecraft.core.component.DataComponentPatch) stack.getClass().getMethod("getPatch").invoke(stack);
            var data = patch.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            return data == null || data.isEmpty() ? null : data.get().copyTag();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Architectury FluidStack.getPatch is not available", e);
        }
    }

    /** The component patch that stands for a fluid stack's NBT (the third argument of {@code FluidStack.create}). */
    public static net.minecraft.core.component.DataComponentPatch fluidPatch(net.minecraft.nbt.CompoundTag tag) {
        var builder = net.minecraft.core.component.DataComponentPatch.builder();
        if (tag != null && !tag.isEmpty()) builder.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag.copy()));
        return builder.build();
    }

    /** A pool builder that hands out a pool built earlier. */
    private static final class PrebuiltPool extends LootPool.Builder {
        private final LootPool pool;

        PrebuiltPool(LootPool pool) {
            this.pool = pool;
        }

        @Override
        public LootPool build() {
            return pool;
        }
    }
}
