package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityBoat;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.init.Blocks;
import legacyconnector.v1_12_2.net.minecraft.util.ActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 boat item: right-clicking a block or water the player looks at puts a boat of the item's wood on it, unless something solid
 * is in the way. Mods subclass it for boats of their own wood type (the type decides the modern boat that appears).
 */
public class ItemBoat extends Item {
    private final EntityBoat.Type type;

    public ItemBoat(EntityBoat.Type typeIn) {
        this.type = typeIn;
        this.maxStackSize = 1;
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.TRANSPORTATION);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        RayTraceResult hit = rayTrace(worldIn, playerIn, true);
        if (hit == null) return new ActionResult<>(EnumActionResult.PASS, itemstack);
        Vec3d look = playerIn.getLook(1.0F);
        Vec3d eyes = playerIn.getPositionEyes(1.0F);
        boolean inside = false;
        for (Object o : worldIn.getEntitiesWithinAABBExcludingEntity(playerIn, playerIn.getEntityBoundingBox().expand(look.x * 5.0D, look.y * 5.0D, look.z * 5.0D).grow(1.0D))) {
            Entity entity = (Entity) o;
            if (entity.canBeCollidedWith() && entity.getEntityBoundingBox().grow(entity.getCollisionBorderSize()).contains(eyes)) inside = true;
        }
        if (inside || hit.typeOfHit != RayTraceResult.Type.BLOCK) return new ActionResult<>(EnumActionResult.PASS, itemstack);
        var block = worldIn.getBlockState(hit.getBlockPos()).getBlock();
        boolean water = block == Blocks.WATER || block == Blocks.FLOWING_WATER;
        EntityBoat boat = new EntityBoat(worldIn, hit.hitVec.x, water ? hit.hitVec.y - 0.12D : hit.hitVec.y, hit.hitVec.z);
        boat.setBoatType(this.type);
        Entity.mcc$set$rotationYaw(boat, Entity.mcc$get$rotationYaw(playerIn));
        AxisAlignedBB box = boat.getEntityBoundingBox().grow(-0.1D);
        if (!worldIn.getCollisionBoxes(boat, box).isEmpty()) return new ActionResult<>(EnumActionResult.FAIL, itemstack);
        if (!worldIn.isRemote) worldIn.spawnEntity(boat);
        if (!legacyconnector.v1_12_2.net.minecraft.entity.player.PlayerCapabilities.mcc$get$isCreativeMode(EntityPlayer.mcc$get$capabilities(playerIn))) itemstack.shrink(1);
        return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
    }
}
