package legacyconnector.v1_6_4.net.minecraftforge.liquids;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/** The stock tank of the 1.5.2/1.6.4 liquid API. */
public class LiquidTank implements ILiquidTank {
    private LiquidStack liquid;
    private int capacity;
    private int tankPressure;

    public LiquidTank(int capacity) {
        this(null, capacity);
    }

    public LiquidTank(int liquidId, int quantity, int capacity) {
        this(new LiquidStack(liquidId, quantity, 0), capacity);
    }

    public LiquidTank(int liquidId, int quantity, int liquidMeta, int capacity) {
        this(new LiquidStack(liquidId, quantity, liquidMeta), capacity);
    }

    public LiquidTank(LiquidStack liquid, int capacity) {
        this.liquid = liquid;
        this.capacity = capacity;
    }

    @Override
    public LiquidStack getLiquid() {
        return liquid;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    public void setLiquid(LiquidStack liquid) {
        this.liquid = liquid;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public int fill(LiquidStack resource, boolean doFill) {
        if (resource == null || resource.itemID <= 0 || resource.amount <= 0) return 0;
        if (liquid == null || liquid.itemID <= 0) {
            int filled = Math.min(capacity, resource.amount);
            if (doFill) liquid = new LiquidStack(resource.itemID, filled, resource.itemMeta);
            return filled;
        }
        if (!liquid.isLiquidEqual(resource)) return 0;
        int space = capacity - liquid.amount;
        int filled = Math.min(space, resource.amount);
        if (doFill && filled > 0) liquid.amount += filled;
        return Math.max(filled, 0);
    }

    @Override
    public LiquidStack drain(int maxDrain, boolean doDrain) {
        if (liquid == null || liquid.itemID <= 0 || maxDrain <= 0) return null;
        int drained = Math.min(liquid.amount, maxDrain);
        LiquidStack out = new LiquidStack(liquid.itemID, drained, liquid.itemMeta);
        if (doDrain) {
            liquid.amount -= drained;
            if (liquid.amount <= 0) liquid = null;
        }
        return out;
    }

    @Override
    public int getTankPressure() {
        return tankPressure;
    }

    public void setTankPressure(int pressure) {
        this.tankPressure = pressure;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        if (liquid != null) liquid.writeToNBT(nbt);
        return nbt;
    }

    public LiquidTank readFromNBT(NBTTagCompound nbt) {
        liquid = LiquidStack.loadLiquidStackFromNBT(nbt);
        return this;
    }
}
