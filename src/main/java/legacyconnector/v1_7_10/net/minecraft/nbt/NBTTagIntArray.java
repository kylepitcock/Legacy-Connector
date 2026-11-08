package legacyconnector.v1_7_10.net.minecraft.nbt;

import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;

public class NBTTagIntArray extends NBTBase {
    private final int[] data;

    /** 1.6.4 tags carried a name: a bare named tag holds the default value. */
    public NBTTagIntArray(String name) {
        this(new int[0]);
    }

    public NBTTagIntArray(int[] data) {
        this.data = data == null ? new int[0] : data;
    }

    @Override
    public byte getId() {
        return 11;
    }

    @Override
    public NBTBase copy() {
        return new NBTTagIntArray(data.clone());
    }

    @Override
    public Tag mcc$modern() {
        return new IntArrayTag(data.clone());
    }

    public int[] getIntArray() {
        return data;
    }
}
