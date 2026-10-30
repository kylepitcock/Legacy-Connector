package legacyconnector.v1_12_2.net.minecraft.world.storage.loot;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** Something filled from a loot table when first opened (loot chests and carts). */
public interface ILootContainer {
    ResourceLocation getLootTable();
}
