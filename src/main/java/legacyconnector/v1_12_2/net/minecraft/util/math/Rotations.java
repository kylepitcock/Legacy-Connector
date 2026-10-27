package legacyconnector.v1_12_2.net.minecraft.util.math;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagFloat;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;

/** Rotations about x, y and z in degrees (armor stand poses). */
public class Rotations {
    protected final float x;
    protected final float y;
    protected final float z;

    public Rotations(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Rotations(NBTTagList nbt) {
        this(nbt.getFloatAt(0), nbt.getFloatAt(1), nbt.getFloatAt(2));
    }

    public NBTTagList writeToNBT() {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagFloat(x));
        list.appendTag(new NBTTagFloat(y));
        list.appendTag(new NBTTagFloat(z));
        return list;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Rotations r && x == r.x && y == r.y && z == r.z;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y, z);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public net.minecraft.core.Rotations mcc$modern() {
        return new net.minecraft.core.Rotations(x, y, z);
    }
}
