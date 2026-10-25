package legacyconnector.v1_12_2.net.minecraftforge.common.capabilities;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

public class Capability<T> {
    public interface IStorage<T> {
        NBTBase writeNBT(Capability<T> capability, T instance, EnumFacing side);

        void readNBT(Capability<T> capability, T instance, EnumFacing side, NBTBase nbt);
    }

    private final String name;
    private final IStorage<T> storage;
    private final java.util.concurrent.Callable<? extends T> factory;

    Capability(String name, IStorage<T> storage, java.util.concurrent.Callable<? extends T> factory) {
        this.name = name;
        this.storage = storage;
        this.factory = factory;
    }

    public String getName() {
        return name;
    }

    public IStorage<T> getStorage() {
        return storage;
    }

    public boolean isRegistered() {
        return true;
    }

    public T getDefaultInstance() {
        try {
            return factory.call();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create a default instance of the capability " + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R cast(T instance) {
        return (R) instance;
    }

    public NBTBase writeNBT(T instance, EnumFacing side) {
        return storage.writeNBT(this, instance, side);
    }

    public void readNBT(T instance, EnumFacing side, NBTBase nbt) {
        storage.readNBT(this, instance, side, nbt);
    }
}
