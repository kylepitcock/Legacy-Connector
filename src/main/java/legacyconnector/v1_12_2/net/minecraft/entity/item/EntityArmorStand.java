package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import legacyconnector.v1_12_2.net.minecraft.util.math.Rotations;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 armour stand: a view of the modern one (poses, small, arms, base plate, marker). */
public class EntityArmorStand extends legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase {
    public static final DataParameter<Byte> STATUS = EntityDataManager.createKey(EntityArmorStand.class, DataSerializers.BYTE);
    private Rotations head = new Rotations(0.0F, 0.0F, 0.0F);
    private Rotations body = new Rotations(0.0F, 0.0F, 0.0F);
    private Rotations leftArm = new Rotations(-10.0F, 0.0F, -10.0F);
    private Rotations rightArm = new Rotations(-15.0F, 0.0F, 10.0F);
    private Rotations leftLeg = new Rotations(-1.0F, 0.0F, -1.0F);
    private Rotations rightLeg = new Rotations(1.0F, 0.0F, 1.0F);

    public EntityArmorStand(World world) {
        super(world);
        setSize(0.5F, 1.975F);
    }

    public EntityArmorStand(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
    }

    protected EntityArmorStand(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    private net.minecraft.world.entity.decoration.ArmorStand mcc$stand() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.decoration.ArmorStand a ? a : null;
    }

    private static Rotations mcc$of(net.minecraft.core.Rotations r) {
        return new Rotations(r.getX(), r.getY(), r.getZ());
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(STATUS, (byte) 0);
    }

    private boolean mcc$flag(int mask) {
        Byte b = dataManager.get(STATUS);
        return b != null && (b & mask) != 0;
    }

    public boolean isSmall() {
        var a = mcc$stand();
        return a != null ? a.isSmall() : mcc$flag(1);
    }

    public boolean getShowArms() {
        var a = mcc$stand();
        return a != null ? a.isShowArms() : mcc$flag(4);
    }

    public boolean hasNoBasePlate() {
        var a = mcc$stand();
        return a != null ? a.isNoBasePlate() : mcc$flag(8);
    }

    public boolean hasMarker() {
        var a = mcc$stand();
        return a != null ? a.isMarker() : mcc$flag(16);
    }

    public Rotations getHeadRotation() {
        var a = mcc$stand();
        if (a != null) return mcc$of(a.getHeadPose());
        return head;
    }

    public void setHeadRotation(Rotations r) {
        var a = mcc$stand();
        if (a != null && r != null) a.setHeadPose(r.mcc$modern());
        else if (r != null) head = r;
    }

    public Rotations getBodyRotation() {
        var a = mcc$stand();
        if (a != null) return mcc$of(a.getBodyPose());
        return body;
    }

    public void setBodyRotation(Rotations r) {
        var a = mcc$stand();
        if (a != null && r != null) a.setBodyPose(r.mcc$modern());
        else if (r != null) body = r;
    }

    public Rotations getLeftArmRotation() {
        var a = mcc$stand();
        if (a != null) return mcc$of(a.getLeftArmPose());
        return leftArm;
    }

    public void setLeftArmRotation(Rotations r) {
        var a = mcc$stand();
        if (a != null && r != null) a.setLeftArmPose(r.mcc$modern());
        else if (r != null) leftArm = r;
    }

    public Rotations getRightArmRotation() {
        var a = mcc$stand();
        if (a != null) return mcc$of(a.getRightArmPose());
        return rightArm;
    }

    public void setRightArmRotation(Rotations r) {
        var a = mcc$stand();
        if (a != null && r != null) a.setRightArmPose(r.mcc$modern());
        else if (r != null) rightArm = r;
    }

    public Rotations getLeftLegRotation() {
        var a = mcc$stand();
        if (a != null) return mcc$of(a.getLeftLegPose());
        return leftLeg;
    }

    public void setLeftLegRotation(Rotations r) {
        var a = mcc$stand();
        if (a != null && r != null) a.setLeftLegPose(r.mcc$modern());
        else if (r != null) leftLeg = r;
    }

    public Rotations getRightLegRotation() {
        var a = mcc$stand();
        if (a != null) return mcc$of(a.getRightLegPose());
        return rightLeg;
    }

    public void setRightLegRotation(Rotations r) {
        var a = mcc$stand();
        if (a != null && r != null) a.setRightLegPose(r.mcc$modern());
        else if (r != null) rightLeg = r;
    }
}
