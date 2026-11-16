package legacyconnector.v1_7_10.net.minecraft.nbt;

import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.Tag;

/** The end marker of a compound (tag id 0): carries no data. */
public class NBTTagEnd extends NBTBase {
    public NBTTagEnd() {
    }

    @Override
    public byte getId() {
        return 0;
    }

    @Override
    public NBTBase copy() {
        return new NBTTagEnd();
    }

    @Override
    public Tag mcc$modern() {
        return EndTag.INSTANCE;
    }

    @Override
    public String toString() {
        return "END";
    }
}
