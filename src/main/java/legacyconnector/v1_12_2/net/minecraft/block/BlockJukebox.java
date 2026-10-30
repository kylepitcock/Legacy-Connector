package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 jukebox: HAS_RECORD (metadata 1) while a disc is in it; the disc is its tile entity's. A vanilla jukebox's disc is the modern
 * jukebox's (inserting one there plays it).
 */
public class BlockJukebox extends BlockContainer {
    public static final PropertyBool HAS_RECORD = PropertyBool.create("has_record");

    protected BlockJukebox() {
        super(Material.WOOD, MapColor.DIRT);
        setDefaultState(blockState.getBaseState().withProperty(HAS_RECORD, false));
        setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (state.getValue(HAS_RECORD)) {
            dropRecord(worldIn, pos, state);
            state = state.withProperty(HAS_RECORD, false);
            worldIn.setBlockState(pos, state, 2);
            return true;
        }
        return false;
    }

    /** Puts a disc in (the modern jukebox plays it for a vanilla jukebox). */
    public void insertRecord(World worldIn, BlockPos pos, IBlockState state, ItemStack recordStack) {
        var level = worldIn.mcc$liveLevel();
        if (level != null) {
            var be = level.getBlockEntity(org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos));
            if (be instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity jukebox) {
                jukebox.setTheItem(org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(recordStack.copy()));
                return;
            }
        }
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityJukebox jukebox) {
            jukebox.setRecord(recordStack.copy());
            worldIn.setBlockState(pos, state.withProperty(HAS_RECORD, true), 2);
        }
    }

    private void dropRecord(World worldIn, BlockPos pos, IBlockState state) {
        if (worldIn.isRemote) return;
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityJukebox jukebox) {
            ItemStack record = jukebox.getRecord();
            if (!record.isEmpty()) {
                worldIn.playEvent(1010, pos, 0);
                worldIn.playRecord(pos, null);
                jukebox.setRecord(ItemStack.EMPTY);
                spawnAsEntity(worldIn, pos, record.copy());
            }
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        dropRecord(worldIn, pos, state);
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityJukebox();
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityJukebox jukebox && !jukebox.getRecord().isEmpty()) {
            return Math.max(1, legacyconnector.v1_12_2.net.minecraft.item.Item.getIdFromItem(jukebox.getRecord().getItem())
                    - legacyconnector.v1_12_2.net.minecraft.item.Item.getIdFromItem(legacyconnector.v1_12_2.net.minecraft.init.Items.RECORD_13) + 1);
        }
        return 0;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HAS_RECORD, meta > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(HAS_RECORD) ? 1 : 0;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HAS_RECORD);
    }

    /** The jukebox's disc, saved as "RecordItem". */
    public static class TileEntityJukebox extends TileEntity {
        private ItemStack record = ItemStack.EMPTY;

        @Override
        public void readFromNBT(NBTTagCompound compound) {
            super.readFromNBT(compound);
            if (compound.hasKey("RecordItem", 10)) setRecord(new ItemStack(compound.getCompoundTag("RecordItem")));
            else if (compound.getInteger("Record") > 0) {
                setRecord(new ItemStack(legacyconnector.v1_12_2.net.minecraft.item.Item.getItemById(compound.getInteger("Record"))));
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound compound) {
            super.writeToNBT(compound);
            if (!getRecord().isEmpty()) compound.setTag("RecordItem", getRecord().writeToNBT(new NBTTagCompound()));
            return compound;
        }

        public ItemStack getRecord() {
            return record;
        }

        public void setRecord(ItemStack recordStack) {
            this.record = recordStack;
            markDirty();
        }
    }
}
