package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.InventoryEnderChest;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityEnderChest;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * An ender chest (1.7.10 {@code BlockEnderChest}): a 7/8 high chest that opens the player's own ender inventory (blocked by a solid block
 * above), faces the placer, drops 8 obsidian (itself with Silk Touch).
 */
public class BlockEnderChest extends BlockContainer {
    public BlockEnderChest(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockEnderChest() {
        super(Material.rock);
        setCreativeTab(CreativeTabs.tabDecorations);
        setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 22;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return mcc$vanilla != null ? super.getItemDropped(meta, random, fortune) : Item.getItemFromBlock(legacyconnector.v1_7_10.net.minecraft.init.Blocks.obsidian);
    }

    @Override
    public int quantityDropped(Random random) {
        return mcc$vanilla != null ? super.quantityDropped(random) : 8;
    }

    @Override
    public boolean canSilkHarvest() {
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int facing = MathHelper.floor_double(legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$rotationYaw(placer) * 4.0F / 360.0F + 0.5D) & 3;
        int meta = 0;
        if (facing == 0) meta = 2;
        if (facing == 1) meta = 5;
        if (facing == 2) meta = 3;
        if (facing == 3) meta = 4;
        world.setBlockMetadataWithNotify(x, y, z, meta, 2);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        InventoryEnderChest inventory = player.getInventoryEnderChest();
        TileEntity tile = world.getTileEntity(x, y, z);
        if (inventory == null || !(tile instanceof TileEntityEnderChest chest)) return true;
        if (world.getBlock(x, y + 1, z).isNormalCube()) return true;
        if (world.isRemote) return true;
        inventory.setChestTileEntity(chest);
        player.displayGUIChest(inventory);
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityEnderChest();
    }

    /** Client-only particles around the chest in vanilla; the modern ender chest draws its own, so nothing is added here. */
    @Override
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        this.blockIcon = register.registerIcon("obsidian");
    }
}
