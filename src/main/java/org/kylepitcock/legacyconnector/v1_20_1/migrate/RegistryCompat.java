package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Registry fields of 1.20.1 libraries (Farmer's Delight's {@code ModItems.X}) were {@code RegistryObject}s; the 1.21.1 builds declare them as
 * {@code Supplier}s or {@code Holder}s. Translated code calls {@code DeferredHolder} methods on them, so a row of {@code lib-fields.tsv} converts
 * the value back with these.
 */
public final class RegistryCompat {
    private RegistryCompat() {
    }

    /** The library registers with {@code DeferredRegister}, so its suppliers are the deferred holders. */
    public static DeferredHolder<?, ?> fromSupplier(Supplier<?> supplier) {
        if (supplier instanceof DeferredHolder<?, ?> h) return h;
        throw new IllegalStateException("A library registry field is a " + (supplier == null ? "null" : supplier.getClass().getName()) + ", not a DeferredHolder");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DeferredHolder<?, ?> fromHolder(Holder<?> holder) {
        if (holder instanceof DeferredHolder<?, ?> h) return h;
        if (holder == null) return null;
        return DeferredHolder.create((net.minecraft.resources.ResourceKey) holder.unwrapKey().orElseThrow());
    }
}
