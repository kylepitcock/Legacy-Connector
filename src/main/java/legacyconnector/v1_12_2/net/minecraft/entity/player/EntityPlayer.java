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
}
