package legacyconnector.v1_7_10.net.minecraft.client.multiplayer;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.world.inventory.ClickType;

/** The local player's game mode and container clicks ({@code Minecraft.playerController}). */
public class PlayerControllerMP {
    public PlayerControllerMP() {
    }

    /** Mods subclass the controller (longer reach) and construct it like the game did. */
    public PlayerControllerMP(legacyconnector.v1_7_10.net.minecraft.client.Minecraft mc, legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient handler) {
    }

    private static net.minecraft.client.multiplayer.MultiPlayerGameMode mode() {
        return net.minecraft.client.Minecraft.getInstance().gameMode;
    }

    private static net.minecraft.client.player.LocalPlayer local() {
        return net.minecraft.client.Minecraft.getInstance().player;
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

    /** Never true in 1.7.10 (the flag of a debug mode that did not ship). */
    public boolean enableEverythingIsScrewedUpMode() {
        return false;
    }

    /** Presses a button of the open container (the enchanting table's enchantments, a mod GUI's buttons). */
    public void sendEnchantPacket(int windowId, int button) {
        var m = mode();
        if (m != null) m.handleInventoryButtonClick(windowId, button);
    }

    /** Sets the local game mode (what the client shows and allows; the server's game mode is unchanged). */
    public void setGameType(legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType type) {
        var m = mode();
        if (m == null || type == null || type.getID() < 0) return;
        m.setLocalMode(net.minecraft.world.level.GameType.byId(type.getID()));
    }

    /** Creative players reach further (5 blocks instead of 4.5). */
    public boolean extendedReach() {
        var m = mode();
        return m != null && m.getPlayerMode().isCreative();
    }

    /** Attacks the entity as a left click would. */
    public void attackEntity(EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.entity.Entity target) {
        var m = mode();
        var local = local();
        if (m == null || local == null || target == null || target.mcc$entity() == null) return;
        m.attack(local, target.mcc$entity());
    }

    /** Right-clicks the block at (x, y, z) on {@code side} with the held item; true when something happened. */
    public boolean onPlayerRightClick(EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.world.World world, ItemStack stack, int x, int y, int z,
            int side, legacyconnector.v1_7_10.net.minecraft.util.Vec3 hit) {
        var m = mode();
        var local = local();
        if (m == null || local == null) return false;
        var pos = new net.minecraft.core.BlockPos(x, y, z);
        var at = hit == null ? net.minecraft.world.phys.Vec3.atCenterOf(pos) : new net.minecraft.world.phys.Vec3(hit.xCoord, hit.yCoord, hit.zCoord);
        var face = net.minecraft.core.Direction.from3DDataValue(side);
        return m.useItemOn(local, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(at, face, pos, false)).consumesAction();
    }

    /** Uses the held item in the air; true when it did something. */
    public boolean sendUseItem(EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.world.World world, ItemStack stack) {
        var m = mode();
        var local = local();
        if (m == null || local == null) return false;
        return m.useItem(local, net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction();
    }

    /**
     * Cancels the block the player is breaking. Mods whose items take the left click call it every frame so holding the
     * button never breaks blocks (Portal Gun shoots its blue portal with it).
     */
    public void resetBlockRemoving() {
        var m = mode();
        if (m != null && m.isDestroying()) m.stopDestroyBlock();
    }

    /** True while the player is breaking a block. */
    public boolean getIsHittingBlock() {
        var m = mode();
        return m != null && m.isDestroying();
    }

    /** Starts breaking the block at (x, y, z) from {@code side}, as a left click would. */
    public void clickBlock(int x, int y, int z, int side) {
        var m = mode();
        if (m != null) m.startDestroyBlock(new net.minecraft.core.BlockPos(x, y, z), net.minecraft.core.Direction.from3DDataValue(side));
    }

    /** Continues breaking the block at (x, y, z) (called every tick while the button is held). */
    public void onPlayerDamageBlock(int x, int y, int z, int side) {
        var m = mode();
        if (m != null) m.continueDestroyBlock(new net.minecraft.core.BlockPos(x, y, z), net.minecraft.core.Direction.from3DDataValue(side));
    }

    /** Releases the item in use (a drawn bow fires). */
    public void onStoppedUsingItem(EntityPlayer player) {
        var m = mode();
        var local = local();
        if (m != null && local != null) m.releaseUsingItem(local);
    }
}
