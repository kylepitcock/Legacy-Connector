package legacyconnector.v1_7_10.net.minecraftforge.common.network;

/** Forge's own messages (the dimension registration sent to clients). The host registers modded dimensions itself; these carry the legacy data. */
public abstract class ForgeMessage {
    /** A dimension and the provider type it runs, sent to a joining client. */
    public static class DimensionRegisterMessage extends ForgeMessage {
        public int dimensionId;
        public int providerId;

        public DimensionRegisterMessage() {
        }

        public DimensionRegisterMessage(int dimensionId, int providerId) {
            this.dimensionId = dimensionId;
            this.providerId = providerId;
        }
    }
}
