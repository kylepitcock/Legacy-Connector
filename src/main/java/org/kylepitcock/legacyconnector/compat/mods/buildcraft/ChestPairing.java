package org.kylepitcock.legacyconnector.compat.mods.buildcraft;

import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.inventory.InventoryLargeChest;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityChest;

/**
 * MOD-SPECIFIC FIX (BuildCraft 3.x to 7.x), kept isolated so it is easy to remove: {@code Utils.getInventory(IInventory)} treated any chest
 * tile in one of the four horizontal neighbour positions as the other half of a double chest (the last one found won). 1.4.7 to 1.7.10
 * never let a third chest stand next to a double chest, a modern world does, so a pipe filling a double chest also filled the single chest
 * beside it. The loader redirects that method here ({@code BuildCraftChestPairing}); this pairs a chest only with the partner its modern
 * block state names (ChestType LEFT/RIGHT), and a single chest with nothing.
 */
public final class ChestPairing {
    private ChestPairing() {}

    public static IInventory getInventory(IInventory inv) {
        if (!(inv instanceof TileEntityChest chest) || chest.worldObj == null) return inv;
        // World.getTileEntity on the chest's own position refreshes the view and links the real partner (adjacentChest*) the way
        // 1.7.10's checkForAdjacentChests did; the fields stay empty for a single chest
        try {
            TileEntity self = chest.worldObj.getTileEntity(chest.xCoord, chest.yCoord, chest.zCoord);
            TileEntityChest c = self instanceof TileEntityChest sc ? sc : chest;
            TileEntityChest partner = c.adjacentChestXNeg != null ? c.adjacentChestXNeg : c.adjacentChestXPos != null ? c.adjacentChestXPos
                    : c.adjacentChestZNeg != null ? c.adjacentChestZNeg : c.adjacentChestZPos;
            return partner == null ? inv : new InventoryLargeChest("", inv, partner);
        } catch (RuntimeException e) {
            return inv;
        }
    }
}
