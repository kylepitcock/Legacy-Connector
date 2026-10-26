package legacyconnector.v1_7_10.net.minecraft.client.audio;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/** The rolling sound of a moving minecart: follows the cart, louder the faster it goes. */
public class MovingSoundMinecart extends MovingSound {
    private final EntityMinecart field_147670_k;
    private float field_147669_l;

    public MovingSoundMinecart(EntityMinecart cart) {
        super(new ResourceLocation("minecraft:minecart.base"));
        this.field_147670_k = cart;
        this.repeat = true;
        this.field_147665_h = 0;
    }

    @Override
    public void update() {
        if (Entity.mcc$get$isDead(field_147670_k) || !field_147670_k.isEntityAlive()) {
            donePlaying = true;
            return;
        }
        xPosF = (float) field_147670_k.mcc$x();
        yPosF = (float) field_147670_k.mcc$y();
        zPosF = (float) field_147670_k.mcc$z();
        double mx = Entity.mcc$get$motionX(field_147670_k), mz = Entity.mcc$get$motionZ(field_147670_k);
        float speed = (float) Math.sqrt(mx * mx + mz * mz);
        if (speed >= 0.01F) {
            field_147669_l = Math.max(0.0F, Math.min(1.0F, field_147669_l + 0.0025F));
            volume = Math.max(0.0F, Math.min(0.5F, speed)) * 0.7F;
        } else {
            field_147669_l = 0.0F;
            volume = 0.0F;
        }
    }
}
