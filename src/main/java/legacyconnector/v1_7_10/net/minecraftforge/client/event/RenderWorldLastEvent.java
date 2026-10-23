package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal;

/** Fired after the world has been drawn, before the HUD: overlays (outlines, markers, waypoints) draw in world space here. */
public class RenderWorldLastEvent extends Event {
    public final RenderGlobal context;
    public final float partialTicks;

    public RenderWorldLastEvent(RenderGlobal context, float partialTicks) {
        this.context = context;
        this.partialTicks = partialTicks;
    }
}
