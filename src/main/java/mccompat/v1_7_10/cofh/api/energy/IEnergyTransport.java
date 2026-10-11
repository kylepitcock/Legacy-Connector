package mccompat.v1_7_10.cofh.api.energy;

public interface IEnergyTransport extends IEnergyProvider, IEnergyReceiver {
    @Override
    int getEnergyStored(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);

    InterfaceType getTransportState(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);

    boolean setTransportState(InterfaceType state, mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);

    enum InterfaceType {
        SEND, RECEIVE, BALANCE;

        public InterfaceType getOpposite() {
            return this == BALANCE ? BALANCE : this == SEND ? RECEIVE : SEND;
        }

        public InterfaceType rotate() {
            return rotate(true);
        }

        public InterfaceType rotate(boolean forward) {
            InterfaceType[] v = values();
            return v[Math.floorMod(ordinal() + (forward ? 1 : -1), v.length)];
        }
    }
}
