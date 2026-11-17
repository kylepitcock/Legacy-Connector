package legacyconnector.v1_6_4.net.minecraft.src;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.IEntityMultiPart;
import legacyconnector.v1_7_10.net.minecraft.entity.boss.EntityDragonPart;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Minecraft 1.2.5's {@code EntityDragonBase} (Feather: {@code BossEntity}, obfuscated {@code ajn} on the client and {@code xb} on the server): the common
 * superclass of the dragon and of mods' multi-part bosses (Twilight Forest's hydra), which 1.3 folded into {@code EntityDragon}. Read from the real class
 * (Mojang 1.2.5 client jar, {@code javap -c}):
 * <ul>
 * <li>the constructor takes the world and sets the protected int {@code maxHealth} (Feather name; {@code aA}) to 100;</li>
 * <li>{@code getMaxHealth()} returns that field; here it is the layer's legacy maximum-health hook, which the 1.3+ forward bridge points a mod's
 * {@code int getMaxHealth()} override at;</li>
 * <li>{@code takeDamage(part, source, amount)} (a hit on a part; {@code a(dg, md, int)}) calls {@code attackEntityFrom(source, amount)};</li>
 * <li>{@code attackEntityFrom(source, amount)} returns false: the base is not hurt directly, a part forwards the hit;</li>
 * <li>{@code doTakeDamage(source, amount)} (protected {@code e(md, int)}) is the real {@code EntityLiving.attackEntityFrom} (an {@code invokespecial} of the
 * living entity's), for the subclass that applies damage itself.</li>
 * </ul>
 * The member names are Feather's, because that is what the loader names a 1.2.5 reference to an unjoined member; amounts stay {@code int} as in 1.2.5.
 */
public class EntityDragonBase extends EntityLiving implements IEntityMultiPart {
    /** {@code aA}: the maximum health a subclass may set in its constructor; 0 until set (then the default 100 applies). */
    protected int maxHealth;

    public EntityDragonBase(World world) {
        super(world);
        // the real constructor stores 100 here after the living entity's constructor ran
        if (this.maxHealth == 0) this.maxHealth = 100;
    }

    /** The maximum health a 1.2.5 class declared (the field, 100 unless the subclass changed it or overrides {@code getMaxHealth()}). */
    @Override
    public float mcc$legacyMaxHealth() {
        return maxHealth > 0 ? maxHealth : 100;
    }

    /** {@code takeDamage}: damage to one hit box; the real base forwards it to {@code attackEntityFrom}. */
    public boolean takeDamage(EntityDragonPart part, DamageSource source, int amount) {
        return attackEntityFrom(source, (float) amount);
    }

    /** {@code doTakeDamage}: the living entity's own damage handling, for a subclass that applies the hit itself. */
    protected boolean doTakeDamage(DamageSource source, int amount) {
        return super.attackEntityFrom(source, (float) amount);
    }

    /** The base is not hurt directly; its parts forward the damage. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean attackEntityFromPart(EntityDragonPart part, DamageSource source, float damage) {
        return takeDamage(part, source, (int) damage);
    }

    @Override
    public World func_82194_d() {
        return mcc$world();
    }
}
