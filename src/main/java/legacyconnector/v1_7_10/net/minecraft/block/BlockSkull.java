package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.ArrayList;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntitySkull;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps;

/**
 * A mob head, ported from 1.7.10 with Forge's drop handling: metadata 1 on the floor (its {@link TileEntitySkull} holds the
 * rotation), 2-5 on a wall, bit 8 "broken in creative, no drop". Three wither skeleton skulls on a T of soul sand make a wither.
 */
public class BlockSkull extends BlockContainer {
    public BlockSkull(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockSkull() {
        super(Material.circuits);
        setBlockBounds(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F);
    }

    @Override
    public int getRenderType() {
        return -1;
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
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        switch (world.getBlockMetadata(x, y, z) & 7) {
            case 2 -> setBlockBounds(0.25F, 0.25F, 0.5F, 0.75F, 0.75F, 1.0F);
            case 3 -> setBlockBounds(0.25F, 0.25F, 0.0F, 0.75F, 0.75F, 0.5F);
            case 4 -> setBlockBounds(0.5F, 0.25F, 0.25F, 1.0F, 0.75F, 0.75F);
            case 5 -> setBlockBounds(0.0F, 0.25F, 0.25F, 0.5F, 0.75F, 0.75F);
            default -> setBlockBounds(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F);
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int facing = MathHelper.floor_double(Entity.mcc$get$rotationYaw(placer) * 4.0F / 360.0F + 2.5D) & 3;
        world.setBlockMetadataWithNotify(x, y, z, facing, 2);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntitySkull();
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return Items.skull;
    }

    @Override
    public int getDamageValue(World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(x, y, z);
        return te instanceof TileEntitySkull skull ? skull.getSkullType() : super.getDamageValue(world, x, y, z);
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    /**
     * Forge: the head drops here, while its tile entity (type, owner) still exists; the metadata then gets the "no drop" bit so the
     * later harvest drops nothing more. Broken in creative it drops nothing.
     */
    @Override
    public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
        if (!PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)) dropBlockAsItem(world, x, y, z, meta, 0);
        world.setBlockMetadataWithNotify(x, y, z, meta | 8, 4);
        super.onBlockHarvested(world, x, y, z, meta, player);
    }

    /** Forge: the head as an item, with the owner's profile for a player head; nothing when broken in creative. */
    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        if ((metadata & 8) != 0 || world.getBlock(x, y, z) == this && (world.getBlockMetadata(x, y, z) & 8) != 0) return drops;
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntitySkull skull)) return drops;
        ItemStack stack = new ItemStack(Items.skull, 1, skull.getSkullType());
        if (skull.getSkullType() == 3 && skull.func_152108_a() != null) {
            stack.setTagCompound(new NBTTagCompound());
            NBTTagCompound owner = new NBTTagCompound();
            legacyconnector.v1_7_10.net.minecraft.nbt.NBTUtil.writeGameProfileToNBT(owner, skull.func_152108_a());
            stack.getTagCompound().setTag("SkullOwner", owner);
        }
        drops.add(stack);
        return drops;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Items.skull;
    }

    /**
     * {@code func_149965_a}: builds a wither when a wither skeleton skull completes the pattern (three such skulls in a row on a
     * T of soul sand). The vanilla skull uses the modern check (which also triggers the advancement).
     */
    public void makeWither(World world, int x, int y, int z, TileEntitySkull skull) {
        var level = VanillaOps.server(world);
        if (level == null || world.isRemote || y < 2) return;
        if (mcc$vanilla != null) {
            net.minecraft.world.level.block.WitherSkullBlock.checkSpawn(level, VanillaOps.pos(x, y, z));
            return;
        }
        if (skull == null || skull.getSkullType() != 1 || level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        for (int axis = 0; axis < 2; ++axis) {
            int dx = axis == 1 ? 1 : 0;
            int dz = axis == 0 ? 1 : 0;
            for (int l = -2; l <= 0; ++l) {
                int bx = x + dx * l;
                int bz = z + dz * l;
                if (world.getBlock(bx, y - 1, bz) != Blocks.soul_sand || world.getBlock(bx + dx, y - 1, bz + dz) != Blocks.soul_sand
                        || world.getBlock(bx + dx, y - 2, bz + dz) != Blocks.soul_sand || world.getBlock(bx + 2 * dx, y - 1, bz + 2 * dz) != Blocks.soul_sand
                        || !func_149966_a(world, bx, y, bz, 1) || !func_149966_a(world, bx + dx, y, bz + dz, 1)
                        || !func_149966_a(world, bx + 2 * dx, y, bz + 2 * dz, 1)) {
                    continue;
                }
                for (int i = 0; i < 3; ++i) world.setBlockMetadataWithNotify(bx + i * dx, y, bz + i * dz, 8, 2);
                for (int i = 0; i < 3; ++i) {
                    world.setBlock(bx + i * dx, y, bz + i * dz, Blocks.air, 0, 2);
                    world.setBlock(bx + i * dx, y - 1, bz + i * dz, Blocks.air, 0, 2);
                }
                world.setBlock(bx + dx, y - 2, bz + dz, Blocks.air, 0, 2);
                var wither = net.minecraft.world.entity.EntityType.WITHER.create(level);
                if (wither != null) {
                    float yaw = axis == 0 ? 90.0F : 0.0F;
                    wither.moveTo(bx + dx + 0.5D, y - 1.45D, bz + dz + 0.5D, yaw, 0.0F);
                    wither.yBodyRot = yaw;
                    wither.makeInvulnerable();
                    level.addFreshEntity(wither);
                }
                for (int i = 0; i < 120; ++i) {
                    world.spawnParticle("snowballpoof", bx + dx + world.rand.nextDouble(), y - 2 + world.rand.nextDouble() * 3.9D,
                            bz + dz + world.rand.nextDouble(), 0.0D, 0.0D, 0.0D);
                }
                for (int i = 0; i < 3; ++i) {
                    world.notifyBlockChange(bx + i * dx, y, bz + i * dz, Blocks.air);
                    world.notifyBlockChange(bx + i * dx, y - 1, bz + i * dz, Blocks.air);
                }
                world.notifyBlockChange(bx + dx, y - 2, bz + dz, Blocks.air);
                return;
            }
        }
    }

    /** Whether the block at the position is this skull block with a head of that type. */
    private boolean func_149966_a(World world, int x, int y, int z, int type) {
        if (world.getBlock(x, y, z) != this) return false;
        TileEntity te = world.getTileEntity(x, y, z);
        return te instanceof TileEntitySkull skull && skull.getSkullType() == type;
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return Blocks.soul_sand.getBlockTextureFromSide(side);
    }

    @Override
    public String getItemIconName() {
        return getTextureName() + "_skeleton";
    }
}
