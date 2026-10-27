package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Something that moves items like a hopper (the hopper block, the hopper minecart): its world and position. */
public interface IHopper extends IInventory {
    World getWorld();

    double getXPos();

    double getYPos();

    double getZPos();
}
