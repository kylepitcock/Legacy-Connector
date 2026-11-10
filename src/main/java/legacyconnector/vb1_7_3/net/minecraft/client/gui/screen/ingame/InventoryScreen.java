package legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;

/** Typed view of the player's native inventory for original legacy screen callbacks. */
public class InventoryScreen extends HandledScreen {
    public InventoryScreen(PlayerEntity player) { super(null); }
    @Override protected void drawBackground(float delta) {}
}
