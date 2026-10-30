package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityHanging;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItemFrame;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityPainting;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 item that hangs an entity (painting, item frame) on the side of the clicked block. */
public class ItemHangingEntity extends Item {
    private final Class<? extends EntityHanging> hangingEntityClass;

    public ItemHangingEntity(Class<? extends EntityHanging> entityClass) {
        this.hangingEntityClass = entityClass;
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.DECORATIONS);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack itemstack = player.getHeldItem(hand);
        BlockPos blockpos = pos.offset(facing);
        if (facing != EnumFacing.DOWN && facing != EnumFacing.UP && player.canPlayerEdit(blockpos, facing, itemstack)) {
            EntityHanging entity = createEntity(worldIn, blockpos, facing);
            if (entity != null && entity.onValidSurface()) {
                if (!worldIn.isRemote) {
                    entity.playPlaceSound();
                    worldIn.spawnEntity(entity);
                }
                itemstack.shrink(1);
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }

    private EntityHanging createEntity(World worldIn, BlockPos pos, EnumFacing clickedSide) {
        if (hangingEntityClass == EntityPainting.class) return new EntityPainting(worldIn, pos, clickedSide);
        return hangingEntityClass == EntityItemFrame.class ? new EntityItemFrame(worldIn, pos, clickedSide) : null;
    }
}
