package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderItemFrame;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItemFrame;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** The item in an item frame is about to be drawn; cancelling skips it (handlers draw it themselves). */
@legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
public class RenderItemInFrameEvent extends legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event {
    private final ItemStack item;
    private final EntityItemFrame entityItemFrame;
    private final RenderItemFrame renderer;

    public RenderItemInFrameEvent(EntityItemFrame itemFrame, RenderItemFrame renderItemFrame) {
        this.item = itemFrame == null ? ItemStack.EMPTY : itemFrame.getDisplayedItem();
        this.entityItemFrame = itemFrame;
        this.renderer = renderItemFrame;
    }

    public ItemStack getItem() {
        return item;
    }

    public EntityItemFrame getEntityItemFrame() {
        return entityItemFrame;
    }

    public RenderItemFrame getRenderer() {
        return renderer;
    }
}
