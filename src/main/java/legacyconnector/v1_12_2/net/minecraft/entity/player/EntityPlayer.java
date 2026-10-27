package legacyconnector.v1_12_2.net.minecraft.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.world.World;

public abstract class EntityPlayer extends EntityLivingBase {
    public EntityPlayer(World world) {
        super(world);
    }

    public net.minecraft.world.entity.player.Player mcc$player() {
        return (net.minecraft.world.entity.player.Player) mcc$entity;
    }

    /** FML: opens GUI {@code id} of {@code mod}'s {@code IGuiHandler} (a container GUI from the server, a client-only one on the client). */
    public void openGui(Object mod, int id, World world, int x, int y, int z) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122.openGui(this, mod, id, world, x, y, z);
    }
}
