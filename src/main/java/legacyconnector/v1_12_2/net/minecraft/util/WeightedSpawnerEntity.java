package legacyconnector.v1_12_2.net.minecraft.util;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** One entry of a spawner's spawn list: an entity (its saved data, with its id) and a weight. */
public class WeightedSpawnerEntity extends WeightedRandom.Item {
    private final NBTTagCompound nbt;

    public WeightedSpawnerEntity() {
        super(1);
        this.nbt = new NBTTagCompound();
        this.nbt.setString("id", "minecraft:pig");
    }

    public WeightedSpawnerEntity(NBTTagCompound nbtIn) {
        this(nbtIn.hasKey("Weight", 99) ? nbtIn.getInteger("Weight") : 1, nbtIn.getCompoundTag("Entity"));
    }

    public WeightedSpawnerEntity(int itemWeightIn, NBTTagCompound nbtIn) {
        super(itemWeightIn);
        this.nbt = nbtIn;
    }

    public NBTTagCompound toCompoundTag() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Entity", nbt);
        tag.setInteger("Weight", itemWeight);
        return tag;
    }

    public NBTTagCompound getNbt() {
        return nbt;
    }
}
