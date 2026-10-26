package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps;

/**
 * A pumpkin or jack o'lantern, ported from 1.7.10: metadata 0-3 is the direction its face looks (south, west, north, east);
 * placed on two snow blocks it becomes a snow golem, on a T of iron blocks an iron golem.
 */
public class BlockPumpkin extends BlockDirectional {
    /** Lit (jack o'lantern). */
    private boolean field_149985_a;
    /** Top texture. */
    private IIcon field_149984_b;
    /** Face texture. */
    private IIcon field_149986_M;

    public BlockPumpkin(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.field_149985_a = info.legacyName().endsWith("lit_pumpkin");
    }

    protected BlockPumpkin(boolean lit) {
        super(Material.gourd);
        setTickRandomly(true);
        this.field_149985_a = lit;
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabBlock);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (field_149984_b == null && mcc$vanilla != null) return super.getIcon(side, meta);
        if (side == 1 || side == 0) return field_149984_b;
        boolean face = meta == 2 && side == 2 || meta == 3 && side == 5 || meta == 0 && side == 3 || meta == 1 && side == 4;
        return face ? field_149986_M : blockIcon;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        if (world.isRemote) return;
        if (world.getBlock(x, y - 1, z) == Blocks.snow && world.getBlock(x, y - 2, z) == Blocks.snow) {
            world.setBlock(x, y, z, Blocks.air, 0, 2);
            world.setBlock(x, y - 1, z, Blocks.air, 0, 2);
            world.setBlock(x, y - 2, z, Blocks.air, 0, 2);
            mcc$spawnGolem(world, net.minecraft.world.entity.EntityType.SNOW_GOLEM, x + 0.5D, y - 1.95D, z + 0.5D);
            for (int i = 0; i < 120; ++i) {
                world.spawnParticle("snowshovel", x + world.rand.nextDouble(), y - 2 + world.rand.nextDouble() * 2.5D, z + world.rand.nextDouble(), 0.0D, 0.0D, 0.0D);
            }
            world.notifyBlockChange(x, y, z, Blocks.air);
            world.notifyBlockChange(x, y - 1, z, Blocks.air);
            world.notifyBlockChange(x, y - 2, z, Blocks.air);
        } else if (world.getBlock(x, y - 1, z) == Blocks.iron_block && world.getBlock(x, y - 2, z) == Blocks.iron_block) {
            boolean alongX = world.getBlock(x - 1, y - 1, z) == Blocks.iron_block && world.getBlock(x + 1, y - 1, z) == Blocks.iron_block;
            boolean alongZ = world.getBlock(x, y - 1, z - 1) == Blocks.iron_block && world.getBlock(x, y - 1, z + 1) == Blocks.iron_block;
            if (!alongX && !alongZ) return;
            world.setBlock(x, y, z, Blocks.air, 0, 2);
            world.setBlock(x, y - 1, z, Blocks.air, 0, 2);
            world.setBlock(x, y - 2, z, Blocks.air, 0, 2);
            if (alongX) {
                world.setBlock(x - 1, y - 1, z, Blocks.air, 0, 2);
                world.setBlock(x + 1, y - 1, z, Blocks.air, 0, 2);
            } else {
                world.setBlock(x, y - 1, z - 1, Blocks.air, 0, 2);
                world.setBlock(x, y - 1, z + 1, Blocks.air, 0, 2);
            }
            var golem = mcc$spawnGolem(world, net.minecraft.world.entity.EntityType.IRON_GOLEM, x + 0.5D, y - 1.95D, z + 0.5D);
            if (golem != null) golem.setPlayerCreated(true);
            for (int i = 0; i < 120; ++i) {
                world.spawnParticle("snowballpoof", x + world.rand.nextDouble(), y - 2 + world.rand.nextDouble() * 3.9D, z + world.rand.nextDouble(), 0.0D, 0.0D, 0.0D);
            }
            world.notifyBlockChange(x, y, z, Blocks.air);
            world.notifyBlockChange(x, y - 1, z, Blocks.air);
            world.notifyBlockChange(x, y - 2, z, Blocks.air);
            if (alongX) {
                world.notifyBlockChange(x - 1, y - 1, z, Blocks.air);
                world.notifyBlockChange(x + 1, y - 1, z, Blocks.air);
            } else {
                world.notifyBlockChange(x, y - 1, z - 1, Blocks.air);
                world.notifyBlockChange(x, y - 1, z + 1, Blocks.air);
            }
        }
    }

    /** Spawns a modern golem at the position (facing south, as 1.7.10 placed it). */
    private static <T extends net.minecraft.world.entity.Mob> T mcc$spawnGolem(World world, net.minecraft.world.entity.EntityType<T> type, double x, double y, double z) {
        var level = VanillaOps.server(world);
        if (level == null) return null;
        T golem = type.create(level);
        if (golem == null) return null;
        golem.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(golem);
        return golem;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.getBlock(x, y, z).isReplaceable(world, x, y, z) && World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int facing = MathHelper.floor_double(Entity.mcc$get$rotationYaw(placer) * 4.0F / 360.0F + 2.5D) & 3;
        world.setBlockMetadataWithNotify(x, y, z, facing, 2);
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        field_149986_M = register.registerIcon(getTextureName() + "_face_" + (field_149985_a ? "on" : "off"));
        field_149984_b = register.registerIcon(getTextureName() + "_top");
        blockIcon = register.registerIcon(getTextureName() + "_side");
    }
}
