package legacyconnector.v1_12_2.net.minecraft.util;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;

/** The cause of damage: a type name plus flags (fire, magic, unblockable, ...). Becomes the modern damage source of the same kind. */
public class DamageSource {
    public static final DamageSource IN_FIRE = new DamageSource("inFire").setFireDamage();
    public static final DamageSource LIGHTNING_BOLT = new DamageSource("lightningBolt");
    public static final DamageSource ON_FIRE = new DamageSource("onFire").setDamageBypassesArmor().setFireDamage();
    public static final DamageSource LAVA = new DamageSource("lava").setFireDamage();
    public static final DamageSource HOT_FLOOR = new DamageSource("hotFloor").setFireDamage();
    public static final DamageSource IN_WALL = new DamageSource("inWall").setDamageBypassesArmor();
    public static final DamageSource CRAMMING = new DamageSource("cramming").setDamageBypassesArmor();
    public static final DamageSource DROWN = new DamageSource("drown").setDamageBypassesArmor();
    public static final DamageSource STARVE = new DamageSource("starve").setDamageBypassesArmor().setDamageIsAbsolute();
    public static final DamageSource CACTUS = new DamageSource("cactus");
    public static final DamageSource FALL = new DamageSource("fall").setDamageBypassesArmor();
    public static final DamageSource FLY_INTO_WALL = new DamageSource("flyIntoWall").setDamageBypassesArmor();
    public static final DamageSource OUT_OF_WORLD = new DamageSource("outOfWorld").setDamageBypassesArmor().setDamageAllowedInCreativeMode();
    public static final DamageSource GENERIC = new DamageSource("generic").setDamageBypassesArmor();
    public static final DamageSource MAGIC = new DamageSource("magic").setDamageBypassesArmor().setMagicDamage();
    public static final DamageSource WITHER = new DamageSource("wither").setDamageBypassesArmor();
    public static final DamageSource ANVIL = new DamageSource("anvil");
    public static final DamageSource FALLING_BLOCK = new DamageSource("fallingBlock");
    public static final DamageSource DRAGON_BREATH = new DamageSource("dragonBreath").setDamageBypassesArmor();
    public static final DamageSource FIREWORKS = new DamageSource("fireworks").setExplosion();

    private boolean isUnblockable;
    private boolean isDamageAllowedInCreativeMode;
    private boolean damageIsAbsolute;
    private float hungerDamage = 0.1F;
    private boolean fireDamage;
    private boolean projectile;
    private boolean difficultyScaled;
    private boolean magicDamage;
    private boolean explosion;
    public String damageType;

    public DamageSource(String type) {
        this.damageType = type;
    }

    public static DamageSource causeMobDamage(EntityLivingBase mob) {
        return new EntityDamageSource("mob", mob);
    }

    public static DamageSource causeIndirectDamage(Entity source, EntityLivingBase indirect) {
        return new EntityDamageSourceIndirect("mob", source, indirect);
    }

    public static DamageSource causePlayerDamage(EntityPlayer player) {
        return new EntityDamageSource("player", player);
    }

    public static DamageSource causeThrownDamage(Entity source, Entity thrower) {
        return new EntityDamageSourceIndirect("thrown", source, thrower).setProjectile();
    }

    public static DamageSource causeIndirectMagicDamage(Entity source, Entity indirect) {
        return new EntityDamageSourceIndirect("indirectMagic", source, indirect).setDamageBypassesArmor().setMagicDamage();
    }

    public static DamageSource causeThornsDamage(Entity source) {
        return new EntityDamageSource("thorns", source).setIsThornsDamage().setMagicDamage();
    }

    public static DamageSource causeExplosionDamage(EntityLivingBase source) {
        return source != null ? new EntityDamageSource("explosion.player", source).setDifficultyScaled().setExplosion()
                : new DamageSource("explosion").setDifficultyScaled().setExplosion();
    }

    public boolean isProjectile() {
        return projectile;
    }

    public DamageSource setProjectile() {
        this.projectile = true;
        return this;
    }

    public boolean isExplosion() {
        return explosion;
    }

    public DamageSource setExplosion() {
        this.explosion = true;
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

    public Entity getImmediateSource() {
        return getTrueSource();
    }

    public Entity getTrueSource() {
        return null;
    }

    public DamageSource setDamageBypassesArmor() {
        this.isUnblockable = true;
        this.hungerDamage = 0.0F;
        return this;
    }

    public DamageSource setDamageAllowedInCreativeMode() {
        this.isDamageAllowedInCreativeMode = true;
        return this;
    }

    public DamageSource setDamageIsAbsolute() {
        this.damageIsAbsolute = true;
        this.hungerDamage = 0.0F;
        return this;
    }

    public DamageSource setFireDamage() {
        this.fireDamage = true;
        return this;
    }

    public boolean isFireDamage() {
        return fireDamage;
    }

    public String getDamageType() {
        return damageType;
    }

    public DamageSource setDifficultyScaled() {
        this.difficultyScaled = true;
        return this;
    }

    public boolean isDifficultyScaled() {
        return difficultyScaled;
    }

    public boolean isMagicDamage() {
        return magicDamage;
    }

    public DamageSource setMagicDamage() {
        this.magicDamage = true;
        return this;
    }

    public boolean isCreativePlayer() {
        Entity e = getTrueSource();
        return e != null && e.mcc$entity instanceof net.minecraft.world.entity.player.Player p && p.getAbilities().instabuild;
    }

    /** The modern damage source of the same kind (vanilla types by name; mod types by their flags), for hurting modern entities. */
    public net.minecraft.world.damagesource.DamageSource mcc$modern(net.minecraft.world.level.Level level) {
        var sources = level.damageSources();
        Entity trueSource = getTrueSource();
        Entity direct = getImmediateSource();
        var attacker = trueSource == null ? null : trueSource.mcc$entity;
        var directModern = direct == null ? null : direct.mcc$entity;
        switch (damageType) {
            case "inFire": return sources.inFire();
            case "lightningBolt": return sources.lightningBolt();
            case "onFire": return sources.onFire();
            case "lava": return sources.lava();
            case "hotFloor": return sources.hotFloor();
            case "inWall": return sources.inWall();
            case "cramming": return sources.cramming();
            case "drown": return sources.drown();
            case "starve": return sources.starve();
            case "cactus": return sources.cactus();
            case "fall": return sources.fall();
            case "flyIntoWall": return sources.flyIntoWall();
            case "outOfWorld": return sources.fellOutOfWorld();
            case "generic": return sources.generic();
            case "magic": return sources.magic();
            case "wither": return sources.wither();
            case "dragonBreath": return sources.dragonBreath();
            case "anvil": return sources.anvil(directModern);
            case "fallingBlock": return sources.fallingBlock(directModern);
            default: break;
        }
        if (attacker instanceof net.minecraft.world.entity.player.Player p && !projectile && !magicDamage) return sources.playerAttack(p);
        if (attacker instanceof net.minecraft.world.entity.LivingEntity l && !projectile && !magicDamage && !explosion) return sources.mobAttack(l);
        if (magicDamage) return directModern != null ? sources.indirectMagic(directModern, attacker) : sources.magic();
        if (explosion) return sources.explosion(directModern, attacker);
        if (fireDamage) return isUnblockable ? sources.onFire() : sources.inFire();
        if (projectile && directModern != null) return sources.thrown(directModern, attacker);
        return sources.generic();
    }
}
