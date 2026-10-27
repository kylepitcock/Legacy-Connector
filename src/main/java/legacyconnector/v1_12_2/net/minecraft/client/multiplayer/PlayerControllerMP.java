package legacyconnector.v1_12_2.net.minecraft.client.multiplayer;

import legacyconnector.v1_12_2.net.minecraft.client.entity.EntityPlayerSP;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

/**
 * The client's game mode handler ({@code Minecraft.playerController}) as a view over the modern one: reach, block breaking state,
 * container button and slot packets, right clicks.
 */
public class PlayerControllerMP {
    public static final PlayerControllerMP mcc$INSTANCE = new PlayerControllerMP();
    /** Phantom: how far the block being broken is (0-1). */
    @Deprecated
    public float curBlockDamageMP;
    /** Phantom: whether a block is being broken. */
    @Deprecated
    public boolean isHittingBlock;
    /** Phantom: the block being broken. */
    @Deprecated
    public BlockPos currentBlock;

    private static net.minecraft.client.multiplayer.MultiPlayerGameMode mode() {
        return net.minecraft.client.Minecraft.getInstance().gameMode;
    }

    private static <T> T field(String name, T fallback) {
        var m = mode();
        if (m == null) return fallback;
        try {
            var f = net.minecraft.client.multiplayer.MultiPlayerGameMode.class.getDeclaredField(name);
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            T v = (T) f.get(m);
            return v;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return fallback;
        }
    }

    public static float mcc$get$curBlockDamageMP(PlayerControllerMP c) {
        return field("destroyProgress", 0.0F);
    }

    public static void mcc$set$curBlockDamageMP(PlayerControllerMP c, float v) {
    }

    public static boolean mcc$get$isHittingBlock(PlayerControllerMP c) {
        var m = mode();
        return m != null && m.isDestroying();
    }

    public static void mcc$set$isHittingBlock(PlayerControllerMP c, boolean v) {
    }

    public static BlockPos mcc$get$currentBlock(PlayerControllerMP c) {
        net.minecraft.core.BlockPos p = field("destroyBlockPos", null);
        return p == null ? new BlockPos(-1, -1, -1) : new BlockPos(p.getX(), p.getY(), p.getZ());
    }

    public static void mcc$set$currentBlock(PlayerControllerMP c, BlockPos v) {
    }

    public float getBlockReachDistance() {
        var p = net.minecraft.client.Minecraft.getInstance().player;
        return p == null ? 4.5F : (float) p.blockInteractionRange();
    }

    public boolean getIsHittingBlock() {
        return mcc$get$isHittingBlock(this);
    }

    /** A click on a container button (enchanting table slots, mod GUIs' buttons): the server container's enchantItem. */
    public void sendEnchantPacket(int windowID, int button) {
        var m = mode();
        if (m != null) m.handleInventoryButtonClick(windowID, button);
    }

    /** Creative mode: puts a stack into a slot of the player's inventory container. */
    public void sendSlotPacket(ItemStack itemStackIn, int slotId) {
        var m = mode();
        if (m != null) m.handleCreativeModeItemAdd(org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(itemStackIn), slotId);
    }

    public void pickItem(int index) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.getConnection() != null) mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPickItemPacket(index));
    }

    public boolean isInCreativeMode() {
        var m = mode();
        return m != null && m.hasInfiniteItems();
    }

    public boolean isSpectator() {
        var m = mode();
        return m != null && m.getPlayerMode() == net.minecraft.world.level.GameType.SPECTATOR;
    }

    public boolean gameIsSurvivalOrAdventure() {
        var m = mode();
        return m != null && m.getPlayerMode().isSurvival();
    }

    public boolean shouldDrawHUD() {
        var m = mode();
        return m != null && m.canHurtPlayer();
    }

    public boolean isNotCreative() {
        return !isInCreativeMode();
    }

    public boolean extendedReach() {
        return isInCreativeMode();
    }

    public void resetBlockRemoving() {
        var m = mode();
        if (m != null) m.stopDestroyBlock();
    }

    public boolean onPlayerDestroyBlock(BlockPos pos) {
        var m = mode();
        return m != null && m.destroyBlock(new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
    }

    public boolean clickBlock(BlockPos loc, EnumFacing face) {
        var m = mode();
        return m != null && m.startDestroyBlock(new net.minecraft.core.BlockPos(loc.getX(), loc.getY(), loc.getZ()), face.mcc$modern());
    }

    public EnumActionResult processRightClickBlock(EntityPlayerSP player, WorldClient worldIn, BlockPos pos, EnumFacing direction, Vec3d vec, EnumHand hand) {
        var m = mode();
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (m == null || mc.player == null) return EnumActionResult.PASS;
        var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(vec.x, vec.y, vec.z), direction.mcc$modern(),
                new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ()), false);
        var result = m.useItemOn(mc.player, hand == EnumHand.OFF_HAND ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        return result.consumesAction() ? EnumActionResult.SUCCESS : result == net.minecraft.world.InteractionResult.FAIL ? EnumActionResult.FAIL : EnumActionResult.PASS;
    }

    public EnumActionResult processRightClick(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, EnumHand hand) {
        var m = mode();
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (m == null || mc.player == null) return EnumActionResult.PASS;
        var result = m.useItem(mc.player, hand == EnumHand.OFF_HAND ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND);
        return result.consumesAction() ? EnumActionResult.SUCCESS : result == net.minecraft.world.InteractionResult.FAIL ? EnumActionResult.FAIL : EnumActionResult.PASS;
    }

    public void updateController() {
        var m = mode();
        if (m != null) m.tick();
    }

    // ------------------------------------------------------------------ area R2b

    /** The player's game mode. */
    public legacyconnector.v1_12_2.net.minecraft.world.GameType getCurrentGameType() {
        var m = mode();
        return m == null ? legacyconnector.v1_12_2.net.minecraft.world.GameType.NOT_SET : legacyconnector.v1_12_2.net.minecraft.world.GameType.mcc$of(m.getPlayerMode());
    }

    /** Clicks a slot of the open container (sent to the server, applied here at once); returns the stack the cursor holds afterwards. */
    public ItemStack windowClick(int windowId, int slotId, int mouseButton, legacyconnector.v1_12_2.net.minecraft.inventory.ClickType type,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        var m = mode();
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (m == null || mc.player == null || type == null) return ItemStack.EMPTY;
        m.handleInventoryMouseClick(windowId, slotId, mouseButton, type.mcc$modern(), mc.player);
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.view(mc.player.containerMenu.getCarried());
    }
}
