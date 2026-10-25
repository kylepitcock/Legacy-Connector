package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public class NetworkRegistry {
    public static final NetworkRegistry INSTANCE = new NetworkRegistry();

    public SimpleNetworkWrapper newSimpleChannel(String name) {
        return new SimpleNetworkWrapper(name);
    }

    public void registerGuiHandler(Object mod, IGuiHandler handler) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122.register(mod, handler);
    }

    public static class TargetPoint {
        public final int dimension;
        public final double x;
        public final double y;
        public final double z;
        public final double range;

        public TargetPoint(int dimension, double x, double y, double z, double range) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.range = range;
        }
    }
}
