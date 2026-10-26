package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.world.Explosion;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.vehicle.MinecartTNT;

/**
 * The TNT minecart: primed by an activator rail (80 ticks), it explodes when the fuse runs out, when it crashes into something
 * fast, falls three blocks or more, or is broken by fire or an explosion. Constructing this exact class makes a vanilla TNT cart.
 */
public class EntityMinecartTNT extends EntityMinecart {
    private int minecartTNTFuse = -1;

    public EntityMinecartTNT(World world) {
        super(world);
    }

    public EntityMinecartTNT(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    private MinecartTNT mcc$tnt() {
        return mcc$entity instanceof MinecartTNT t ? t : null;
    }

    @Override
    public int getMinecartType() {
        return 3;
    }

    @Override
    public Block getDefaultDisplayTile() {
        return Blocks.tnt;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (mcc$tnt() != null || mcc$entity == null) return;
        if (minecartTNTFuse > 0) {
            --minecartTNTFuse;
            mcc$world().spawnParticle("smoke", mcc$get$posX(this), mcc$get$posY(this) + 0.5D, mcc$get$posZ(this), 0.0D, 0.0D, 0.0D);
        } else if (minecartTNTFuse == 0) {
            explodeCart(mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionZ(this) * mcc$get$motionZ(this));
        }
        if (mcc$get$isCollidedHorizontally(this)) {
            double speed = mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionZ(this) * mcc$get$motionZ(this);
            if (speed >= 0.009999999776482582D) explodeCart(speed);
        }
    }

    /** Breaks the cart (dropping the TNT unless an explosion broke it); fire, explosions and speed set it off. */
    @Override
    public void killMinecart(DamageSource source) {
        super.killMinecart(source);
        double speed = mcc$get$motionX(this) * mcc$get$motionX(this) + mcc$get$motionZ(this) * mcc$get$motionZ(this);
        if (source == null || !source.isExplosion()) entityDropItem(new ItemStack(Blocks.tnt, 1), 0.0F);
        if (source != null && (source.isFireDamage() || source.isExplosion()) || speed >= 0.009999999776482582D) explodeCart(speed);
    }

    /** Explodes with a strength of 4 plus up to 1.5 times the speed (capped at 5). */
    protected void explodeCart(double speedSq) {
        if (mcc$world().isRemote) return;
        double speed = Math.min(Math.sqrt(speedSq), 5.0D);
        mcc$world().createExplosion(this, mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this), (float) (4.0D + rand.nextDouble() * 1.5D * speed), true);
        setDead();
    }

    @Override
    protected void fall(float distance) {
        if (distance >= 3.0F) {
            float f = distance / 10.0F;
            explodeCart(f * f);
        }
        super.fall(distance);
    }

    @Override
    public void onActivatorRailPass(int x, int y, int z, boolean powered) {
        if (powered && minecartTNTFuse < 0) ignite();
    }

    @Override
    public void handleHealthUpdate(byte state) {
        if (state == 10) ignite();
        else super.handleHealthUpdate(state);
    }

    /** Primes the fuse (80 ticks); the server tells the clients and plays the hiss. */
    public void ignite() {
        var tnt = mcc$tnt();
        if (tnt != null) {
            tnt.primeFuse();
            return;
        }
        minecartTNTFuse = 80;
        if (!mcc$world().isRemote) {
            mcc$world().setEntityState(this, (byte) 10);
            mcc$world().playSoundAtEntity(this, "game.tnt.primed", 1.0F, 1.0F);
        }
    }

    /** Ticks left on the fuse (-1 when not primed). */
    public int func_94104_d() {
        var tnt = mcc$tnt();
        return tnt != null ? tnt.getFuse() : minecartTNTFuse;
    }

    public boolean isIgnited() {
        var tnt = mcc$tnt();
        return tnt != null ? tnt.isPrimed() : minecartTNTFuse > -1;
    }

    /** A primed cart's explosion does not break the rail it is on. */
    @Override
    public float getExplosionResistance(Explosion explosion, World world, int x, int y, int z, Block block) {
        if (isIgnited() && (legacyconnector.v1_7_10.net.minecraft.block.BlockRailBase.func_150051_a(block)
                || legacyconnector.v1_7_10.net.minecraft.block.BlockRailBase.isRailBlockAt(world, x, y + 1, z))) {
            return 0.0F;
        }
        return super.getExplosionResistance(explosion, world, x, y, z, block);
    }

    @Override
    public boolean func_145774_a(Explosion explosion, World world, int x, int y, int z, Block block, float power) {
        if (isIgnited() && (legacyconnector.v1_7_10.net.minecraft.block.BlockRailBase.func_150051_a(block)
                || legacyconnector.v1_7_10.net.minecraft.block.BlockRailBase.isRailBlockAt(world, x, y + 1, z))) {
            return false;
        }
        return super.func_145774_a(explosion, world, x, y, z, block, power);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("TNTFuse", 99)) minecartTNTFuse = tag.getInteger("TNTFuse");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("TNTFuse", minecartTNTFuse);
    }
}
