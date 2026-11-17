package buildcraft.api.liquids;

/** Test double of the tank interface. */
public interface ILiquidTank {
    LiquidStack getLiquid();

    int getCapacity();
}
