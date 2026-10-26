package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemArmor;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The cauldron, ported from 1.7.10: metadata 0-3 is its water level. Water buckets fill it, glass bottles take water out,
 * dyed leather armour is washed in it, burning entities are put out, rain slowly fills it; comparators read the level.
 */
public class BlockCauldron extends Block {
    public IIcon iconInner;
    public IIcon iconTop;
    public IIcon iconBottom;

    public BlockCauldron(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockCauldron(Material material) {
        super(material);
    }

    public BlockCauldron() {
        this(Material.iron);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (iconTop == null && mcc$vanilla != null) return super.getIcon(side, meta);
        return side == 1 ? iconTop : side == 0 ? iconBottom : blockIcon;
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        iconInner = register.registerIcon(getTextureName() + "_inner");
        iconTop = register.registerIcon(getTextureName() + "_top");
        iconBottom = register.registerIcon(getTextureName() + "_bottom");
        blockIcon = register.registerIcon(getTextureName() + "_side");
    }

    /** The cauldron's "inner" and "bottom" textures. */
    public static IIcon getCauldronIcon(String name) {
        BlockCauldron cauldron = legacyconnector.v1_7_10.net.minecraft.init.Blocks.cauldron;
        IIcon icon = name.equals("inner") ? cauldron.iconInner : name.equals("bottom") ? cauldron.iconBottom : null;
        if (icon != null) return icon;
        return name.equals("inner") || name.equals("bottom") ? mcc$vanillaIcon("cauldron_" + name) : null;
    }

    /** The floor and the four walls. */
    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
        float wall = 0.125F;
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.3125F, 1.0F);
        super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
        setBlockBounds(0.0F, 0.0F, 0.0F, wall, 1.0F, 1.0F);
        super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, wall);
        super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
        setBlockBounds(1.0F - wall, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
        setBlockBounds(0.0F, 0.0F, 1.0F - wall, 1.0F, 1.0F, 1.0F);
        super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
        setBlockBoundsForItemRender();
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 24;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        int level = getPowerFromMeta(world.getBlockMetadata(x, y, z));
        float surface = y + (6.0F + 3 * level) / 16.0F;
        if (!world.isRemote && entity.isBurning() && level > 0 && Entity.mcc$get$boundingBox(entity).minY <= surface) {
            entity.extinguish();
            setWaterLevel(world, x, y, z, level - 1);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        InventoryPlayer inventory = player.inventory;
        ItemStack held = inventory.getCurrentItem();
        if (held == null) return true;
        boolean creative = PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities);
        int level = getPowerFromMeta(world.getBlockMetadata(x, y, z));
        if (held.getItem() == Items.water_bucket) {
            if (level < 3) {
                if (!creative) inventory.setInventorySlotContents(InventoryPlayer.mcc$get$currentItem(inventory), new ItemStack(Items.bucket));
                setWaterLevel(world, x, y, z, 3);
            }
            return true;
        }
        if (held.getItem() == Items.glass_bottle) {
            if (level > 0) {
                if (!creative) {
                    ItemStack water = new ItemStack(Items.potionitem, 1, 0);
                    if (!inventory.addItemStackToInventory(water)) {
                        world.spawnEntityInWorld(new EntityItem(world, x + 0.5D, y + 1.5D, z + 0.5D, water));
                    } else if (player instanceof EntityPlayerMP mp) {
                        mp.sendContainerToPlayer(EntityPlayer.mcc$get$inventoryContainer(player));
                    }
                    held.mcc$setCount(held.mcc$count() - 1);
                    if (held.mcc$count() <= 0) inventory.setInventorySlotContents(InventoryPlayer.mcc$get$currentItem(inventory), null);
                }
                setWaterLevel(world, x, y, z, level - 1);
            }
            return false;
        }
        if (level > 0 && held.getItem() instanceof ItemArmor armor && armor.getArmorMaterial() == ItemArmor.ArmorMaterial.CLOTH) {
            armor.removeColor(held);
            setWaterLevel(world, x, y, z, level - 1);
            return true;
        }
        return false;
    }

    /** {@code func_150024_a}: sets the water level (clamped to 0-3) and updates comparators. */
    public void setWaterLevel(World world, int x, int y, int z, int level) {
        world.setBlockMetadataWithNotify(x, y, z, MathHelper.clamp_int(level, 0, 3), 2);
        world.func_147453_f(x, y, z, this);
    }

    /** Rain fills the cauldron by one level now and then. */
    @Override
    public void fillWithRain(World world, int x, int y, int z) {
        if (world.rand.nextInt(20) != 1) return;
        int meta = world.getBlockMetadata(x, y, z);
        if (meta < 3) world.setBlockMetadataWithNotify(x, y, z, meta + 1, 2);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Items.cauldron;
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return Items.cauldron;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        return getPowerFromMeta(world.getBlockMetadata(x, y, z));
    }

    /** {@code func_150027_b}: the water level of a metadata value. */
    public static int getPowerFromMeta(int meta) {
        return meta;
    }

    /** The height of the water surface for a fill level (0-3). */
    public static float getRenderLiquidLevel(int level) {
        int l = MathHelper.clamp_int(level, 0, 3);
        return (6 + 3 * l) / 16.0F;
    }
}
