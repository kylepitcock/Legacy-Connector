package legacyconnector.v1_6_4.net.minecraftforge.liquids;

/** One tank of the 1.5.2/1.6.4 liquid API. */
public interface ILiquidTank {
    /** The liquid in the tank (a stack that may be edited in place), or null when empty. */
    LiquidStack getLiquid();

    int getCapacity();

    /** @return the amount that went in */
    int fill(LiquidStack resource, boolean doFill);

    /** @return what was taken out, or null */
    LiquidStack drain(int maxDrain, boolean doDrain);

    /** Pressure for pipes (0 for a plain tank). */
    int getTankPressure();
}
