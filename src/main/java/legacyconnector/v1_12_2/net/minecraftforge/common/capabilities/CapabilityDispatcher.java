package legacyconnector.v1_12_2.net.minecraftforge.common.capabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.common.util.INBTSerializable;

/**
 * Forge: the capabilities attached to an object (AttachCapabilitiesEvent's providers, and a parent provider), asked in order; the
 * serializable ones are saved under their names.
 */
public final class CapabilityDispatcher implements INBTSerializable<NBTTagCompound>, ICapabilityProvider {
    private final ICapabilityProvider[] caps;
    private final INBTSerializable<NBTBase>[] writers;
    private final String[] names;

    public CapabilityDispatcher(Map<ResourceLocation, ICapabilityProvider> list) {
        this(list, null);
    }

    @SuppressWarnings("unchecked")
    public CapabilityDispatcher(Map<ResourceLocation, ICapabilityProvider> list, ICapabilityProvider parent) {
        List<ICapabilityProvider> lstCaps = new ArrayList<>();
        List<INBTSerializable<NBTBase>> lstWriters = new ArrayList<>();
        List<String> lstNames = new ArrayList<>();
        if (parent != null) {
            lstCaps.add(parent);
            if (parent instanceof INBTSerializable<?> s) {
                lstWriters.add((INBTSerializable<NBTBase>) s);
                lstNames.add("Parent");
            }
        }
        for (Map.Entry<ResourceLocation, ICapabilityProvider> entry : list.entrySet()) {
            ICapabilityProvider prov = entry.getValue();
            lstCaps.add(prov);
            if (prov instanceof INBTSerializable<?> s) {
                lstWriters.add((INBTSerializable<NBTBase>) s);
                lstNames.add(entry.getKey().toString());
            }
        }
        caps = lstCaps.toArray(new ICapabilityProvider[0]);
        writers = lstWriters.toArray(new INBTSerializable[0]);
        names = lstNames.toArray(new String[0]);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        for (ICapabilityProvider cap : caps) if (cap.hasCapability(capability, facing)) return true;
        return false;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        for (ICapabilityProvider cap : caps) {
            T ret = cap.getCapability(capability, facing);
            if (ret != null) return ret;
        }
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        for (int x = 0; x < writers.length; x++) {
            NBTBase tag = writers[x].serializeNBT();
            if (tag != null) nbt.setTag(names[x], tag);
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        for (int x = 0; x < writers.length; x++) {
            if (nbt.hasKey(names[x])) writers[x].deserializeNBT(nbt.getTag(names[x]));
        }
    }

    public boolean areCompatible(CapabilityDispatcher other) {
        if (other == null) return writers.length == 0;
        if (writers.length == 0) return other.writers.length == 0;
        return serializeNBT().equals(other.serializeNBT());
    }
}
