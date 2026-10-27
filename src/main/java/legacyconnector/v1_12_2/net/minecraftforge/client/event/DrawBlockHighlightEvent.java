package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderGlobal;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;

/** The outline of the block under the crosshair is about to be drawn; cancelling hides it (handlers draw their own). */
@legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
public class DrawBlockHighlightEvent extends legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event {
    private final RenderGlobal context;
    private final EntityPlayer player;
    private final RayTraceResult target;
    private final int subID;
    private final float partialTicks;

    public DrawBlockHighlightEvent(RenderGlobal context, EntityPlayer player, RayTraceResult target, int subID, float partialTicks) {
        this.context = context;
        this.player = player;
        this.target = target;
        this.subID = subID;
        this.partialTicks = partialTicks;
    }

    public RenderGlobal getContext() {
        return context;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public RayTraceResult getTarget() {
        return target;
    }

    public int getSubID() {
        return subID;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
