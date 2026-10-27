package legacyconnector.v1_12_2.net.minecraft.server.management;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.world.WorldServer;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.GameType;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * A player's game mode and block breaking on the server: a view over the modern player's {@code ServerPlayerGameMode}. One made
 * by a mod for a fake player ({@code new PlayerInteractionManager(world)}) keeps its own game mode until a player is attached.
 */
public class PlayerInteractionManager {
    public World world;
    public EntityPlayerMP player;
    private GameType mcc$gameType = GameType.NOT_SET;
    private double mcc$reach = 5.0D;

    public PlayerInteractionManager(World world) {
        this.world = world;
    }

    public PlayerInteractionManager(EntityPlayerMP player) {
        this.player = player;
        this.world = player == null ? null : player.getEntityWorld();
    }

    private net.minecraft.server.level.ServerPlayer mcc$player() {
        return player == null ? null : player.mcc$serverPlayer();
    }

    public void setGameType(GameType type) {
        var sp = mcc$player();
        if (sp != null && type != null && type != GameType.NOT_SET) sp.setGameMode(type.mcc$modern());
        else mcc$gameType = type;
    }

    public GameType getGameType() {
        var sp = mcc$player();
        return sp != null ? GameType.mcc$of(sp.gameMode.getGameModeForPlayer()) : mcc$gameType;
    }

    public boolean survivalOrAdventure() {
        GameType t = getGameType();
        return t == GameType.SURVIVAL || t == GameType.ADVENTURE;
    }

    public boolean isCreative() {
        return getGameType() == GameType.CREATIVE;
    }

    public void initializeGameType(GameType type) {
        if (getGameType() == GameType.NOT_SET) setGameType(type);
    }

    public void setWorld(legacyconnector.v1_12_2.net.minecraft.world.WorldServer serverWorld) {
        this.world = serverWorld;
        var sp = mcc$player();
        if (sp != null && serverWorld != null && serverWorld.mcc$level() instanceof net.minecraft.server.level.ServerLevel level) sp.gameMode.setLevel(level);
    }

    /** The player's block reach (the modern block interaction range attribute; 5 for a detached manager, as Forge's default). */
    public double getBlockReachDistance() {
        var sp = mcc$player();
        return sp != null ? sp.blockInteractionRange() : mcc$reach;
    }

    public void setBlockReachDistance(double distance) {
        var sp = mcc$player();
        var attribute = sp == null ? null : sp.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        if (attribute != null) attribute.setBaseValue(distance);
        else mcc$reach = distance;
    }

    /** Breaks the block as the player would (events, tool damage, drops); false when it could not. */
    public boolean tryHarvestBlock(BlockPos pos) {
        var sp = mcc$player();
        return sp != null && pos != null && sp.gameMode.destroyBlock(pos.mcc$modern());
    }

    /** Starts breaking the block (as a left click on it). */
    public void onBlockClicked(BlockPos pos, EnumFacing side) {
        var sp = mcc$player();
        if (sp == null || pos == null) return;
        sp.gameMode.handleBlockBreakAction(pos.mcc$modern(), net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                side == null ? net.minecraft.core.Direction.UP : side.mcc$modern(), sp.level().getMaxBuildHeight(), 0);
    }

    public void blockRemoving(BlockPos pos) {
        var sp = mcc$player();
        if (sp == null || pos == null) return;
        sp.gameMode.handleBlockBreakAction(pos.mcc$modern(), net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                net.minecraft.core.Direction.UP, sp.level().getMaxBuildHeight(), 0);
    }

    public void cancelDestroyingBlock() {
        var sp = mcc$player();
        if (sp == null) return;
        sp.gameMode.handleBlockBreakAction(sp.blockPosition(), net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                net.minecraft.core.Direction.UP, sp.level().getMaxBuildHeight(), 0);
    }

    /** The modern game mode ticks with the player. */
    public void updateBlockRemoving() {
    }

    /** Uses the stack on a block as the player (right click), returning 1.12's result. */
    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult processRightClickBlock(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            World worldIn, legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, BlockPos pos,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        var sp = player instanceof EntityPlayerMP mp ? mp.mcc$serverPlayer() : mcc$player();
        if (sp == null || pos == null) return legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS;
        var interaction = hand == legacyconnector.v1_12_2.net.minecraft.util.EnumHand.OFF_HAND ? net.minecraft.world.InteractionHand.OFF_HAND
                : net.minecraft.world.InteractionHand.MAIN_HAND;
        var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ),
                facing == null ? net.minecraft.core.Direction.UP : facing.mcc$modern(), pos.mcc$modern(), false);
        var result = sp.gameMode.useItemOn(sp, sp.level(), sp.getItemInHand(interaction), interaction, hit);
        return result.consumesAction() ? legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.SUCCESS
                : result == net.minecraft.world.InteractionResult.FAIL ? legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.FAIL
                : legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.PASS;
    }

    /** The view for a server player. */
    public static PlayerInteractionManager mcc$of(EntityPlayerMP player) {
        PlayerInteractionManager m = new PlayerInteractionManager(player.getEntityWorld());
        m.player = player;
        return m;
    }

    public EnumActionResult processRightClick(EntityPlayer player, World world, ItemStack stack, EnumHand hand) {
        var sp = player != null && player.mcc$entity instanceof net.minecraft.server.level.ServerPlayer p ? p : mcc$player();
        if (sp == null || world == null || world.mcc$level() == null) return EnumActionResult.PASS;
        return EnumActionResult.mcc$of(sp.gameMode.useItem(sp, world.mcc$level(), Stacks1122.toModernLive(stack), hand == EnumHand.OFF_HAND
                ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND));
    }

    public EnumActionResult processRightClickBlock(EntityPlayer player, World world, ItemStack stack, EnumHand hand, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ) {
        var sp = player != null && player.mcc$entity instanceof net.minecraft.server.level.ServerPlayer p ? p : mcc$player();
        if (sp == null || world == null || world.mcc$level() == null || pos == null) return EnumActionResult.PASS;
        var direction = facing == null ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.byName(facing.getName());
        var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ),
                direction, pos.mcc$modern(), false);
        return EnumActionResult.mcc$of(sp.gameMode.useItemOn(sp, world.mcc$level(), Stacks1122.toModernLive(stack), hand == EnumHand.OFF_HAND
                ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND, hit));
    }
}
