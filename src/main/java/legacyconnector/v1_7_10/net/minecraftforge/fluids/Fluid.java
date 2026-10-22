package legacyconnector.v1_7_10.net.minecraftforge.fluids;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.EnumRarity;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.StatCollector;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Forge 1.7.10 fluid definition (properties only; fluids are not placed into the modern world yet). */
public class Fluid {
    protected final String fluidName;
    protected String unlocalizedName;
    protected IIcon stillIcon;
    protected IIcon flowingIcon;
    protected int luminosity = 0;
    protected int density = 1000;
    protected int temperature = 300;
    protected int viscosity = 1000;
    protected boolean isGaseous;
    protected EnumRarity rarity = EnumRarity.common;
    protected Block block = null;

    public Fluid(String fluidName) {
        this.fluidName = fluidName.toLowerCase(java.util.Locale.ENGLISH);
        this.unlocalizedName = fluidName;
    }

    public Fluid setUnlocalizedName(String unlocalizedName) {
        this.unlocalizedName = unlocalizedName;
        return this;
    }

    public Fluid setBlock(Block block) {
        if (this.block == null || this.block == block) this.block = block;
        FluidRegistry.mcc$blockSet(this, block);
        return this;
    }

    public Fluid setLuminosity(int luminosity) {
        this.luminosity = luminosity;
        return this;
    }

    public Fluid setDensity(int density) {
        this.density = density;
        return this;
    }

    public Fluid setTemperature(int temperature) {
        this.temperature = temperature;
        return this;
    }

    public Fluid setViscosity(int viscosity) {
        this.viscosity = viscosity;
        return this;
    }

    public Fluid setGaseous(boolean isGaseous) {
        this.isGaseous = isGaseous;
        return this;
    }

    public Fluid setRarity(EnumRarity rarity) {
        this.rarity = rarity;
        return this;
    }

    public final String getName() {
        return fluidName;
    }

    public final int getID() {
        return FluidRegistry.getFluidID(fluidName);
    }

    public final Block getBlock() {
        return block;
    }

    public final boolean canBePlacedInWorld() {
        return block != null;
    }

    public String getLocalizedName(FluidStack stack) {
        return getLocalizedName();
    }

    public String getLocalizedName() {
        String s = getUnlocalizedName();
        return s == null ? "" : StatCollector.translateToLocal(s);
    }

    public String getUnlocalizedName(FluidStack stack) {
        return getUnlocalizedName();
    }

    public String getUnlocalizedName() {
        return "fluid." + unlocalizedName;
    }

    public final int getSpriteNumber() {
        return 0;
    }

    public final int getLuminosity() {
        return luminosity;
    }

    public final int getDensity() {
        return density;
    }

    public final int getTemperature() {
        return temperature;
    }

    public final int getViscosity() {
        return viscosity;
    }

    public final boolean isGaseous() {
        return isGaseous;
    }

    public EnumRarity getRarity() {
        return rarity;
    }

    public int getColor() {
        return 0xFFFFFF;
    }

    public final Fluid setStillIcon(IIcon stillIcon) {
        this.stillIcon = stillIcon;
        return this;
    }

    public final Fluid setFlowingIcon(IIcon flowingIcon) {
        this.flowingIcon = flowingIcon;
        return this;
    }

    public final Fluid setIcons(IIcon stillIcon, IIcon flowingIcon) {
        return setStillIcon(stillIcon).setFlowingIcon(flowingIcon);
    }

    public final Fluid setIcons(IIcon commonIcon) {
        return setIcons(commonIcon, commonIcon);
    }

    public IIcon getIcon() {
        return getStillIcon();
    }

    public IIcon getStillIcon() {
        return stillIcon;
    }

    public IIcon getFlowingIcon() {
        return flowingIcon;
    }

    public int getLuminosity(FluidStack stack) { return getLuminosity(); }
    public int getDensity(FluidStack stack) { return getDensity(); }
    public int getTemperature(FluidStack stack) { return getTemperature(); }
    public int getViscosity(FluidStack stack) { return getViscosity(); }
    public boolean isGaseous(FluidStack stack) { return isGaseous(); }
    public EnumRarity getRarity(FluidStack stack) { return getRarity(); }
    public int getColor(FluidStack stack) { return getColor(); }
    public IIcon getIcon(FluidStack stack) { return getIcon(); }
    public int getLuminosity(World world, int x, int y, int z) { return getLuminosity(); }
    public int getDensity(World world, int x, int y, int z) { return getDensity(); }
    public int getTemperature(World world, int x, int y, int z) { return getTemperature(); }
    public int getViscosity(World world, int x, int y, int z) { return getViscosity(); }
    public boolean isGaseous(World world, int x, int y, int z) { return isGaseous(); }
    public EnumRarity getRarity(World world, int x, int y, int z) { return getRarity(); }
    public int getColor(World world, int x, int y, int z) { return getColor(); }
    public IIcon getIcon(World world, int x, int y, int z) { return getIcon(); }

    @Override
    public String toString() {
        return "LegacyFluid{" + fluidName + "}";
    }
}
