package org.kylepitcock.legacyconnector.mixin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_20_1.Forge1201Runtime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.20.1 read a name the defaulted registries (blocks, items, entity types...) do not have as the registry's default (air for a block): a pack file
 * naming a block that is not there (a typo, a mod that is not installed) still loaded. 1.21.1's by-name codec fails, and one failing worldgen file stops
 * the whole registry load. While 1.20.1 mods are loaded, the defaulted registries read names the 1.20.1 way. Without 1.20.1 mods nothing changes.
 */
@Mixin(Registry.class)
public interface RegistryLegacyNamesMixin {
    @Inject(method = "byNameCodec", at = @At("HEAD"), cancellable = true, require = 0)
    private <T> void legacyconnector$defaultedNames(CallbackInfoReturnable<Codec<T>> cir) {
        @SuppressWarnings("unchecked")
        Registry<T> self = (Registry<T>) (Object) this;
        if (!(self instanceof DefaultedMappedRegistry<T> defaulted) || Forge1201Runtime.modIds().isEmpty()) return;
        cir.setReturnValue(ResourceLocation.CODEC.flatXmap(
                id -> DataResult.success(defaulted.get(id)),
                value -> {
                    ResourceLocation key = defaulted.getKey(value);
                    return key == null ? DataResult.error(() -> "Unknown registry element " + value) : DataResult.success(key);
                }));
    }

    /** The same for holders (potions, 1.20.1's {@code set_nbt "Potion"} names): an unknown name is the default's holder. */
    @Inject(method = "holderByNameCodec", at = @At("HEAD"), cancellable = true, require = 0)
    private <T> void legacyconnector$defaultedHolderNames(CallbackInfoReturnable<Codec<Holder<T>>> cir) {
        @SuppressWarnings("unchecked")
        Registry<T> self = (Registry<T>) (Object) this;
        if (!(self instanceof DefaultedMappedRegistry<T> defaulted) || Forge1201Runtime.modIds().isEmpty()) return;
        cir.setReturnValue(ResourceLocation.CODEC.flatXmap(
                id -> DataResult.success(defaulted.getHolder(id).<Holder<T>>map(h -> h).orElseGet(() -> defaulted.getHolderOrThrow(ResourceKey.create(defaulted.key(), defaulted.getDefaultKey())))),
                holder -> holder.unwrapKey().map(k -> DataResult.success(k.location())).orElseGet(() -> DataResult.error(() -> "Unregistered holder " + holder))));
    }
}
