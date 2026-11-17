package legacyconnector.v1_12_2.net.minecraftforge.common.crafting;

import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCraftResult;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;

/** A container of a crafting grid that tells recipe mods its grid and output slots (Forge 1.12.2; implemented by mods' crafting containers). */
public interface IRecipeContainer {
    InventoryCrafting getCraftMatrix();

    InventoryCraftResult getCraftResult();
}
