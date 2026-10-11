package org.kylepitcock.mccompat.core.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data components that carry legacy state no modern component expresses. */
public final class CompatComponents {
    public static final DeferredRegister<DataComponentType<?>> REGISTER = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "retrofit");

    /**
     * Legacy item metadata for subtypes that were not flattened into their own modern item (for example values a
     * mod only creates at runtime). Persisted and synced, so such stacks survive saving and network transfer.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LEGACY_META = REGISTER.register("legacy_meta",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    private CompatComponents() {}

    public static DataComponentType<Integer> legacyMeta() {
        return LEGACY_META.get();
    }
}
