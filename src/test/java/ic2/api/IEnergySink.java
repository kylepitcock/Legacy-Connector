package ic2.api;

import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;

/** Test double of IC2 1.103's sink (acceptsEnergyFrom is declared by IEnergyAcceptor in the real jar). */
public interface IEnergySink {
    boolean acceptsEnergyFrom(TileEntity emitter, Direction direction);

    boolean demandsEnergy();

    int injectEnergy(Direction direction, int amount);
}
