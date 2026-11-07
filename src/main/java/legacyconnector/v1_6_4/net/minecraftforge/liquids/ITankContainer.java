package legacyconnector.v1_6_4.net.minecraftforge.liquids;

import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/** A block or tile entity with liquid tanks (pipes and machines of the 1.5.2/1.6.4 era). */
public interface ITankContainer {
    int fill(ForgeDirection from, LiquidStack resource, boolean doFill);

    int fill(int tankIndex, LiquidStack resource, boolean doFill);

    LiquidStack drain(ForgeDirection from, int maxDrain, boolean doDrain);

    LiquidStack drain(int tankIndex, int maxDrain, boolean doDrain);

    ILiquidTank[] getTanks(ForgeDirection direction);

    ILiquidTank getTank(ForgeDirection direction, LiquidStack type);
}
