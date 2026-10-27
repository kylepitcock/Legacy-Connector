package legacyconnector.v1_12_2.net.minecraft.nbt;

import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.Tag;

/** The end tag (what an out-of-range list read returns in 1.12.2). */
public class NBTTagEnd extends NBTBase {
    public NBTTagEnd() {
    }

    @Override
    public byte getId() {
        return 0;
    }

    @Override
    public NBTTagEnd copy() {
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
