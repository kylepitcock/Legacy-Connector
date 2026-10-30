package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 weighted pressure plates: the signal (the metadata) grows with the number of entities on it, up to its maximum weight. */
public class BlockPressurePlateWeighted extends BlockBasePressurePlate {
    public static final PropertyInteger POWER = PropertyInteger.create("power", 0, 15);
    private final int maxWeight;

    protected BlockPressurePlateWeighted(Material materialIn, int p_i46379_2_) {
        this(materialIn, p_i46379_2_, materialIn.getMaterialMapColor());
    }

    protected BlockPressurePlateWeighted(Material materialIn, int p_i46380_2_, MapColor color) {
        super(materialIn, color);
        setDefaultState(blockState.getBaseState().withProperty(POWER, 0));
        this.maxWeight = p_i46380_2_;
    }

    @Override
    protected int computeRedstoneStrength(World worldIn, BlockPos pos) {
        int count = Math.min(worldIn.getEntitiesWithinAABB(Entity.class, PRESSURE_AABB.offset(pos)).size(), maxWeight);
        if (count <= 0) return 0;
        float f = (float) Math.min(maxWeight, count) / (float) maxWeight;
        return (int) Math.ceil(f * 15.0F);
    }

    @Override
    protected void playClickOnSound(World worldIn, BlockPos color) {
        worldIn.playSound(null, color, legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.BLOCK_METAL_PRESSPLATE_CLICK_ON,
                legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.BLOCKS, 0.3F, 0.90000004F);
    }

    @Override
    protected void playClickOffSound(World worldIn, BlockPos pos) {
        worldIn.playSound(null, pos, legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.BLOCK_METAL_PRESSPLATE_CLICK_OFF,
                legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.BLOCKS, 0.3F, 0.75F);
    }

    @Override
    protected int getRedstoneStrength(IBlockState state) {
        return state.getValue(POWER);
    }

    @Override
    protected IBlockState setRedstoneStrength(IBlockState state, int strength) {
        return state.withProperty(POWER, strength);
    }

    @Override
    public int tickRate(World worldIn) {
        return 10;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(POWER, meta);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(POWER);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POWER);
    }
}
