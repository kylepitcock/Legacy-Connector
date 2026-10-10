package mccompat.v1_7_10.net.minecraft.entity;

import java.util.Random;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.util.DamageSource;
import mccompat.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public abstract class EntityLivingBase extends Entity {
    @Deprecated
    public float rotationYawHead;
    @Deprecated
    public float renderYawOffset;
    @Deprecated
    public int hurtTime;
    @Deprecated
    public int deathTime;
    @Deprecated
    public float prevHealth;
    private final Random mcc$rng = new Random();

    public EntityLivingBase(World world) {
        super(world);
    }

    protected EntityLivingBase(LivingEntity modern) {
        super(modern);
    }

    public LivingEntity mcc$living() {
        return (LivingEntity) mcc$entity;
    }

    public static float mcc$get$rotationYawHead(EntityLivingBase e) {
        return e.mcc$entity == null ? 0 : e.mcc$living().getYHeadRot();
    }

    public static void mcc$set$rotationYawHead(EntityLivingBase e, float v) {
        if (e.mcc$entity != null) e.mcc$living().setYHeadRot(v);
    }

    public static float mcc$get$renderYawOffset(EntityLivingBase e) {
        return e.mcc$entity == null ? 0 : e.mcc$living().yBodyRot;
    }

    public static void mcc$set$renderYawOffset(EntityLivingBase e, float v) {
        if (e.mcc$entity != null) e.mcc$living().yBodyRot = v;
    }

    public static int mcc$get$hurtTime(EntityLivingBase e) {
        return e.mcc$entity == null ? 0 : e.mcc$living().hurtTime;
    }

    public static void mcc$set$hurtTime(EntityLivingBase e, int v) {
        if (e.mcc$entity != null) e.mcc$living().hurtTime = v;
    }

    public static int mcc$get$deathTime(EntityLivingBase e) {
        return e.mcc$entity == null ? 0 : e.mcc$living().deathTime;
    }

    public static void mcc$set$deathTime(EntityLivingBase e, int v) {
        if (e.mcc$entity != null) e.mcc$living().deathTime = v;
    }

    public static float mcc$get$prevHealth(EntityLivingBase e) {
        return e.getHealth();
    }

    public static void mcc$set$prevHealth(EntityLivingBase e, float v) {
    }

    public float getHealth() {
        return mcc$entity == null ? 0 : mcc$living().getHealth();
    }

    public void setHealth(float health) {
        if (mcc$entity != null) mcc$living().setHealth(health);
    }

    public final float getMaxHealth() {
        return mcc$entity == null ? 20 : mcc$living().getMaxHealth();
    }

    public void heal(float amount) {
        if (mcc$entity != null) mcc$living().heal(amount);
    }

    public float getAbsorptionAmount() {
        return mcc$entity == null ? 0 : mcc$living().getAbsorptionAmount();
    }

    public void setAbsorptionAmount(float amount) {
        if (mcc$entity != null) mcc$living().setAbsorptionAmount(amount);
    }

    public Random getRNG() {
        return mcc$rng;
    }

    public boolean isChild() {
        return mcc$entity != null && mcc$living().isBaby();
    }

    public int getAge() {
        return mcc$entity == null ? 0 : mcc$living().getNoActionTime();
    }

    public boolean isEntityUndead() {
        return mcc$entity != null && mcc$living().isInvertedHealAndHarm();
    }

    public boolean isOnLadder() {
        return mcc$entity != null && mcc$living().onClimbable();
    }

    public boolean isPlayerSleeping() {
        return mcc$entity != null && mcc$living().isSleeping();
    }

    public int getTotalArmorValue() {
        return mcc$entity == null ? 0 : mcc$living().getArmorValue();
    }

    public ItemStack getHeldItem() {
        return mcc$entity == null ? null : ItemStack.mcc$wrap(mcc$living().getMainHandItem());
    }

    /** 0 = held item, 1-4 = armor from boots to helmet. */
    public ItemStack getEquipmentInSlot(int slot) {
        if (mcc$entity == null) return null;
        return ItemStack.mcc$wrap(mcc$living().getItemBySlot(mcc$slot(slot)));
    }

    public void setCurrentItemOrArmor(int slot, ItemStack stack) {
        if (mcc$entity != null) mcc$living().setItemSlot(mcc$slot(slot), ItemStack.mcc$toModern(stack));
    }

    public ItemStack[] getLastActiveItems() {
        ItemStack[] out = new ItemStack[5];
        for (int i = 0; i < 5; i++) out[i] = getEquipmentInSlot(i);
        return out;
    }

    protected static EquipmentSlot mcc$slot(int legacy) {
        return switch (legacy) {
            case 1 -> EquipmentSlot.FEET;
            case 2 -> EquipmentSlot.LEGS;
            case 3 -> EquipmentSlot.CHEST;
            case 4 -> EquipmentSlot.HEAD;
            default -> EquipmentSlot.MAINHAND;
        };
    }

    public void setPositionAndUpdate(double x, double y, double z) {
        if (mcc$entity != null) mcc$entity.teleportTo(x, y, z);
        else setPosition(x, y, z);
    }

    public boolean canEntityBeSeen(Entity other) {
        return mcc$entity != null && other.mcc$entity != null && mcc$living().hasLineOfSight(other.mcc$entity);
    }

    public void swingItem() {
        if (mcc$entity != null) mcc$living().swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    public EntityLivingBase getAITarget() {
        if (mcc$entity == null) return null;
        var t = mcc$living().getLastHurtByMob();
        return t == null ? null : (EntityLivingBase) org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge.wrap(t);
    }

    public EntityLivingBase getLastAttacker() {
        if (mcc$entity == null) return null;
        var t = mcc$living().getLastHurtMob();
        return t == null ? null : (EntityLivingBase) org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge.wrap(t);
    }

    public void knockBack(Entity source, float strength, double dx, double dz) {
        if (mcc$entity != null) mcc$living().knockback(0.4F, dx, dz);
    }

    public void renderBrokenItemStack(ItemStack stack) {
        if (mcc$entity != null && stack != null) mcc$living().playSound(net.minecraft.sounds.SoundEvents.ITEM_BREAK, 0.8F, 0.8F);
    }

    public boolean isPotionActive(int potionId) {
        return mcc$entity != null && org.kylepitcock.mccompat.v1_7_10.bridge.PotionBridge.isActive(mcc$living(), potionId);
    }

    public void removePotionEffect(int potionId) {
        if (mcc$entity != null) org.kylepitcock.mccompat.v1_7_10.bridge.PotionBridge.remove(mcc$living(), potionId);
    }

    public void clearActivePotions() {
        if (mcc$entity != null) mcc$living().removeAllEffects();
    }

    public void addPotionEffect(mccompat.v1_7_10.net.minecraft.potion.PotionEffect effect) {
        if (mcc$entity != null && effect != null) mcc$living().addEffect(effect.mcc$modern());
    }

    public boolean attackEntityAsMob(Entity target) {
        return mcc$entity != null && target.mcc$entity != null && mcc$living().doHurtTarget(target.mcc$entity);
    }

    public void onDeath(DamageSource source) {
    }

    public void onLivingUpdate() {
    }

    public boolean isClientWorld() {
        return mcc$entity != null && mcc$entity.level().isClientSide();
    }
}
