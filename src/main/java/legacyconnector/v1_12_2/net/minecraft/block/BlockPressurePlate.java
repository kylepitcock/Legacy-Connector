package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.init.SoundEvents;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 wooden and stone pressure plates: full power while any entity (or any mob, for {@link Sensitivity#MOBS}) stands on them. */
public class BlockPressurePlate extends BlockBasePressurePlate {
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    private final Sensitivity sensitivity;

    protected BlockPressurePlate(Material materialIn, Sensitivity sensitivityIn) {
        super(materialIn);
        setDefaultState(blockState.getBaseState().withProperty(POWERED, false));
        this.sensitivity = sensitivityIn == null ? Sensitivity.EVERYTHING : sensitivityIn;
    }

    @Override
    protected int getRedstoneStrength(IBlockState state) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected IBlockState setRedstoneStrength(IBlockState state, int strength) {
        return state.withProperty(POWERED, strength > 0);
    }

    @Override
    protected void playClickOnSound(World worldIn, BlockPos pos) {
        worldIn.playSound(null, pos, getMaterialLegacy() == Material.WOOD ? SoundEvents.BLOCK_WOOD_PRESSPLATE_CLICK_ON : SoundEvents.BLOCK_STONE_PRESSPLATE_CLICK_ON,
                SoundCategory.BLOCKS, 0.3F, getMaterialLegacy() == Material.WOOD ? 0.8F : 0.6F);
    }

    @Override
    protected void playClickOffSound(World worldIn, BlockPos pos) {
        worldIn.playSound(null, pos, getMaterialLegacy() == Material.WOOD ? SoundEvents.BLOCK_WOOD_PRESSPLATE_CLICK_OFF : SoundEvents.BLOCK_STONE_PRESSPLATE_CLICK_OFF,
                SoundCategory.BLOCKS, 0.3F, getMaterialLegacy() == Material.WOOD ? 0.7F : 0.5F);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected int computeRedstoneStrength(World worldIn, BlockPos pos) {
        AxisAlignedBB axisalignedbb = PRESSURE_AABB.offset(pos);
        List<? extends Entity> list = switch (sensitivity) {
            case MOBS -> worldIn.getEntitiesWithinAABB(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.class, axisalignedbb);
            default -> worldIn.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
        };
        for (Entity entity : list) if (!entity.doesEntityNotTriggerPressurePlate()) return 15;
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(POWERED, meta == 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(POWERED) ? 1 : 0;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POWERED);
    }

    public enum Sensitivity {
        EVERYTHING,
        MOBS;
    }
}
