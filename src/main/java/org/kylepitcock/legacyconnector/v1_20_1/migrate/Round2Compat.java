package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Helpers of 1.20.1 calls that 1.21.1 dropped or moved (ATM 9 gap round of 2026-11-13, second pass). */
public final class Round2Compat {
    private Round2Compat() {}

    /**
     * {@code Player.findRespawnPositionAndUseSpawnBlock(level, pos, angle, forced, keepInventory)} (static in 1.20.1, private in {@code ServerPlayer} since
     * 1.21): the stand-up position at a bed, a respawn anchor (which loses a charge unless forced or the inventory is kept) or any block that allows it.
     */
    public static Optional<Vec3> findRespawnPosition(ServerLevel level, BlockPos pos, float angle, boolean forced, boolean keepInventory) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof RespawnAnchorBlock && (forced || state.getValue(RespawnAnchorBlock.CHARGE) > 0) && RespawnAnchorBlock.canSetSpawn(level)) {
            Optional<Vec3> stand = RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, level, pos);
            if (!forced && !keepInventory && stand.isPresent()) {
                level.setBlock(pos, state.setValue(RespawnAnchorBlock.CHARGE, state.getValue(RespawnAnchorBlock.CHARGE) - 1), 3);
            }
            return stand;
        } else if (block instanceof BedBlock && BedBlock.canSetSpawn(level)) {
            return BedBlock.findStandUpPosition(EntityType.PLAYER, level, pos, state.getValue(BlockStateProperties.HORIZONTAL_FACING), angle);
        } else if (!forced) {
            return state.getRespawnPosition(EntityType.PLAYER, level, pos, angle).map(ServerPlayer.RespawnPosAngle::position);
        }
        boolean here = block.isPossibleToRespawnInThis(state);
        BlockState above = level.getBlockState(pos.above());
        boolean there = above.getBlock().isPossibleToRespawnInThis(above);
        return here && there ? Optional.of(new Vec3(pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D)) : Optional.empty();
    }

    /** {@code UUIDTypeAdapter.fromUUID(uuid)}: the UUID without dashes (authlib 6 moved it to {@code UndashedUuid}). */
    public static String uuidToString(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    /** {@code UUIDTypeAdapter.fromString(text)}: parses a UUID with or without dashes. */
    public static UUID uuidFromString(String text) {
        if (text.indexOf('-') >= 0) return UUID.fromString(text);
        return UUID.fromString(text.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
    }

    /** {@code AbstractWidget.setTooltipDelay(int milliseconds)}: 1.21.1 takes a {@code Duration}. */
    public static void setTooltipDelay(AbstractWidget widget, int milliseconds) {
        widget.setTooltipDelay(Duration.ofMillis(milliseconds));
    }

    /** {@code Screen.renderDirtBackground(GuiGraphics)}: the tiled menu background (1.21.1 draws it with {@code renderMenuBackgroundTexture}). */
    public static void renderDirtBackground(net.minecraft.client.gui.screens.Screen screen, net.minecraft.client.gui.GuiGraphics graphics) {
        net.minecraft.client.gui.screens.Screen.renderMenuBackgroundTexture(graphics, net.minecraft.client.gui.screens.Screen.MENU_BACKGROUND, 0, 0, 0.0F, 0.0F, screen.width,
                screen.height);
    }

    /** {@code SharedConstants.filterText(String)} moved to {@code StringUtil}. */
    public static String filterText(String text) {
        return StringUtil.filterText(text);
    }
}
