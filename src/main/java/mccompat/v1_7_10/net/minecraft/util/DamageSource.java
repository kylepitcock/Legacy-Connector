package mccompat.v1_7_10.net.minecraft.util;

import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.level.Level;

/**
 * Damage sources are registry-driven damage types in modern Minecraft. Legacy sources are translated by their
 * damage type string to the closest vanilla damage type; custom mod sources become "generic" with the legacy
 * flags preserved for legacy code that inspects them.
 */
public class DamageSource {
    public static DamageSource inFire = new DamageSource("inFire").setFireDamage();
    public static DamageSource onFire = new DamageSource("onFire").setDamageBypassesArmor().setFireDamage();
    public static DamageSource lava = new DamageSource("lava").setFireDamage();
    public static DamageSource inWall = new DamageSource("inWall").setDamageBypassesArmor();
    public static DamageSource drown = new DamageSource("drown").setDamageBypassesArmor();
    public static DamageSource starve = new DamageSource("starve").setDamageBypassesArmor().setDamageIsAbsolute();
    public static DamageSource cactus = new DamageSource("cactus");
    public static DamageSource fall = new DamageSource("fall").setDamageBypassesArmor();
    public static DamageSource outOfWorld = new DamageSource("outOfWorld").setDamageBypassesArmor().setDamageAllowedInCreativeMode();
    public static DamageSource generic = new DamageSource("generic").setDamageBypassesArmor();
    public static DamageSource magic = new DamageSource("magic").setDamageBypassesArmor().setMagicDamage();
    public static DamageSource wither = new DamageSource("wither").setDamageBypassesArmor();
    public static DamageSource anvil = new DamageSource("anvil");
    public static DamageSource fallingBlock = new DamageSource("fallingBlock");

    public String damageType;
    private boolean isUnblockable;
    private boolean isDamageAllowedInCreativeMode;
    private boolean damageIsAbsolute;
    private float hungerDamage = 0.3F;
    private boolean fireDamage;
    private boolean projectile;
    private boolean difficultyScaled;
    private boolean magicDamage;
    private boolean explosion;
    protected Entity mcc$source;
    protected Entity mcc$direct;

    public DamageSource(String type) {
        this.damageType = type;
    }

    public static DamageSource causeMobDamage(EntityLivingBase mob) {
        return new EntityDamageSource("mob", mob);
    }

    public static DamageSource causePlayerDamage(EntityPlayer player) {
        return new EntityDamageSource("player", player);
    }

    public static DamageSource causeThrownDamage(Entity direct, Entity indirect) {
        return new EntityDamageSourceIndirect("thrown", direct, indirect).setProjectile();
    }

    public static DamageSource causeIndirectMagicDamage(Entity direct, Entity indirect) {
        return new EntityDamageSourceIndirect("indirectMagic", direct, indirect).setDamageBypassesArmor().setMagicDamage();
    }

    public static DamageSource causeThornsDamage(Entity source) {
        return new EntityDamageSource("thorns", source).setMagicDamage();
    }

    public boolean isProjectile() {
        return projectile;
    }

    public DamageSource setProjectile() {
        projectile = true;
        return this;
    }

    public boolean isExplosion() {
        return explosion;
    }

    public DamageSource setExplosion() {
        explosion = true;
        return this;
    }

    public boolean isUnblockable() {
        return isUnblockable;
    }

    public float getHungerDamage() {
        return hungerDamage;
    }

    public boolean canHarmInCreative() {
        return isDamageAllowedInCreativeMode;
    }

    public boolean isDamageAbsolute() {
        return damageIsAbsolute;
    }

    public Entity getSourceOfDamage() {
        return getEntity();
    }

    public Entity getEntity() {
        return null;
    }

    protected DamageSource setDamageBypassesArmor() {
        isUnblockable = true;
        hungerDamage = 0.0F;
        return this;
    }

    protected DamageSource setDamageAllowedInCreativeMode() {
        isDamageAllowedInCreativeMode = true;
        return this;
    }

    protected DamageSource setDamageIsAbsolute() {
        damageIsAbsolute = true;
        hungerDamage = 0.0F;
        return this;
    }

    protected DamageSource setFireDamage() {
        fireDamage = true;
        return this;
    }

    public boolean isFireDamage() {
        return fireDamage;
    }

    public String getDamageType() {
        return damageType;
    }

    public DamageSource setDifficultyScaled() {
        difficultyScaled = true;
        return this;
    }

    public boolean isDifficultyScaled() {
        return difficultyScaled;
    }

    public boolean isMagicDamage() {
        return magicDamage;
    }

    public DamageSource setMagicDamage() {
        magicDamage = true;
        return this;
    }

    public IChatComponent getDeathMessage(EntityLivingBase victim) {
        return new ChatComponentTranslation("death.attack." + damageType, victim.getFormattedCommandSenderName());
    }

    /** Translates to the closest modern damage source. */
    public net.minecraft.world.damagesource.DamageSource mcc$modern(Level level) {
        DamageSources s = level.damageSources();
        var attacker = getEntity() == null ? null : getEntity().mcc$entity;
        var direct = getSourceOfDamage() == null ? null : getSourceOfDamage().mcc$entity;
        if (attacker instanceof net.minecraft.world.entity.player.Player p && !projectile) return s.playerAttack(p);
        if (attacker instanceof net.minecraft.world.entity.LivingEntity l && !projectile && !magicDamage) return s.mobAttack(l);
        if (magicDamage && direct != null) return s.indirectMagic(direct, attacker);
        return switch (damageType) {
            case "inFire" -> s.inFire();
            case "onFire" -> s.onFire();
            case "lava" -> s.lava();
            case "inWall" -> s.inWall();
            case "drown" -> s.drown();
            case "starve" -> s.starve();
            case "cactus" -> s.cactus();
            case "fall" -> s.fall();
            case "outOfWorld" -> s.fellOutOfWorld();
            case "magic" -> s.magic();
            case "wither" -> s.wither();
            case "anvil" -> s.anvil(null);
            case "fallingBlock" -> s.fallingBlock(null);
            default -> fireDamage ? s.inFire() : explosion ? s.explosion(direct, attacker) : s.generic();
        };
    }
}
