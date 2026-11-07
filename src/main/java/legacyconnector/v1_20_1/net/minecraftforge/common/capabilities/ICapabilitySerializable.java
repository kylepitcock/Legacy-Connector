package legacyconnector.v1_20_1.net.minecraftforge.common.capabilities;

import net.minecraft.nbt.Tag;

/** Forge 1.20.1's serializable capability provider (a provider that saves itself with the entity or item it is attached to). */
public interface ICapabilitySerializable<T extends Tag> extends ICapabilityProvider, legacyconnector.v1_20_1.net.minecraftforge.common.util.INBTSerializable<T> {
}
