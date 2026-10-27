package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.potion.Potion;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * 1.12.2 living entity over a modern {@link net.minecraft.world.entity.LivingEntity}. Equipment comes back as live stacks:
 * shrinking or damaging a held stack writes it back to the hand it came from, as the shared stack did in 1.12.2.
 */
public abstract class EntityLivingBase extends Entity {
    public EntityLivingBase(World world) {
        super(world);
    }

    protected net.minecraft.world.entity.LivingEntity mcc$living() {
        return mcc$entity instanceof net.minecraft.world.entity.LivingEntity l ? l : null;
    }

    private static EntityLivingBase mcc$living(Entity view) {
        return view instanceof EntityLivingBase l ? l : null;
    }

    static net.minecraft.world.entity.EquipmentSlot mcc$slot(EntityEquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            case OFFHAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
            case FEET -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case LEGS -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case CHEST -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case HEAD -> net.minecraft.world.entity.EquipmentSlot.HEAD;
        };
    }

    static net.minecraft.world.InteractionHand mcc$hand(EnumHand hand) {
        return hand == EnumHand.OFF_HAND ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND;
    }

    // ------------------------------------------------------------------ equipment

    public ItemStack getItemStackFromSlot(EntityEquipmentSlot slot) {
        var l = mcc$living();
        if (l == null || slot == null) return ItemStack.EMPTY;
        var modernSlot = mcc$slot(slot);
        return Stacks1122.fromModern(l.getItemBySlot(modernSlot)).mcc$bind(s -> l.setItemSlot(modernSlot, Stacks1122.toModern(s)));
    }

    public void setItemStackToSlot(EntityEquipmentSlot slot, ItemStack stack) {
        var l = mcc$living();
        if (l != null && slot != null) l.setItemSlot(mcc$slot(slot), Stacks1122.toModern(stack));
    }

    public ItemStack getHeldItem(EnumHand hand) {
        return getItemStackFromSlot(hand == EnumHand.OFF_HAND ? EntityEquipmentSlot.OFFHAND : EntityEquipmentSlot.MAINHAND);
    }

    public ItemStack getHeldItemMainhand() {
        return getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
    }

    public ItemStack getHeldItemOffhand() {
        return getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
    }

    public void setHeldItem(EnumHand hand, ItemStack stack) {
        setItemStackToSlot(hand == EnumHand.OFF_HAND ? EntityEquipmentSlot.OFFHAND : EntityEquipmentSlot.MAINHAND, stack);
    }

    public Iterable<ItemStack> getHeldEquipment() {
        return java.util.List.of(getHeldItemMainhand(), getHeldItemOffhand());
    }

    public Iterable<ItemStack> getArmorInventoryList() {
        return java.util.List.of(getItemStackFromSlot(EntityEquipmentSlot.FEET), getItemStackFromSlot(EntityEquipmentSlot.LEGS),
                getItemStackFromSlot(EntityEquipmentSlot.CHEST), getItemStackFromSlot(EntityEquipmentSlot.HEAD));
    }

    public EnumHand getActiveHand() {
        var l = mcc$living();
        return l != null && l.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
    }

    public void setActiveHand(EnumHand hand) {
        var l = mcc$living();
        if (l != null) l.startUsingItem(mcc$hand(hand));
    }

    public boolean isHandActive() {
        var l = mcc$living();
        return l != null && l.isUsingItem();
    }

    public ItemStack getActiveItemStack() {
        var l = mcc$living();
        return l == null ? ItemStack.EMPTY : Stacks1122.fromModern(l.getUseItem());
    }

    public int getItemInUseCount() {
        var l = mcc$living();
        return l == null ? 0 : l.getUseItemRemainingTicks();
    }

    public void stopActiveHand() {
        var l = mcc$living();
        if (l != null) l.releaseUsingItem();
    }

    public void resetActiveHand() {
        var l = mcc$living();
        if (l != null) l.stopUsingItem();
    }

    public void swingArm(EnumHand hand) {
        var l = mcc$living();
        if (l != null) l.swing(mcc$hand(hand), true);
    }

    public int getTotalArmorValue() {
        var l = mcc$living();
        return l == null ? 0 : l.getArmorValue();
    }

    // ------------------------------------------------------------------ health

    public float getHealth() {
        var l = mcc$living();
        return l == null ? 0 : l.getHealth();
    }

    public void setHealth(float health) {
        var l = mcc$living();
        if (l != null) l.setHealth(health);
    }

    public float getMaxHealth() {
        var l = mcc$living();
        return l == null ? 0 : l.getMaxHealth();
    }

    public void heal(float amount) {
        var l = mcc$living();
        if (l != null) l.heal(amount);
    }

    public float getAbsorptionAmount() {
        var l = mcc$living();
        return l == null ? 0 : l.getAbsorptionAmount();
    }

    public void setAbsorptionAmount(float amount) {
        var l = mcc$living();
        if (l != null) l.setAbsorptionAmount(amount);
    }

    @Override
    public boolean isEntityAlive() {
        var l = mcc$living();
        return l != null ? l.isAlive() && l.getHealth() > 0 : super.isEntityAlive();
    }

    public boolean isChild() {
        var l = mcc$living();
        return l != null && l.isBaby();
    }

    public boolean isOnLadder() {
        var l = mcc$living();
        return l != null && l.onClimbable();
    }

    public boolean isElytraFlying() {
        var l = mcc$living();
        return l != null && l.isFallFlying();
    }

    public boolean isPlayerSleeping() {
        var l = mcc$living();
        return l != null && l.isSleeping();
    }

    public java.util.Random getRNG() {
        return rand;
    }

    public void knockBack(Entity attacker, float strength, double xRatio, double zRatio) {
        var l = mcc$living();
        if (l != null) l.knockback(strength, xRatio, zRatio);
    }

    public EntityLivingBase getRevengeTarget() {
        var l = mcc$living();
        return l == null ? null : mcc$living(mcc$view(l.getLastHurtByMob()));
    }

    public void setRevengeTarget(EntityLivingBase target) {
        var l = mcc$living();
        if (l != null) l.setLastHurtByMob(target == null ? null : target.mcc$living());
    }

    public EntityLivingBase getAttackingEntity() {
        var l = mcc$living();
        return l == null ? null : mcc$living(mcc$view(l.getKillCredit()));
    }

    public EntityLivingBase getLastAttackedEntity() {
        var l = mcc$living();
        return l == null ? null : mcc$living(mcc$view(l.getLastHurtMob()));
    }

    public boolean canEntityBeSeen(Entity other) {
        var l = mcc$living();
        return l != null && other != null && other.mcc$entity != null && l.hasLineOfSight(other.mcc$entity);
    }

    // ------------------------------------------------------------------ potion effects

    public boolean isPotionActive(Potion potion) {
        var l = mcc$living();
        var effect = potion == null ? null : potion.mcc$modern();
        return l != null && effect != null && l.hasEffect(effect);
    }

    public PotionEffect getActivePotionEffect(Potion potion) {
        var l = mcc$living();
        var effect = potion == null ? null : potion.mcc$modern();
        var instance = l == null || effect == null ? null : l.getEffect(effect);
        return instance == null ? null : PotionEffect.mcc$fromModern(instance);
    }

    public java.util.Collection<PotionEffect> getActivePotionEffects() {
        java.util.List<PotionEffect> out = new java.util.ArrayList<>();
        var l = mcc$living();
        if (l != null) {
            for (var e : l.getActiveEffects()) {
                var legacy = PotionEffect.mcc$fromModern(e);
                if (legacy != null) out.add(legacy);
            }
        }
        return out;
    }

    public void addPotionEffect(PotionEffect effect) {
        var l = mcc$living();
        var modern = effect == null ? null : effect.mcc$toModern();
        if (l != null && modern != null) l.addEffect(modern);
    }

    public PotionEffect removeActivePotionEffect(Potion potion) {
        PotionEffect old = getActivePotionEffect(potion);
        removePotionEffect(potion);
        return old;
    }

    public void removePotionEffect(Potion potion) {
        var l = mcc$living();
        var effect = potion == null ? null : potion.mcc$modern();
        if (l != null && effect != null) l.removeEffect(effect);
    }

    public void clearActivePotions() {
        var l = mcc$living();
        if (l != null) l.removeAllEffects();
    }
}
