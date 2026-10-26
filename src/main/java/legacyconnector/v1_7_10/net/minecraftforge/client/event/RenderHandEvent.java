package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal;

/**
 * Fired once per frame before the first-person hand and held item are drawn; cancelling it hides them (handlers that draw
 * their own hand or held item cancel it). {@code renderPass} is the anaglyph pass: 2 without 3D anaglyph.
 */
@Cancelable
public class RenderHandEvent extends Event {
    public final RenderGlobal context;
    public final float partialTicks;
    public final int renderPass;

    public RenderHandEvent(RenderGlobal context, float partialTicks, int renderPass) {
        this.context = context;
        this.partialTicks = partialTicks;
        this.renderPass = renderPass;
    }
}
