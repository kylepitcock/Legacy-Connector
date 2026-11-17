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

    /** Per-tick sync of the selected hotbar slot with the server; the modern game mode does this itself. */
    public void updateController() {
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

    /** True in survival and adventure mode (not creative, not spectator). */
    public boolean gameIsSurvivalOrAdventure() {
        var m = mode();
        return m != null && m.getPlayerMode().isSurvival();
    }

    /** Tells the server which hotbar slot is selected (the modern game does it every tick; mods call it right after changing the slot). */
    public void syncCurrentPlayItem() {
        var m = mode();
        if (m != null) m.ensureHasSentCarriedItem();
    }

    /** A creative player's dropped stack: the creative inventory packet for slot -1 (the server spawns the item). */
    public void sendPacketDropItem(ItemStack stack) {
        var m = mode();
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (m == null || connection == null || stack == null || !m.hasInfiniteItems()) return;
        connection.send(new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(-1, ItemStack.mcc$toModern(stack)));
    }

    /** A left click on a block in creative mode: breaks it at once (as the game's creative click does). */
    public static void clickBlockCreative(legacyconnector.v1_7_10.net.minecraft.client.Minecraft mc, PlayerControllerMP controller, int x, int y, int z,
            int side) {
        controller.clickBlock(x, y, z, side);
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

    /** Breaks the block at (x, y, z) now (as the client does when the mining finishes); true when it was destroyed. */
    public boolean onPlayerDestroyBlock(int x, int y, int z, int side) {
        var m = mode();
        return m != null && m.destroyBlock(new net.minecraft.core.BlockPos(x, y, z));
    }

    /** Interacts with an entity as a right click does (the item in hand, main hand); true when the entity took it. */
    public boolean interactWithEntitySendPacket(EntityPlayer player, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        var m = mode();
        var local = local();
        if (m == null || local == null || entity == null || entity.mcc$entity() == null) return false;
        return m.interact(local, entity.mcc$entity(), net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction();
    }

    /** Releases the item in use (a drawn bow fires). */
    public void onStoppedUsingItem(EntityPlayer player) {
        var m = mode();
        var local = local();
        if (m != null && local != null) m.releaseUsingItem(local);
    }

    // Members125 batch 1

    /** 1.2.5 {@code closeMenu(windowId, player)}: closes the open container on the client and tells the server. */
    public void mcc125$closeMenu(int windowId, EntityPlayer player) {
        var local = local();
        if (local != null) local.closeContainer();
    }

    /** Phantom field {@code creative} of 1.2.5's controller: whether the local player is in creative mode. */
    public static boolean mcc$get$mcc125$creative(PlayerControllerMP c) {
        return c.isInCreativeMode();
    }

    public static void mcc$set$mcc125$creative(PlayerControllerMP c, boolean v) {
        c.mcc125$setGameMode(v);
    }

    /** 1.2.5 {@code setGameMode(creative)}: switches the player between creative and survival (through the server's gamemode command, as the game now keeps the mode on the server). */
    public void mcc125$setGameMode(boolean creative) {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection != null && creative != isInCreativeMode()) connection.sendCommand(creative ? "gamemode creative" : "gamemode survival");
    }

    // Members125 batch 2

    /** 1.2.5's constructor (the connection handler came with 1.3): mods subclass the controller and construct it like the game did. */
    public PlayerControllerMP(legacyconnector.v1_7_10.net.minecraft.client.Minecraft mc) {
        this(mc, null);
    }
}
