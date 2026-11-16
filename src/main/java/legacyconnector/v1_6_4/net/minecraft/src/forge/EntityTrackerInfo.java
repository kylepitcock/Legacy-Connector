package legacyconnector.v1_6_4.net.minecraft.src.forge;

public class EntityTrackerInfo {
    public final NetworkMod Mod;
    public final int ID;
    public final int Range;
    public final int UpdateFrequency;
    public final boolean SendVelocityInfo;

    public EntityTrackerInfo(NetworkMod mod, int id, int range, int updateFrequency, boolean sendVelocityInfo) {
        this.Mod = mod;
        this.ID = id;
        this.Range = range;
        this.UpdateFrequency = updateFrequency;
        this.SendVelocityInfo = sendVelocityInfo;
    }
}
