package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 hoe: right-click tills grass and dirt under an air block (a mod's hoe such as Thaumcraft's has no modern tilling logic of its own). */
public class ItemHoe extends Item {
    protected Item.ToolMaterial theToolMaterial;

    public ItemHoe(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.theToolMaterial = ItemTool.mcc$materialOf(info);
    }

    public ItemHoe(Item.ToolMaterial material) {
        this.theToolMaterial = material == null ? Item.ToolMaterial.WOOD : material;
        this.maxStackSize = 1;
        this.setMaxDamage(theToolMaterial.getMaxUses());
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (!player.canPlayerEdit(x, y, z, side, stack)) return false;
        Block block = world.getBlock(x, y, z);
        if (side != 0 && world.isAirBlock(x, y + 1, z) && (block == Blocks.grass || block == Blocks.dirt)) {
            Block farmland = Blocks.farmland;
            world.mcc$playPlaceSound(farmland, x, y, z);
            if (world.isRemote) return true;
            world.setBlock(x, y, z, farmland);
            stack.damageItem(1, player);
            return true;
        }
        return false;
    }

    public String getToolMaterialName() {
        return theToolMaterial.toString();
    }
}
