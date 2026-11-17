package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityBoat;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The boat item: right-click places a boat on the water (or block) the player looks at. */
public class ItemBoat extends Item {
    public ItemBoat(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemBoat() {
        super();
        this.maxStackSize = 1;
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabTransport);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (mcc$vanilla != null) return super.onItemRightClick(stack, world, player);
        MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, true);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return stack;
        int x = hit.blockX;
        int y = hit.blockY;
        int z = hit.blockZ;
        if (!world.isRemote) {
            if (world.getBlock(x, y, z) == Blocks.snow_layer) --y;
            EntityBoat boat = new EntityBoat(world, x + 0.5F, y + 1.0F, z + 0.5F);
            Entity.mcc$set$rotationYaw(boat, ((MathHelper.floor_double(Entity.mcc$get$rotationYaw(player) * 4.0F / 360.0F + 0.5D) & 3) - 1) * 90);
            AxisAlignedBB box = Entity.mcc$get$boundingBox(boat).expand(-0.1D, -0.1D, -0.1D);
            if (!world.getCollidingBoundingBoxes(boat, box).isEmpty()) return stack;
            world.spawnEntityInWorld(boat);
        }
        if (!PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)) stack.mcc$setCount(stack.mcc$count() - 1);
        return stack;
    }
}
