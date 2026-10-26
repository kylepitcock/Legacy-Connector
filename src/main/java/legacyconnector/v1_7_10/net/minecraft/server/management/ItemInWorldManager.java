package legacyconnector.v1_7_10.net.minecraft.server.management;

import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The per-player block breaking/interaction manager (1.7.10); modern code keeps this in the player's
 * {@link net.minecraft.server.level.ServerPlayerGameMode}, which every method here drives. Fake players (CoFH's
 * activator, Thaumcraft golems) break blocks by calling {@link #onBlockClicked} and then {@link #updateBlockRemoving}
 * every tick, exactly as the modern game mode is ticked.
 */
public class ItemInWorldManager {
    public World theWorld;
    private final ServerPlayer mcc$player;

    public ItemInWorldManager(World world) {
        this.theWorld = world;
        this.mcc$player = null;
    }

    public ItemInWorldManager(World world, ServerPlayer player) {
        this.theWorld = world;
        this.mcc$player = player;
    }

    public double getBlockReachDistance() {
        return mcc$player != null ? mcc$player.blockInteractionRange() : 5.0D;
    }

    public void setBlockReachDistance(double distance) {
    }

    public boolean isCreative() {
        return mcc$player != null && mcc$player.isCreative();
    }

    public boolean tryHarvestBlock(int x, int y, int z) {
        if (mcc$player == null) return false;
        return mcc$player.gameMode.destroyBlock(new BlockPos(x, y, z));
    }

    public legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType getGameType() {
        if (mcc$player == null) return legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType.SURVIVAL;
        return legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType.getByID(mcc$player.gameMode.getGameModeForPlayer().getId());
    }

    /** Changes the player's game mode (and with it the capabilities the client is told about). */
    public void setGameType(legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType type) {
        if (mcc$player == null || type == null || type.getID() < 0) return;
        mcc$player.setGameMode(net.minecraft.world.level.GameType.byId(type.getID()));
    }

    public void initializeGameType(legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType type) {
        if (type != null && type != legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType.NOT_SET) setGameType(type);
    }

    public void setWorld(legacyconnector.v1_7_10.net.minecraft.world.WorldServer world) {
        this.theWorld = world;
    }

    private int mcc$buildHeight() {
        return mcc$player.serverLevel().getMaxBuildHeight();
    }

    /** Advances block breaking by one tick (the progress the player made since {@link #onBlockClicked}). */
    public void updateBlockRemoving() {
        if (mcc$player != null) mcc$player.gameMode.tick();
    }

    /** The player starts hitting a block: creative players break it at once, others start breaking it. */
    public void onBlockClicked(int x, int y, int z, int side) {
        if (mcc$player == null) return;
        mcc$player.gameMode.handleBlockBreakAction(new BlockPos(x, y, z), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                Direction.from3DDataValue(side), mcc$buildHeight(), 0);
    }

    /** Stops breaking the block and clears its crack overlay. */
    public void cancelDestroyingBlock(int x, int y, int z) {
        if (mcc$player == null) return;
        mcc$player.gameMode.handleBlockBreakAction(new BlockPos(x, y, z), ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                Direction.UP, mcc$buildHeight(), 0);
    }

    /** Clears the crack overlay and harvests the block without the breaking-time checks. */
    public void uncheckedTryHarvestBlock(int x, int y, int z) {
        if (mcc$player == null) return;
        BlockPos pos = new BlockPos(x, y, z);
        mcc$player.serverLevel().destroyBlockProgress(mcc$player.getId(), pos, -1);
        mcc$player.gameMode.destroyBlock(pos);
    }

    private static ServerPlayer mcc$serverPlayer(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, ServerPlayer fallback) {
        if (player != null && player.mcc$player() instanceof ServerPlayer sp) return sp;
        return fallback;
    }

    /**
     * Runs {@code use} with {@code stack} in the player's main hand (modern interaction reads the stack from the hand,
     * legacy callers pass it in); restores the hand afterwards when it held something else.
     */
    private static <T> T mcc$withHeld(ServerPlayer sp, legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, java.util.function.Supplier<T> use) {
        net.minecraft.world.item.ItemStack modern = stack == null ? net.minecraft.world.item.ItemStack.EMPTY : stack.mcc$modern();
        net.minecraft.world.item.ItemStack held = sp.getMainHandItem();
        if (held == modern) return use.get();
        sp.setItemInHand(InteractionHand.MAIN_HAND, modern);
        try {
            return use.get();
        } finally {
            sp.setItemInHand(InteractionHand.MAIN_HAND, held);
        }
    }

    /** Right-click with an item in the air; true when the item did something. */
    public boolean tryUseItem(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, World world,
            legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
        ServerPlayer sp = mcc$serverPlayer(player, mcc$player);
        if (sp == null || stack == null) return false;
        net.minecraft.world.level.Level level = world == null ? sp.level() : world.mcc$fullLevel();
        return mcc$withHeld(sp, stack, () -> sp.gameMode.useItem(sp, level, sp.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction());
    }

    /**
     * Right-click on a block: activates it, or uses the item on it when the block does nothing (placing blocks, using
     * hoes and buckets). True when either happened.
     */
    public boolean activateBlockOrUseItem(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, World world,
            legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        ServerPlayer sp = mcc$serverPlayer(player, mcc$player);
        if (sp == null) return false;
        net.minecraft.world.level.Level level = world == null ? sp.level() : world.mcc$fullLevel();
        BlockPos pos = new BlockPos(x, y, z);
        BlockHitResult hit = new BlockHitResult(new Vec3(x + hitX, y + hitY, z + hitZ), Direction.from3DDataValue(side), pos, false);
        return mcc$withHeld(sp, stack, () -> sp.gameMode.useItemOn(sp, level, sp.getMainHandItem(), InteractionHand.MAIN_HAND, hit).consumesAction());
    }
}
