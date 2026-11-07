package ic2.api.energy.tile;

import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/** Test double of IC2's 1.6.4 sink API (the real one ships inside mods). */
public interface IEnergySink {
    boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction);

    double demandedEnergyUnits();

    double injectEnergyUnits(ForgeDirection directionFrom, double amount);

    int getMaxSafeInput();
}
