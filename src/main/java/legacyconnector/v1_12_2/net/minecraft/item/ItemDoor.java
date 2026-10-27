package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.BlockDoor;
import legacyconnector.v1_12_2.net.minecraft.block.SoundType;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** The item of a door: places both halves, facing away from the player, with the hinge on the side that has a wall or a door. */
public class ItemDoor extends Item {
    private final Block block;

    public ItemDoor(Block block) {
        this.block = block;
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.REDSTONE);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (facing != EnumFacing.UP) return EnumActionResult.FAIL;
        IBlockState iblockstate = worldIn.getBlockState(pos);
        if (!iblockstate.getBlock().isReplaceable(worldIn, pos)) pos = pos.offset(facing);
        ItemStack itemstack = player.getHeldItem(hand);
        if (player.canPlayerEdit(pos, facing, itemstack) && block.canPlaceBlockAt(worldIn, pos)) {
            EnumFacing enumfacing = player.getHorizontalFacing();
            int i = enumfacing.getXOffset();
            int j = enumfacing.getZOffset();
            boolean flag = i < 0 && hitZ < 0.5F || i > 0 && hitZ > 0.5F || j < 0 && hitX > 0.5F || j > 0 && hitX < 0.5F;
            placeDoor(worldIn, pos, enumfacing, block, flag);
            SoundType soundtype = worldIn.getBlockState(pos).getBlock().getSoundType(worldIn.getBlockState(pos), worldIn, pos, player);
            worldIn.playSound(player, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
            itemstack.shrink(1);
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }

    public static void placeDoor(World worldIn, BlockPos pos, EnumFacing facing, Block door, boolean isRightHinge) {
        BlockPos right = pos.offset(facing.rotateY());
        BlockPos left = pos.offset(facing.rotateYCCW());
        int i = (worldIn.getBlockState(left).isNormalCube() ? 1 : 0) + (worldIn.getBlockState(left.up()).isNormalCube() ? 1 : 0);
        int j = (worldIn.getBlockState(right).isNormalCube() ? 1 : 0) + (worldIn.getBlockState(right.up()).isNormalCube() ? 1 : 0);
        boolean doorLeft = worldIn.getBlockState(left).getBlock() == door || worldIn.getBlockState(left.up()).getBlock() == door;
        boolean doorRight = worldIn.getBlockState(right).getBlock() == door || worldIn.getBlockState(right.up()).getBlock() == door;
        if (doorLeft && !doorRight || j > i) isRightHinge = true;
        BlockPos up = pos.up();
        boolean powered = worldIn.isBlockPowered(pos) || worldIn.isBlockPowered(up);
        IBlockState iblockstate = door.getDefaultState().withProperty(BlockDoor.FACING, facing)
                .withProperty(BlockDoor.HINGE, isRightHinge ? BlockDoor.EnumHingePosition.RIGHT : BlockDoor.EnumHingePosition.LEFT)
                .withProperty(BlockDoor.POWERED, powered).withProperty(BlockDoor.OPEN, powered);
        worldIn.setBlockState(pos, iblockstate.withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.LOWER), 2);
        worldIn.setBlockState(up, iblockstate.withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.UPPER), 2);
        worldIn.notifyNeighborsOfStateChange(pos, door, false);
        worldIn.notifyNeighborsOfStateChange(up, door, false);
    }
}
