package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;

/**
 * The vanilla screens the 1.7.10 vanilla blocks open (what {@code EntityPlayer.displayGUIWorkbench} did), as the modern menus.
 * They are opened for mod blocks that subclass the vanilla block, so their validity is the distance check only (the modern
 * menus otherwise close at once when the block is not the vanilla one).
 */
public final class VanillaMenus1710 {
    private VanillaMenus1710() {}

    /** Opens a 3x3 crafting grid for the block at the position (server side; nothing on the client). */
    public static void openWorkbench(EntityPlayer player, World world, int x, int y, int z) {
        if (player == null || !(player.mcc$player() instanceof ServerPlayer sp)) return;
        var level = VanillaOps.live(world);
        if (level == null) level = sp.level();
        BlockPos pos = new BlockPos(x, y, z);
        ContainerLevelAccess access = ContainerLevelAccess.create(level, pos);
        sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftingMenu(id, inv, access) {
            @Override
            public boolean stillValid(Player who) {
                return who.distanceToSqr(x + 0.5D, y + 0.5D, z + 0.5D) <= 64.0D;
            }
        }, Component.translatable("container.crafting")));
    }
}
