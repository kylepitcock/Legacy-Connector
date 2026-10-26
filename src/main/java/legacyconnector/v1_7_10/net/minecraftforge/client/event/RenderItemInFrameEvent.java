package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderItemFrame;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItemFrame;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Fired before the item in an item frame is drawn (already moved and turned into place); cancelling it skips the vanilla drawing. */
@Cancelable
public class RenderItemInFrameEvent extends Event {
    public final ItemStack item;
    public final EntityItemFrame entityItemFrame;
    public final RenderItemFrame renderer;

    public RenderItemInFrameEvent(EntityItemFrame entityItemFrame, RenderItemFrame renderer) {
        this.item = entityItemFrame == null ? null : entityItemFrame.getDisplayedItem();
        this.entityItemFrame = entityItemFrame;
        this.renderer = renderer;
    }
}
