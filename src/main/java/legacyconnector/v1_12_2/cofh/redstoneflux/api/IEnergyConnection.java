package legacyconnector.v1_12_2.cofh.redstoneflux.api;

import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** Redstone Flux (1.12.2): a tile that connects to RF cables and machines on some sides. */
public interface IEnergyConnection {
    boolean canConnectEnergy(EnumFacing from);
}
