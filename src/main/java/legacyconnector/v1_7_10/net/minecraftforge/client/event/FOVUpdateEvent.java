package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** The field of view is being computed: handlers (zoom items, bows) set {@link #newfov}. */
public class FOVUpdateEvent extends Event {
    public final EntityPlayer entity;
    public final float fov;
    public float newfov;

    public FOVUpdateEvent(EntityPlayer entity, float fov) {
        this.entity = entity;
        this.fov = fov;
        this.newfov = fov;
    }
}
