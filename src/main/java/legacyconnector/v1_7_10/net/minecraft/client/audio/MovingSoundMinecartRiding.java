package legacyconnector.v1_7_10.net.minecraft.client.audio;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** The sound heard inside a moving minecart, for the player riding it. */
public class MovingSoundMinecartRiding extends MovingSound {
    private final EntityPlayer field_147672_k;
    private final EntityMinecart field_147671_l;

    public MovingSoundMinecartRiding(EntityPlayer player, EntityMinecart cart) {
        super(new ResourceLocation("minecraft:minecart.inside"));
        this.field_147672_k = player;
        this.field_147671_l = cart;
        this.field_147666_i = AttenuationType.NONE;
        this.repeat = true;
        this.field_147665_h = 0;
    }

    @Override
    public void update() {
        Entity riding = Entity.mcc$get$ridingEntity(field_147672_k);
        boolean same = riding == field_147671_l || riding != null && riding.mcc$entity != null && riding.mcc$entity == field_147671_l.mcc$entity;
        if (!field_147671_l.isEntityAlive() || !field_147672_k.isRiding() || !same) {
            donePlaying = true;
            return;
        }
        double mx = Entity.mcc$get$motionX(field_147671_l), mz = Entity.mcc$get$motionZ(field_147671_l);
        float speed = (float) Math.sqrt(mx * mx + mz * mz);
        volume = speed >= 0.01F ? Math.max(0.0F, Math.min(1.0F, speed)) * 0.75F : 0.0F;
    }
}
