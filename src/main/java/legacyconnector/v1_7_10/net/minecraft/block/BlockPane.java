package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/**
 * 1.7.10's panes and bars: the face texture ({@code blockIcon}) and the edge texture ({@link #func_150097_e}) a mod passes to
 * the constructor, render type 18 (41 for glass), connections to neighbours, the matching bounds and collision boxes. In the
 * world they are drawn by {@code org.kylepitcock.legacyconnector.v1_7_10.client.VanillaBlockRenderers}.
 */
public class BlockPane extends legacyconnector.v1_7_10.net.minecraft.block.Block {
    /** The edge texture's name. */
    private String field_150100_a;
    /** Whether the pane drops itself when broken. */
    private boolean field_150099_b = true;
    /** The face texture's name. */
    private String field_150101_M;
    /** The edge icon. */
    private IIcon field_150102_N;

    public BlockPane(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockPane(Material material) {
        super(material);
    }

    protected BlockPane(String face, String edge, Material material, boolean dropsItself) {
        this(material);
        this.field_150100_a = edge;
        this.field_150099_b = dropsItself;
        this.field_150101_M = face;
        this.setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabDecorations);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return !field_150099_b ? null : super.getItemDropped(meta, random, fortune);
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
        return blockMaterial == Material.glass ? 41 : 18;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return world.getBlock(x, y, z) != this && super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
        boolean north = canPaneConnectTo(world, x, y, z - 1, ForgeDirection.NORTH);
        boolean south = canPaneConnectTo(world, x, y, z + 1, ForgeDirection.SOUTH);
        boolean west = canPaneConnectTo(world, x - 1, y, z, ForgeDirection.WEST);
        boolean east = canPaneConnectTo(world, x + 1, y, z, ForgeDirection.EAST);
        boolean any = north || south || west || east;
        if ((!west || !east) && any) {
            if (west && !east) {
                setBlockBounds(0.0F, 0.0F, 0.4375F, 0.5F, 1.0F, 0.5625F);
                super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
            } else if (!west && east) {
                setBlockBounds(0.5F, 0.0F, 0.4375F, 1.0F, 1.0F, 0.5625F);
                super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
            }
        } else {
            setBlockBounds(0.0F, 0.0F, 0.4375F, 1.0F, 1.0F, 0.5625F);
            super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
        }
        if ((!north || !south) && any) {
            if (north && !south) {
                setBlockBounds(0.4375F, 0.0F, 0.0F, 0.5625F, 1.0F, 0.5F);
                super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
            } else if (!north && south) {
                setBlockBounds(0.4375F, 0.0F, 0.5F, 0.5625F, 1.0F, 1.0F);
                super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
            }
        } else {
            setBlockBounds(0.4375F, 0.0F, 0.0F, 0.5625F, 1.0F, 1.0F);
            super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
        }
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        float x1 = 0.4375F, x2 = 0.5625F, z1 = 0.4375F, z2 = 0.5625F;
        boolean north = canPaneConnectTo(world, x, y, z - 1, ForgeDirection.NORTH);
        boolean south = canPaneConnectTo(world, x, y, z + 1, ForgeDirection.SOUTH);
        boolean west = canPaneConnectTo(world, x - 1, y, z, ForgeDirection.WEST);
        boolean east = canPaneConnectTo(world, x + 1, y, z, ForgeDirection.EAST);
        boolean any = north || south || west || east;
        if ((!west || !east) && any) {
            if (west && !east) x1 = 0.0F;
            else if (!west && east) x2 = 1.0F;
        } else {
            x1 = 0.0F;
            x2 = 1.0F;
        }
        if ((!north || !south) && any) {
            if (north && !south) z1 = 0.0F;
            else if (!north && south) z2 = 1.0F;
        } else {
            z1 = 0.0F;
            z2 = 1.0F;
        }
        setBlockBounds(x1, 0.0F, z1, x2, 1.0F, z2);
    }

    /** The edge texture (the top, bottom and ends of the pane). */
    public IIcon func_150097_e() {
        return field_150102_N != null ? field_150102_N : blockIcon;
    }

    /** Whether a pane connects to this kind of block: full blocks, glass and other panes. */
    public final boolean canPaneConnectToBlock(legacyconnector.v1_7_10.net.minecraft.block.Block block) {
        if (block == null) return false;
        return block.isFullBlock() || block == this || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.glass
                || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.stained_glass
                || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.stained_glass_pane || block instanceof BlockPane;
    }

    @Override
    public boolean canSilkHarvest() {
        return true;
    }

    @Override
    protected ItemStack createStackedBlock(int meta) {
        Item item = Item.getItemFromBlock(this);
        return item == null ? null : new ItemStack(item, 1, meta);
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        if (field_150101_M == null) {
            super.registerBlockIcons(register);
            return;
        }
        this.blockIcon = register.registerIcon(field_150101_M);
        this.field_150102_N = register.registerIcon(field_150100_a != null ? field_150100_a : field_150101_M);
    }

    public boolean canPaneConnectTo(IBlockAccess world, int x, int y, int z, ForgeDirection direction) {
        return canPaneConnectToBlock(world.getBlock(x, y, z)) || world.isSideSolid(x, y, z, direction.getOpposite(), false);
    }
}
