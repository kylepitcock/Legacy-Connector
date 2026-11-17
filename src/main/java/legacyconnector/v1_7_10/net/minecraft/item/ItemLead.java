package legacyconnector.v1_7_10.net.minecraft.item;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLeashKnot;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The lead: used on a fence it ties every animal the player is leading to a leash knot there. */
public class ItemLead extends Item {
    public ItemLead(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemLead() {
        super();
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabTools);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (mcc$vanilla != null) return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
        Block block = world.getBlock(x, y, z);
        if (!(block instanceof legacyconnector.v1_7_10.net.minecraft.block.BlockFence)) return false;
        if (!world.isRemote) func_150909_a(player, world, x, y, z);
        return true;
    }

    /** {@code func_150909_a}: ties the leashed animals within 7 blocks that the player holds to the fence knot at the position. */
    public static boolean func_150909_a(EntityPlayer player, World world, int x, int y, int z) {
        EntityLeashKnot knot = EntityLeashKnot.getKnotForBlock(world, x, y, z);
        boolean tied = false;
        double range = 7.0D;
        List list = world.getEntitiesWithinAABB(EntityLiving.class,
                AxisAlignedBB.getBoundingBox(x - range, y - range, z - range, x + range, y + range, z + range));
        if (list != null) {
            for (Object o : list) {
                EntityLiving animal = (EntityLiving) o;
                if (animal.getLeashed() && animal.getLeashedToEntity() == player) {
                    if (knot == null) knot = EntityLeashKnot.func_110129_a(world, x, y, z);
                    animal.setLeashedToEntity(knot, true);
                    tied = true;
                }
            }
        }
        return tied;
    }
}
