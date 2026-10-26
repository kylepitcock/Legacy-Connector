package legacyconnector.v1_7_10.net.minecraftforge.event.entity.minecart;

import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;

/** Posted at the end of every server tick of a cart, with the block it is on (Railcraft's cart hooks). */
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
