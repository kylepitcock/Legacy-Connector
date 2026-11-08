package org.kylepitcock.legacyconnector.v1_20_1.bridge;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** Registry-aware ItemStack wire codec used by translated Forge 1.20.1 packet adapters. */
public final class ItemStackCodec1201 {
    private ItemStackCodec1201() {}

    /** Writes an optional stack; null legacy slots are normalized to ItemStack.EMPTY. */
    public static FriendlyByteBuf writeOptional(FriendlyByteBuf buffer, ItemStack stack) {
        RegistryFriendlyByteBuf registryBuffer = requireRegistryBuffer(buffer);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(registryBuffer, stack == null ? ItemStack.EMPTY : stack);
        // Match FriendlyByteBuf#writeItem's fluent return so a transformed call keeps its stack shape.
        return buffer;
    }

    /** Reads an optional stack using the registry view attached to the PLAY buffer. */
    public static ItemStack readOptional(FriendlyByteBuf buffer) {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(requireRegistryBuffer(buffer));
    }

    private static RegistryFriendlyByteBuf requireRegistryBuffer(FriendlyByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        if (!(buffer instanceof RegistryFriendlyByteBuf registryBuffer)) {
            throw new IllegalArgumentException(
                    "Forge 1.20.1 ItemStack packets require the registry-aware PLAY buffer");
        }
        return registryBuffer;
    }
}
