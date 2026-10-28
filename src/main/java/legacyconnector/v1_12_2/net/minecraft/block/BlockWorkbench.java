package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 crafting tables (mods' workbenches extend it): right-click opens the 3x3 crafting screen, valid while the player stays near. */
public class BlockWorkbench extends Block {
    protected BlockWorkbench() {
        super(Material.WOOD);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.DECORATIONS);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (worldIn.isRemote) return true;
        var player = playerIn.mcc$player();
        var level = worldIn.mcc$level();
        if (player == null || level == null) return true;
        var mp = org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos);
        var table = level.getBlockState(mp).getBlock();
        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) -> new net.minecraft.world.inventory.CraftingMenu(id, inventory,
                net.minecraft.world.inventory.ContainerLevelAccess.create(level, mp)) {
            /** Vanilla's check wants a vanilla crafting table; this one is the mod's block. */
            @Override
            public boolean stillValid(net.minecraft.world.entity.player.Player who) {
                return level.getBlockState(mp).is(table) && who.distanceToSqr(mp.getX() + 0.5D, mp.getY() + 0.5D, mp.getZ() + 0.5D) <= 64.0D;
            }
        }, net.minecraft.network.chat.Component.translatable("container.crafting")));
        return true;
    }
}
