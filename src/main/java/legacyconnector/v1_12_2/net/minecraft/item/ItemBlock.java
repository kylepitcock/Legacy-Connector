package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.block.Block;

/** The item form of a block; registered under the block's name it becomes the block's modern item. */
public class ItemBlock extends Item {
    protected final Block block;

    public ItemBlock(Block block) {
        this.block = block;
        legacyconnector.v1_12_2.net.minecraftforge.registries.GameData.mcc$blockItem(block, this);
    }

    public Block getBlock() {
        return block;
    }

    @Override
    public String getUnlocalizedName() {
        return block.getUnlocalizedName();
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return block.getUnlocalizedName();
    }

    /**
     * 1.12's placement: into the clicked block when it can be replaced, else next to it; the block's state for placement from the
     * stack's metadata; one item used up. The modern item calls this only when the mod changed the placement (see Legacy1122BlockItem).
     */
    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult onItemUse(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY,
            float hitZ) {
        var level = world.mcc$level();
        if (!level.getBlockState(pos.mcc$modern()).canBeReplaced()) pos = pos.offset(facing);
        ItemStack stack = mcc$held(player, hand);
        var target = pos.mcc$modern();
        boolean editable = !(player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p)
                || p.mayUseItemAt(target, facing.mcc$modern(), org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModernLive(stack));
        if (stack.isEmpty() || !editable || !level.getBlockState(target).canBeReplaced() || !level.isInWorldBounds(target)) {
            return legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.FAIL;
        }
        int meta = getMetadata(stack.getMetadata());
        var placer = player;
        var state = block.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand);
        if (placeBlockAt(stack, player, world, pos, facing, hitX, hitY, hitZ, state)) {
            // the modern block carries the legacy block's sound type (BlockBridge1122)
            var sound = level.getBlockState(target).getSoundType();
            level.playSound(player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p ? p : null, target, sound.getPlaceSound(),
                    net.minecraft.sounds.SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            if (!(player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p && p.getAbilities().instabuild)) stack.shrink(1);
        }
        return legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult.SUCCESS;
    }

    /** Sets the block, then the tile entity data the stack carries and the block's own placement code. */
    public boolean placeBlockAt(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ,
            legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState newState) {
        if (!world.setBlockState(pos, newState, 11)) return false;
        var state = world.getBlockState(pos);
        if (state.getBlock() == block) {
            setTileEntityNBT(world, player, pos, stack);
            block.onBlockPlacedBy(world, pos, state, player, stack);
        }
        return true;
    }

    /** Copies the stack's {@code BlockEntityTag} into the placed tile entity (operators only for blocks that hold commands). */
    public static boolean setTileEntityNBT(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, ItemStack stack) {
        if (world == null || world.isRemote || stack == null) return false;
        var tag = stack.getSubCompound("BlockEntityTag");
        if (tag == null) return false;
        var te = world.getTileEntity(pos);
        if (te == null) return false;
        var data = te.writeToNBT(new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound());
        var before = (legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound) data.copy();
        data.merge(tag);
        data.setInteger("x", pos.getX());
        data.setInteger("y", pos.getY());
        data.setInteger("z", pos.getZ());
        if (data.equals(before)) return false;
        te.readFromNBT(data);
        te.markDirty();
        return true;
    }

    public boolean canPlaceBlockOnSide(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, ItemStack stack) {
        var level = world.mcc$level();
        if (!level.getBlockState(pos.mcc$modern()).canBeReplaced()) pos = pos.offset(side);
        return level.getBlockState(pos.mcc$modern()).canBeReplaced() && block.canPlaceBlockOnSide(world, pos, side);
    }

    @Override
    public String getUnlocalizedNameInefficiently(ItemStack stack) {
        return getUnlocalizedName(stack);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs getCreativeTab() {
        var own = super.getCreativeTab();
        return own != null ? own : block.getCreativeTabToDisplayOn();
    }

    /** A block item's tooltip is its block's. */
    @Override
    public void addInformation(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, java.util.List<String> tooltip,
            legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        block.addInformation(stack, worldIn, tooltip, flagIn);
    }
}
