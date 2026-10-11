package mccompat.v1_7_10.net.minecraftforge.common;

import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.world.World;

/** Per-entity data a mod attaches during EntityConstructing; saved with the entity. */
public interface IExtendedEntityProperties {
    void saveNBTData(NBTTagCompound compound);

    void loadNBTData(NBTTagCompound compound);

    void init(Entity entity, World world);
}
