package buildcraft.api.liquids;

import buildcraft.api.Orientations;

/** Test double of the tile interface Forestry 1.4.8 bundles (the methods the adapter calls). */
public interface ITankContainer {
    int fill(Orientations from, LiquidStack resource, boolean doFill);

    LiquidStack drain(Orientations from, int maxDrain, boolean doDrain);

    ILiquidTank[] getTanks();
}
