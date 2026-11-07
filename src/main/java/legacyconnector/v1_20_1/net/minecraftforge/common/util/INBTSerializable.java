package legacyconnector.v1_20_1.net.minecraftforge.common.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

/**
 * Forge 1.20.1's {@code INBTSerializable}: {@code serializeNBT()} / {@code deserializeNBT(tag)}. NeoForge 1.21.1's version takes a registry provider; this
 * one keeps the old methods abstract (what mod classes implement and call) and answers the new ones through them, so NeoForge code can save the object too.
 */
public interface INBTSerializable<T extends Tag> extends net.neoforged.neoforge.common.util.INBTSerializable<T> {
    T serializeNBT();

    void deserializeNBT(T nbt);

    @Override
    default T serializeNBT(HolderLookup.Provider provider) {
        return serializeNBT();
    }

    @Override
    default void deserializeNBT(HolderLookup.Provider provider, T nbt) {
        deserializeNBT(nbt);
    }
}
