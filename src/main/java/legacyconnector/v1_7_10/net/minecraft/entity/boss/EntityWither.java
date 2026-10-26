package legacyconnector.v1_7_10.net.minecraft.entity.boss;

import legacyconnector.v1_7_10.net.minecraft.command.IEntitySelector;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureAttribute;
import legacyconnector.v1_7_10.net.minecraft.entity.IRangedAttackMob;
import legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityMob;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10 wither: three heads whose targets are watcher slots 17-19 and whose angles {@link #func_82207_a}/{@link #func_82210_r}
 * give the renderer; slot 20 is the spawn invulnerability timer. A plain wither is the modern boss; mods' withers (Botania's pink
 * wither) keep this state and their own behaviour. The wither's skull-shooting AI is not reproduced for them.
 */
public class EntityWither extends EntityMob implements IBossDisplayData, IRangedAttackMob {
    public float[] field_82220_d = new float[2];
    public float[] field_82221_e = new float[2];
    public float[] field_82217_f = new float[2];
    public float[] field_82218_g = new float[2];
    public int[] field_82223_h = new int[2];
    public int[] field_82224_i = new int[2];
    public int field_82222_j;
    /** Who the wither attacks: anything living that is not undead. */
    public static final IEntitySelector attackEntitySelector = e -> e instanceof EntityLivingBase l
            && l.getCreatureAttribute() != EnumCreatureAttribute.UNDEAD;

    /** View of a modern wither. */
    public EntityWither(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityWither(World world) {
        super(world);
        setHealth(getMaxHealth());
        setSize(0.9F, 4.0F);
        this.isImmuneToFire = true;
        this.experienceValue = 50;
    }

    private net.minecraft.world.entity.boss.wither.WitherBoss mcc$wither() {
        return mcc$entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss w && !mcc$hosted() ? w : null;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(17, Integer.valueOf(0));
        dataWatcher.addObject(18, Integer.valueOf(0));
        dataWatcher.addObject(19, Integer.valueOf(0));
        dataWatcher.addObject(20, Integer.valueOf(0));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(300.0D);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.6000000238418579D);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(40.0D);
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    /** Hosted: turns each side head towards its target (or the body's facing) for the renderer. */
    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!mcc$hosted()) return;
        for (int i = 0; i < 2; i++) {
            field_82218_g[i] = field_82221_e[i];
            field_82217_f[i] = field_82220_d[i];
            Entity target = getWatchedTargetId(i + 1) > 0 ? mcc$world().getEntityByID(getWatchedTargetId(i + 1)) : null;
            if (target != null) {
                double hx = func_82214_u(i + 1), hy = func_82208_v(i + 1), hz = func_82213_w(i + 1);
                double dx = mcc$get$posX(target) - hx;
                double dy = mcc$get$posY(target) + target.getEyeHeight() - hy;
                double dz = mcc$get$posZ(target) - hz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
                float pitch = (float) -(Math.atan2(dy, dist) * 180.0D / Math.PI);
                field_82220_d[i] = mcc$turn(field_82220_d[i], pitch, 40.0F);
                field_82221_e[i] = mcc$turn(field_82221_e[i], yaw, 10.0F);
            } else {
                field_82221_e[i] = mcc$turn(field_82221_e[i], mcc$get$renderYawOffset(this), 10.0F);
            }
        }
    }

    private static float mcc$turn(float current, float target, float max) {
        float d = net.minecraft.util.Mth.wrapDegrees(target - current);
        return current + Math.max(-max, Math.min(max, d));
    }

    private double func_82214_u(int head) {
        if (head <= 0) return mcc$get$posX(this);
        float yaw = (mcc$get$renderYawOffset(this) + 180 * (head - 1)) / 180.0F * (float) Math.PI;
        return mcc$get$posX(this) + Math.cos(yaw) * 1.3D;
    }

    private double func_82208_v(int head) {
        return head <= 0 ? mcc$get$posY(this) + 3.0D : mcc$get$posY(this) + 2.2D;
    }

    private double func_82213_w(int head) {
        if (head <= 0) return mcc$get$posZ(this);
        float yaw = (mcc$get$renderYawOffset(this) + 180 * (head - 1)) / 180.0F * (float) Math.PI;
        return mcc$get$posZ(this) + Math.sin(yaw) * 1.3D;
    }

    /** Pitch of side head {@code head} (0 or 1). */
    public float func_82207_a(int head) {
        return field_82221_e[head];
    }

    /** Yaw of side head {@code head}. */
    public float func_82210_r(int head) {
        return field_82220_d[head];
    }

    public int getInvulTime() {
        var w = mcc$wither();
        if (w != null) return w.getInvulnerableTicks();
        return dataWatcher.getWatchableObjectInt(20);
    }

    public void setInvulTime(int ticks) {
        var w = mcc$wither();
        if (w != null) w.setInvulnerableTicks(ticks);
        else dataWatcher.updateObject(20, Integer.valueOf(ticks));
    }

    /** Id of the entity head {@code head} (0 middle, 1 and 2 the sides) looks at. */
    public int getWatchedTargetId(int head) {
        var w = mcc$wither();
        if (w != null) return w.getAlternativeTarget(head);
        return dataWatcher.getWatchableObjectInt(17 + head);
    }

    public void func_82211_c(int head, int entityId) {
        var w = mcc$wither();
        if (w != null) w.setAlternativeTarget(head, entityId);
        else dataWatcher.updateObject(17 + head, Integer.valueOf(entityId));
    }

    /** Below half health the wither has its armour (arrows bounce off). */
    public boolean isArmored() {
        var w = mcc$wither();
        if (w != null) return w.isPowered();
        return getHealth() <= getMaxHealth() / 2.0F;
    }

    /** The spawn sequence: invulnerable and growing for 220 ticks. */
    public void func_82206_m() {
        setInvulTime(220);
        setHealth(getMaxHealth() / 3.0F);
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
    }

    @Override
    public int getTotalArmorValue() {
        return 4;
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    @Override
    protected void fall(float distance) {
    }

    @Override
    public void setInWeb() {
    }

    @Override
    public void mountEntity(Entity vehicle) {
        if (!mcc$hosted()) super.mountEntity(vehicle);
    }

    @Override
    public float getShadowSize() {
        return mcc$get$height(this) / 8.0F;
    }

    @Override
    protected String getLivingSound() {
        return "mob.wither.idle";
    }

    @Override
    protected String getHurtSound() {
        return "mob.wither.hurt";
    }

    @Override
    protected String getDeathSound() {
        return "mob.wither.death";
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("Invul", getInvulTime());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setInvulTime(tag.getInteger("Invul"));
    }
}
