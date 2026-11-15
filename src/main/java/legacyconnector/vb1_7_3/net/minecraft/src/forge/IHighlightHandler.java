package legacyconnector.vb1_7_3.net.minecraft.src.forge;

import legacyconnector.vb1_7_3.net.minecraft.client.render.WorldRenderer;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult;

/** Forge 1.0 (client): draws the targeted block's highlight itself (a cover's placement preview); true replaces the standard outline. */
public interface IHighlightHandler {
    boolean onBlockHighlight(WorldRenderer renderer, PlayerEntity player, HitResult target, int mode, ItemStack held, float partial);
}
