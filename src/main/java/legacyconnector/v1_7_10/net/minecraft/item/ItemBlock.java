package legacyconnector.v1_7_10.net.minecraft.item;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.World;

public class ItemBlock extends Item {
    protected final Block blockInstance;

    public ItemBlock(Block block) {
        this.blockInstance = block;
    }

    /** Wrapper for the item form of a vanilla (or modern-only) block. */
    public ItemBlock(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info, Block block) {
        super(info);
        this.blockInstance = block;
    }

    public Block mcc$block() {
        return blockInstance;
    }

    @Override
    public ItemBlock setUnlocalizedName(String name) {
        super.setUnlocalizedName(name);
        return this;
    }

    /** As 1.7.10: the item atlas only when the block names an icon of its own for the item (doors, cakes, beds). */
    @Override
    public int getSpriteNumber() {
        return blockInstance != null && blockInstance.getItemIconName() != null ? 1 : 0;
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        return blockInstance == null ? super.getIconFromDamage(damage) : blockInstance.getIcon(1, damage);
    }

    /** Vanilla 1.7.10 placement logic, verbatim in behaviour. */
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        Block target = world.getBlock(x, y, z);
        if (target == Blocks.snow_layer && (world.getBlockMetadata(x, y, z) & 7) < 1) {
            side = 1;
        } else if (!target.isReplaceable(world, x, y, z)) {
            switch (side) {
                case 0 -> y--;
                case 1 -> y++;
                case 2 -> z--;
                case 3 -> z++;
                case 4 -> x--;
                case 5 -> x++;
                default -> { }
            }
        }
        if (stack.mcc$count() == 0) return false;
        if (!player.canPlayerEdit(x, y, z, side, stack)) return false;
        if (y >= world.mcc$maxBuildHeight() - 1 && blockInstance.getMaterial().isSolid()) return false;
        if (!world.canPlaceEntityOnSide(blockInstance, x, y, z, false, side, player, stack)) return false;
        int meta = getMetadata(stack.getItemDamage());
        int placedMeta = blockInstance.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, meta);
        if (placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, placedMeta)) {
            world.mcc$playPlaceSound(blockInstance, x, y, z);
            stack.mcc$setCount(stack.mcc$count() - 1);
        }
        return true;
    }

    /** Forge hook used by most custom ItemBlocks. */
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        if (!world.setBlock(x, y, z, blockInstance, metadata, 3)) return false;
        if (world.getBlock(x, y, z) == blockInstance) {
            blockInstance.onBlockPlacedBy(world, x, y, z, player, stack);
            blockInstance.onPostBlockPlaced(world, x, y, z, metadata);
        }
        return true;
    }

    public boolean func_150936_a(World world, int x, int y, int z, int side, EntityPlayer player, ItemStack stack) {
        return true;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return blockInstance == null ? super.getUnlocalizedName(stack) : blockInstance.getUnlocalizedName();
    }

    @Override
    public String getUnlocalizedName() {
        return blockInstance == null ? super.getUnlocalizedName() : blockInstance.getUnlocalizedName();
    }

    @Override
    public CreativeTabs getCreativeTab() {
        return blockInstance == null ? super.getCreativeTab() : blockInstance.getCreativeTabToDisplayOn();
    }

    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        if (blockInstance != null) blockInstance.getSubBlocks(item, tab, list);
    }

    @Override
    public void registerIcons(IIconRegister register) {
        String s = blockInstance == null ? null : blockInstance.getItemIconName();
        if (s != null) this.itemIcon = register.registerIcon(s);
    }
}
