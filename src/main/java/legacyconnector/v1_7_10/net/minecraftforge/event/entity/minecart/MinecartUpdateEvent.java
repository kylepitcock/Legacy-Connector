package legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart;

import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;

/** A minecart moved on (or off) a rail this tick; x, y, z are the rail's block position. */
public class MinecartUpdateEvent extends MinecartEvent {
    public final float x;
    public final float y;
    public final float z;

    public MinecartUpdateEvent(EntityMinecart minecart, float x, float y, float z) {
        super(minecart);
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
