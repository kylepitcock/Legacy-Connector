package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionType;
import legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 lingering potion cloud: {@code new EntityAreaEffectCloud(world, x, y, z)} is a modern cloud, and its setters (radius, duration,
 * wait time, growth, particle, potion, effects, colour, owner) configure the modern one.
 */
public class EntityAreaEffectCloud extends Entity {
    private float mcc$radius = 0.5F;
    private int mcc$duration = 600;
    private int mcc$color;
    private boolean mcc$colorExplicit;
    private PotionType mcc$potionType;
    private final java.util.List<PotionEffect> mcc$added = new java.util.ArrayList<>();

    public EntityAreaEffectCloud(World world) {
        super(world);
        mcc$set$noClip(this, true);
        mcc$set$isImmuneToFire(this, true);
        // a view of a cloud already in the world keeps its radius
        if (mcc$cloud() == null || !mcc$entity.isAddedToLevel()) setRadius(3.0F);
    }

    public EntityAreaEffectCloud(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
    }

    protected EntityAreaEffectCloud(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private net.minecraft.world.entity.AreaEffectCloud mcc$cloud() {
        return mcc$entity instanceof net.minecraft.world.entity.AreaEffectCloud c ? c : null;
    }

    @Override
    protected void entityInit() {
    }

    public void setRadius(float radius) {
        mcc$radius = radius;
        var c = mcc$cloud();
        if (c != null) c.setRadius(radius);
    }

    public float getRadius() {
        var c = mcc$cloud();
        return c != null ? c.getRadius() : mcc$radius;
    }

    public void setDuration(int duration) {
        mcc$duration = duration;
        var c = mcc$cloud();
        if (c != null) c.setDuration(duration);
    }

    public int getDuration() {
        var c = mcc$cloud();
        return c != null ? c.getDuration() : mcc$duration;
    }

    public void setWaitTime(int ticks) {
        var c = mcc$cloud();
        if (c != null) c.setWaitTime(ticks);
    }

    public void setRadiusOnUse(float radius) {
        var c = mcc$cloud();
        if (c != null) c.setRadiusOnUse(radius);
    }

    public void setRadiusPerTick(float radius) {
        var c = mcc$cloud();
        if (c != null) c.setRadiusPerTick(radius);
    }

    public void setDurationOnUse(int duration) {
        var c = mcc$cloud();
        if (c != null) c.setDurationOnUse(duration);
    }

    public void setParticle(EnumParticleTypes particle) {
        var c = mcc$cloud();
        if (c == null || particle == null) return;
        var options = particle.mcc$modern(0.0D, 0.0D, 0.0D);
        if (options != null) c.setParticle(options);
    }

    public EnumParticleTypes getParticle() {
        return EnumParticleTypes.SPELL_MOB;
    }

    public void setPotion(PotionType potion) {
        var c = mcc$cloud();
        var modern = potion == null ? null : potion.mcc$modern();
        if (c != null && modern != null) c.setPotionContents(new net.minecraft.world.item.alchemy.PotionContents(modern));
        mcc$potionType = potion;
        mcc$refreshColor();
    }

    /**
     * 1.12 mixed the cloud's colour from its potion's and its own effects (those that show particles) whenever they changed, unless a
     * colour was set. The modern cloud mixes it itself, but from the converted effects, and drops to the default water blue when
     * none converted (Scape and Run Parasites' Call of the Hive clouds, whose effect hides its particles: blue swirls instead of the
     * 1.12 colour, #110).
     */
    private void mcc$refreshColor() {
        if (mcc$colorExplicit) return;
        java.util.List<PotionEffect> all = new java.util.ArrayList<>();
        if (mcc$potionType != null) all.addAll(mcc$potionType.getEffects());
        all.addAll(mcc$added);
        if (all.isEmpty()) return;
        mcc$applyColor(legacyconnector.v1_12_2.net.minecraft.potion.PotionUtils.getPotionColorFromEffectList(all));
    }

    /** Phantom field: {@link #mcc$get$effects}. */
    @Deprecated
    private java.util.List<PotionEffect> effects;
    private final java.util.List<PotionEffect> mcc$localEffects = new java.util.ArrayList<>();

    /** effects: the cloud's own effects besides its potion's (the modern cloud's custom effects). */
    public static java.util.List<PotionEffect> mcc$get$effects(EntityAreaEffectCloud e) {
        var c = e.mcc$cloud();
        if (c == null) return e.mcc$localEffects;
        java.util.List<PotionEffect> out = new java.util.ArrayList<>();
        var tag = new net.minecraft.nbt.CompoundTag();
        c.saveWithoutId(tag);
        var contents = net.minecraft.world.item.alchemy.PotionContents.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("potion_contents")).result().orElse(null);
        if (contents != null) for (var m : contents.customEffects()) out.add(PotionEffect.mcc$fromModern(m));
        return out;
    }

    public static void mcc$set$effects(EntityAreaEffectCloud e, java.util.List<PotionEffect> v) {
        e.mcc$localEffects.clear();
        if (v != null) e.mcc$localEffects.addAll(v);
        var c = e.mcc$cloud();
        if (c == null) return;
        var tag = new net.minecraft.nbt.CompoundTag();
        c.saveWithoutId(tag);
        var contents = net.minecraft.world.item.alchemy.PotionContents.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("potion_contents")).result()
                .orElse(net.minecraft.world.item.alchemy.PotionContents.EMPTY);
        java.util.List<net.minecraft.world.effect.MobEffectInstance> modern = new java.util.ArrayList<>();
        if (v != null) for (PotionEffect p : v) if (p != null && p.mcc$toModern() != null) modern.add(p.mcc$toModern());
        c.setPotionContents(new net.minecraft.world.item.alchemy.PotionContents(contents.potion(), contents.customColor(), modern));
        e.mcc$added.clear();
        if (v != null) e.mcc$added.addAll(v);
        e.mcc$refreshColor();
    }

    public void addEffect(PotionEffect effect) {
        var c = mcc$cloud();
        var modern = effect == null ? null : effect.mcc$toModern();
        if (c != null && modern != null) c.addEffect(modern);
        else if (c == null && effect != null) mcc$localEffects.add(effect);
        if (effect != null) mcc$added.add(effect);
        mcc$refreshColor();
    }

    /** A custom cloud colour (the modern cloud keeps the colour with its potion contents). */
    public void setColor(int color) {
        mcc$colorExplicit = true;
        mcc$applyColor(color);
    }

    private void mcc$applyColor(int color) {
        mcc$color = color;
        var c = mcc$cloud();
        if (c == null) return;
        var tag = new net.minecraft.nbt.CompoundTag();
        c.saveWithoutId(tag);
        var contents = tag.getCompound("potion_contents");
        contents.putInt("custom_color", color);
        tag.put("potion_contents", contents);
        c.load(tag);
    }

    public int getColor() {
        return mcc$color;
    }

    public void setOwner(EntityLivingBase owner) {
        var c = mcc$cloud();
        if (c != null) c.setOwner(owner != null && owner.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l ? l : null);
    }

    public EntityLivingBase getOwner() {
        var c = mcc$cloud();
        return c == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(c.getOwner()) instanceof EntityLivingBase l ? l : null;
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }
}
