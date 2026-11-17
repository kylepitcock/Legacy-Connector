package legacyconnector.v1_6_4.net.minecraft.src.forge;

public interface IHighlightHandler {
    /** Called before the block outline is drawn; true means the handler drew its own and the game's outline is skipped. */
    boolean onBlockHighlight(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderGlobal renderer, legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition target, int subID, legacyconnector.v1_7_10.net.minecraft.item.ItemStack currentItem, float partialTicks);
}
