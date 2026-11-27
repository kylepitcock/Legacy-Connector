package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.holdersets.HolderSetType;
import net.neoforged.neoforge.registries.holdersets.ICustomHolderSet;

/** Adapts the old functional holder-set type to NeoForge's data and network codec factories. */
public final class HolderSetTypeCompat {
    private HolderSetTypeCompat() {}

    public interface Factory {
        <T> Codec<? extends ICustomHolderSet<T>> makeCodec(ResourceKey<? extends Registry<T>> registry, Codec<Holder<T>> elements, boolean forceList);
    }

    public static HolderSetType adapt(Factory factory) {
        return new HolderSetType() {
            @Override
            public <T> MapCodec<? extends ICustomHolderSet<T>> makeCodec(ResourceKey<? extends Registry<T>> registry, Codec<Holder<T>> elements, boolean forceList) {
                // Legacy custom holder-set dispatch also encoded the type and payload in a single map.
                return MapCodec.assumeMapUnsafe(factory.makeCodec(registry, elements, forceList));
            }

            @Override
            public <T> StreamCodec<RegistryFriendlyByteBuf, ? extends ICustomHolderSet<T>> makeStreamCodec(ResourceKey<? extends Registry<T>> registry) {
                return ByteBufCodecs.fromCodecWithRegistries(factory.makeCodec(registry, RegistryFixedCodec.create(registry), false));
            }
        };
    }
}