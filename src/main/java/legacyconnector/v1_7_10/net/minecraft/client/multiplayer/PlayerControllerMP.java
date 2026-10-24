package legacyconnector.v1_7_10.net.minecraft.client.multiplayer;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.world.inventory.ClickType;

/** The local player's game mode and container clicks ({@code Minecraft.playerController}). */
public class PlayerControllerMP {
    private static net.minecraft.client.multiplayer.MultiPlayerGameMode mode() {
        return net.minecraft.client.Minecraft.getInstance().gameMode;
    }

    public boolean isInCreativeMode() {
        var m = mode();
        return m != null && m.hasInfiniteItems();
    }

    public boolean isNotCreative() {
        var m = mode();
        return m != null && !m.hasInfiniteItems();
    }

    public boolean shouldDrawHUD() {
        var m = mode();
        return m != null && m.canHurtPlayer();
    }

    /** {@code mode}: 0 pickup, 1 quick move, 2 swap, 3 clone, 4 throw, 5 quick craft, 6 pickup all. */
    public ItemStack windowClick(int windowId, int slot, int button, int mode, EntityPlayer player) {
        var m = mode();
        var modern = net.minecraft.client.Minecraft.getInstance().player;
        if (m == null || modern == null) return null;
        ClickType type = mode >= 0 && mode < ClickType.values().length ? ClickType.values()[mode] : ClickType.PICKUP;
        m.handleInventoryMouseClick(windowId, slot, button, type, modern);
        return null;
    }

    public void sendSlotPacket(ItemStack stack, int slot) {
    }

    public float getBlockReachDistance() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        return player == null ? 4.5F : (float) player.blockInteractionRange();
    }
}
