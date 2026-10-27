package legacyconnector.v1_12_2.net.minecraft.server.management;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.GameType;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.WorldServer;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * 1.12.2 player interaction manager ({@code EntityPlayerMP.interactionManager}): a view of the server player's modern game mode (game
 * type, block breaking, item use). Forge's block reach is the modern block interaction range attribute.
 */
public class PlayerInteractionManager {
    public World world;
    public EntityPlayerMP player;
    private GameType mcc$localType = GameType.NOT_SET;

    public PlayerInteractionManager(World world) {
        this.world = world;
    }

    /** The view for a server player. */
    public static PlayerInteractionManager mcc$of(EntityPlayerMP player) {
        PlayerInteractionManager m = new PlayerInteractionManager(player.getEntityWorld());
        m.player = player;
        return m;
    }

    private net.minecraft.server.level.ServerPlayer mcc$player() {
        return player != null && player.mcc$entity instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null;
    }

    public void setGameType(GameType type) {
        var sp = mcc$player();
        if (sp != null && type != null && type != GameType.NOT_SET) sp.setGameMode(type.mcc$modern());
        else mcc$localType = type;
    }

    public GameType getGameType() {
        var sp = mcc$player();
        return sp != null ? GameType.mcc$of(sp.gameMode.getGameModeForPlayer()) : mcc$localType;
    }

    public boolean survivalOrAdventure() {
        var sp = mcc$player();
        return sp != null ? sp.gameMode.isSurvival() : mcc$localType == GameType.SURVIVAL || mcc$localType == GameType.ADVENTURE;
    }

    public boolean isCreative() {
        var sp = mcc$player();
        return sp != null ? sp.gameMode.isCreative() : mcc$localType == GameType.CREATIVE;
    }

    /** Sets the world's default game type when the player has none yet. */
    public void initializeGameType(GameType type) {
        if (getGameType() == GameType.NOT_SET) setGameType(type);
    }

    public void updateBlockRemoving() {
    }

    public void onBlockClicked(BlockPos pos, EnumFacing side) {
        var sp = mcc$player();
        if (sp != null && pos != null) {
            sp.gameMode.handleBlockBreakAction(pos.mcc$modern(), net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    side == null ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.byName(side.getName()), sp.level().getMaxBuildHeight(), 0);
        }
    }

    public void blockRemoving(BlockPos pos) {
        var sp = mcc$player();
        if (sp != null && pos != null) {
            sp.gameMode.handleBlockBreakAction(pos.mcc$modern(), net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    net.minecraft.core.Direction.UP, sp.level().getMaxBuildHeight(), 0);
        }
    }

    public void cancelDestroyingBlock() {
        var sp = mcc$player();
        if (sp != null) {
            sp.gameMode.handleBlockBreakAction(sp.blockPosition(), net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                    net.minecraft.core.Direction.UP, sp.level().getMaxBuildHeight(), 0);
        }
    }

    /** Breaks the block as the player (events, tool damage, drops, game mode rules). */
    public boolean tryHarvestBlock(BlockPos pos) {
        var sp = mcc$player();
        return sp != null && pos != null && sp.gameMode.destroyBlock(pos.mcc$modern());
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

    public void setWorld(WorldServer world) {
        this.world = world;
    }

    /** Forge: how far the player reaches blocks (the modern block interaction range). */
    public double getBlockReachDistance() {
        var sp = mcc$player();
        return sp == null ? 5.0D : sp.blockInteractionRange();
    }

    /** Forge: sets the reach as the base of the modern block interaction range attribute. */
    public void setBlockReachDistance(double distance) {
        var sp = mcc$player();
        var attribute = sp == null ? null : sp.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        if (attribute != null) attribute.setBaseValue(distance);
    }
}
