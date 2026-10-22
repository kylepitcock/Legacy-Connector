package mccompat.v1_12_2.net.minecraft.nbt;

import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;

public class NBTTagIntArray extends NBTBase {
    private final int[] data;

    public NBTTagIntArray(int[] data) {
        this.data = data;
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
