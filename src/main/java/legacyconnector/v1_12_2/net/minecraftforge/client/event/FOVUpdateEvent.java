package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** The field of view modifier of the local player (sprinting, bows, speed effects); handlers change {@code newfov}. */
public class FOVUpdateEvent extends Event {
    private final EntityPlayer entity;
    private final float fov;
    private float newfov;

    public FOVUpdateEvent(EntityPlayer entity, float fov) {
        this.entity = entity;
        this.fov = fov;
        this.newfov = fov;
    }

    public EntityPlayer getEntity() {
        return entity;
    }

    public float getFov() {
        return fov;
    }

    public float getNewfov() {
        return newfov;
    }

    public void setNewfov(float newfov) {
        this.newfov = newfov;
    }
}
