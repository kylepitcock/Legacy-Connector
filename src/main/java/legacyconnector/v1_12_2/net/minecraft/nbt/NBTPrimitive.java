package legacyconnector.v1_12_2.net.minecraft.nbt;

/** The numeric tags (a top-level class in 1.12.2; 1.7.10 nested it in NBTBase). */
public abstract class NBTPrimitive extends NBTBase {
    public abstract long getLong();

    public abstract int getInt();

    public abstract short getShort();

    public abstract byte getByte();

    public abstract double getDouble();

    public abstract float getFloat();
}
