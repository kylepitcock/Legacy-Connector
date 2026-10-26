package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.Explosion;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps;

/**
 * TNT, ported from 1.7.10: metadata bit 1 means "explode when broken"; redstone, flint and steel, burning arrows and explosions
 * prime it. The primed TNT is the modern {@code PrimedTnt} entity (80 ticks, or 10-29 when set off by another explosion).
 */
public class BlockTNT extends Block {
    /** Top texture. */
    private IIcon field_150116_a;
    /** Bottom texture. */
    private IIcon field_150115_b;

    public BlockTNT(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockTNT() {
        super(Material.tnt);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabRedstone);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (field_150116_a == null && mcc$vanilla != null) return super.getIcon(side, meta);
        return side == 0 ? field_150115_b : side == 1 ? field_150116_a : blockIcon;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        if (world.isBlockIndirectlyGettingPowered(x, y, z)) {
            onBlockDestroyedByPlayer(world, x, y, z, 1);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isBlockIndirectlyGettingPowered(x, y, z)) {
            onBlockDestroyedByPlayer(world, x, y, z, 1);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion explosion) {
        if (world.isRemote) return;
        var level = VanillaOps.live(world);
        if (level == null) return;
        var tnt = new net.minecraft.world.entity.item.PrimedTnt(level, x + 0.5D, y, z + 0.5D, mcc$owner(explosion == null ? null : explosion.getExplosivePlacedBy()));
        tnt.setFuse(level.random.nextInt(20) + 10);
        level.addFreshEntity(tnt);
    }

    @Override
    public void onBlockDestroyedByPlayer(World world, int x, int y, int z, int meta) {
        func_150114_a(world, x, y, z, meta, null);
    }

    /** Primes the TNT at the position when {@code meta} has bit 1 set ({@code igniter} is credited for the explosion). */
    public void func_150114_a(World world, int x, int y, int z, int meta, EntityLivingBase igniter) {
        if (world == null || world.isRemote || (meta & 1) != 1) return;
        var level = VanillaOps.live(world);
        if (level == null) return;
        var tnt = new net.minecraft.world.entity.item.PrimedTnt(level, x + 0.5D, y, z + 0.5D, mcc$owner(igniter));
        level.addFreshEntity(tnt);
        level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), net.minecraft.sounds.SoundEvents.TNT_PRIMED, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static net.minecraft.world.entity.LivingEntity mcc$owner(EntityLivingBase entity) {
        return entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity living ? living : null;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getCurrentEquippedItem();
        if (held != null && held.getItem() == Items.flint_and_steel) {
            func_150114_a(world, x, y, z, 1, player);
            world.setBlockToAir(x, y, z);
            held.damageItem(1, player);
            return true;
        }
        return super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (world.isRemote || entity == null) return;
        if (entity.mcc$entity instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow && arrow.isOnFire()) {
            EntityLivingBase shooter = arrow.getOwner() instanceof net.minecraft.world.entity.LivingEntity living
                    ? (EntityLivingBase) org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(living) : null;
            func_150114_a(world, x, y, z, 1, shooter);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public boolean canDropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(getTextureName() + "_side");
        field_150116_a = register.registerIcon(getTextureName() + "_top");
        field_150115_b = register.registerIcon(getTextureName() + "_bottom");
    }
}
