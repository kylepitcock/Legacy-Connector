package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Vanilla 1.7.10 shears: fast on wool, leaves and cobwebs, harvest cobwebs and redstone, shear {@code IShearable} entities. */
public class ItemShears extends Item {
    public ItemShears(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected ItemShears() {
        super();
        this.maxStackSize = 1;
        this.setMaxDamage(238);
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase entity) {
        Material m = block.getMaterial();
        if (m == Material.leaves || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.web || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.tallgrass
                || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.vine || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.tripwire
                || block instanceof legacyconnector.v1_7_10.net.minecraftforge.common.IShearable) {
            stack.damageItem(1, entity);
            return true;
        }
        return super.onBlockDestroyed(stack, world, block, x, y, z, entity);
    }

    @Override
    public boolean canItemHarvestBlock(Block block) {
        return block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.web || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.redstone_wire
                || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.tripwire;
    }

    @Override
    public float getStrVsBlock(ItemStack stack, Block block) {
        if (block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.web || block.getMaterial() == Material.leaves) return 15.0F;
        return block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.wool ? 5.0F : super.getStrVsBlock(stack, block);
    }

    /** Forge: shearing an entity that is {@code IShearable} drops what it says and costs one durability. */
    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase entity) {
        if (entity.mcc$world().isRemote) return false;
        if (entity instanceof legacyconnector.v1_7_10.net.minecraftforge.common.IShearable target) {
            int x = (int) Math.floor(legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posX(entity));
            int y = (int) Math.floor(legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posY(entity));
            int z = (int) Math.floor(legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posZ(entity));
            if (target.isShearable(stack, entity.mcc$world(), x, y, z)) {
                java.util.ArrayList<ItemStack> drops = target.onSheared(stack, entity.mcc$world(), x, y, z, 0);
                if (drops != null) for (ItemStack drop : drops) entity.entityDropItem(drop, 1.0F);
                stack.damageItem(1, entity);
            }
            return true;
        }
        return false;
    }
}
