package legacyconnector.v1_6_4.net.minecraftforge.liquids;

/** Forge 1.4.7/1.5.2: implemented by the block of a liquid so the liquid containers know the stack it stands for. */
public interface ILiquid {
    /** The id of the still (source) block of this liquid. */
    int stillLiquidId();

    /** Whether the metadata of the block tells liquids apart. */
    boolean isMetaSensitive();

    /** The metadata of the still block when {@link #isMetaSensitive()}. */
    int stillLiquidMeta();
}
