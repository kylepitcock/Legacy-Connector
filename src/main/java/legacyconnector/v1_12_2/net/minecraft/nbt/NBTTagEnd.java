package legacyconnector.v1_12_2.net.minecraft.nbt;

/** The end tag, which lookups of absent list entries return. */
public class NBTTagEnd extends NBTBase {
    @Override
    public byte getId() {
        return 0;
    }

    @Override
    public NBTBase copy() {
        return new NBTTagEnd();
    }

    @Override
    public net.minecraft.nbt.Tag mcc$modern() {
        return net.minecraft.nbt.EndTag.INSTANCE;
    }

    @Override
    public String toString() {
        return "END";
    }
}
