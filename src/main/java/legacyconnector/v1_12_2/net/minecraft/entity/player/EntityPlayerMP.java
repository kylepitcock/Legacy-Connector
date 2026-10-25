package legacyconnector.v1_12_2.net.minecraft.entity.player;

import legacyconnector.v1_12_2.net.minecraft.world.World;

public class EntityPlayerMP extends EntityPlayer {
    public EntityPlayerMP(World world) {
        super(world);
    }

    public net.minecraft.server.level.ServerPlayer mcc$serverPlayer() {
        return (net.minecraft.server.level.ServerPlayer) mcc$entity;
    }
}
