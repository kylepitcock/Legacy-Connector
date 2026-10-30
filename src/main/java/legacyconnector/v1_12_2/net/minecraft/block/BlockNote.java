package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.init.SoundEvents;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityNote;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 note block: plays its note (the tile entity's pitch) with the instrument of the block below, on a click or a redstone pulse. */
public class BlockNote extends BlockContainer {
    private static final java.util.List<SoundEvent> INSTRUMENTS = java.util.List.of(SoundEvents.BLOCK_NOTE_HARP, SoundEvents.BLOCK_NOTE_BASEDRUM,
            SoundEvents.BLOCK_NOTE_SNARE, SoundEvents.BLOCK_NOTE_HAT, SoundEvents.BLOCK_NOTE_BASS, SoundEvents.BLOCK_NOTE_FLUTE, SoundEvents.BLOCK_NOTE_BELL,
            SoundEvents.BLOCK_NOTE_GUITAR, SoundEvents.BLOCK_NOTE_CHIME, SoundEvents.BLOCK_NOTE_XYLOPHONE);

    public BlockNote() {
        super(Material.WOOD);
        setCreativeTab(CreativeTabs.REDSTONE);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        boolean powered = worldIn.isBlockPowered(pos);
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityNote note && note.previousRedstoneState != powered) {
            if (powered) note.triggerNote(worldIn, pos);
            note.previousRedstoneState = powered;
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (worldIn.isRemote) return true;
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityNote note) {
            note.changePitch();
            note.triggerNote(worldIn, pos);
        }
        return true;
    }

    @Override
    public void onBlockClicked(World worldIn, BlockPos pos, EntityPlayer playerIn) {
        if (worldIn.isRemote) return;
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityNote note) note.triggerNote(worldIn, pos);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityNote();
    }

    /** The sound of an instrument number (0 harp ... 9 xylophone). */
    protected SoundEvent getInstrument(int p_185576_1_) {
        if (p_185576_1_ < 0 || p_185576_1_ >= INSTRUMENTS.size()) p_185576_1_ = 0;
        return INSTRUMENTS.get(p_185576_1_);
    }

    /** Plays instrument {@code id} at pitch {@code param} (the block event a note block sends itself). */
    @Override
    public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int id, int param) {
        float pitch = (float) Math.pow(2.0D, (param - 12) / 12.0D);
        worldIn.playSound(null, pos, getInstrument(id), SoundCategory.RECORDS, 3.0F, pitch);
        return true;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType getRenderType(IBlockState state) {
        return legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType.MODEL;
    }
}
