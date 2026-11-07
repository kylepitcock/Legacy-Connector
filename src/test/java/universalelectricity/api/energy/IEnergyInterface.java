package universalelectricity.api.energy;

import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/** Test double of Universal Electricity 3.0's interface. */
public interface IEnergyInterface {
    boolean canConnect(ForgeDirection direction);

    long onReceiveEnergy(ForgeDirection from, long receive, boolean doReceive);

    long onExtractEnergy(ForgeDirection from, long extract, boolean doExtract);
}
