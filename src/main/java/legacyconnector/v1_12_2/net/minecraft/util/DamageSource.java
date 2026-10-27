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

    /** The modern source this one was made from (a modern hit handed to legacy code), returned unchanged when it goes back. */
    public net.minecraft.world.damagesource.DamageSource mcc$wrapped;

    /** Modern damage type ids whose 1.12 name differs. */
    private static final java.util.Map<String, String> LEGACY_NAMES = java.util.Map.ofEntries(
            java.util.Map.entry("in_fire", "inFire"), java.util.Map.entry("lightning_bolt", "lightningBolt"), java.util.Map.entry("on_fire", "onFire"),
            java.util.Map.entry("hot_floor", "hotFloor"), java.util.Map.entry("in_wall", "inWall"), java.util.Map.entry("fly_into_wall", "flyIntoWall"),
            java.util.Map.entry("out_of_world", "outOfWorld"), java.util.Map.entry("dragon_breath", "dragonBreath"),
            java.util.Map.entry("falling_anvil", "anvil"), java.util.Map.entry("falling_block", "fallingBlock"), java.util.Map.entry("mob_attack", "mob"),
            java.util.Map.entry("mob_attack_no_aggro", "mob"), java.util.Map.entry("player_attack", "player"), java.util.Map.entry("arrow", "arrow"),
            java.util.Map.entry("trident", "trident"), java.util.Map.entry("fireball", "fireball"), java.util.Map.entry("thrown", "thrown"),
            java.util.Map.entry("indirect_magic", "indirectMagic"), java.util.Map.entry("explosion", "explosion"),
            java.util.Map.entry("player_explosion", "explosion.player"), java.util.Map.entry("sweet_berry_bush", "sweetBerryBush"),
            java.util.Map.entry("freeze", "freeze"), java.util.Map.entry("sting", "sting"), java.util.Map.entry("thorns", "thorns"),
            java.util.Map.entry("falling_stalactite", "fallingStalactite"), java.util.Map.entry("stalagmite", "stalagmite"));

    /** A modern damage source as a 1.12 one: its type by the 1.12 name, its flags from the modern damage tags, its entities as views. */
    public static DamageSource mcc$of(net.minecraft.world.damagesource.DamageSource modern) {
        if (modern == null) return GENERIC;
        var key = modern.typeHolder().unwrapKey().map(k -> k.location()).orElse(null);
        String type = key == null ? modern.getMsgId() : LEGACY_NAMES.getOrDefault(key.getPath(), modern.getMsgId());
        Entity direct = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(modern.getDirectEntity());
        Entity attacker = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(modern.getEntity());
        DamageSource out;
        if (direct != null && attacker != null && direct != attacker) out = new EntityDamageSourceIndirect(type, direct, attacker);
        else if (attacker != null || direct != null) out = new EntityDamageSource(type, attacker != null ? attacker : direct);
        else out = new DamageSource(type);
        if (modern.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) out.setDamageBypassesArmor();
        if (modern.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) out.setDamageAllowedInCreativeMode();
        if (modern.is(net.minecraft.tags.DamageTypeTags.BYPASSES_EFFECTS)) out.setDamageIsAbsolute();
        if (modern.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) out.setFireDamage();
        if (modern.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) out.setProjectile();
        if (modern.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) out.setExplosion();
        if (modern.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) out.setMagicDamage();
        if (modern.scalesWithDifficulty()) out.setDifficultyScaled();
        out.mcc$wrapped = modern;
        return out;
    }

    /** The modern damage source of the same kind (vanilla types by name; mod types by their flags), for hurting modern entities. */
    public net.minecraft.world.damagesource.DamageSource mcc$modern(net.minecraft.world.level.Level level) {
        if (mcc$wrapped != null) return mcc$wrapped;
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
