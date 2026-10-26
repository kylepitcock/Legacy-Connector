package legacyconnector.v1_7_10.net.minecraft.tileentity;

/** Something that moves items like a hopper (the hopper block, the hopper minecart): its world and position. */
public interface IHopper extends legacyconnector.v1_7_10.net.minecraft.inventory.IInventory {
    legacyconnector.v1_7_10.net.minecraft.world.World getWorldObj();

    double getXPos();

    double getYPos();

    double getZPos();
}
