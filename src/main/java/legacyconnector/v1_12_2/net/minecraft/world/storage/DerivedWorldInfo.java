package legacyconnector.v1_12_2.net.minecraft.world.storage;

/** 1.12.2 world info of a secondary dimension: reads through to the overworld's info; its setters change nothing. */
public class DerivedWorldInfo extends WorldInfo {
    private final WorldInfo delegate;

    public DerivedWorldInfo(WorldInfo worldInfoIn) {
        super(worldInfoIn);
        this.delegate = worldInfoIn;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound cloneNBTCompound(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound nbt) {
        return delegate.cloneNBTCompound(nbt);
    }

    public void setServerInitialized(boolean initializedIn) {
    }

    public void setSpawn(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos spawnPoint) {
    }
}
