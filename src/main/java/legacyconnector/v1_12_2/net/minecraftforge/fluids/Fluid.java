package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** A fluid: its name, textures and physical properties (the modern fluid type is made from these when it is registered). */
public class Fluid {
    protected final String fluidName;
    protected final ResourceLocation still;
    protected final ResourceLocation flowing;
    protected ResourceLocation overlay;
    protected String unlocalizedName;
    protected int luminosity;
    protected int density = 1000;
    protected int temperature = 295;
    protected int viscosity = 1000;
    protected boolean isGaseous;
    protected int color = 0xFFFFFFFF;
    protected Block block;

    public Fluid(String fluidName, ResourceLocation still, ResourceLocation flowing) {
        this.fluidName = fluidName == null ? null : fluidName.toLowerCase(java.util.Locale.ROOT);
        this.unlocalizedName = fluidName;
        this.still = still;
        this.flowing = flowing;
    }

    public Fluid(String fluidName, ResourceLocation still, ResourceLocation flowing, int color) {
        this(fluidName, still, flowing);
        this.color = color;
    }

    public Fluid(String fluidName, ResourceLocation still, ResourceLocation flowing, ResourceLocation overlay) {
        this(fluidName, still, flowing);
        this.overlay = overlay;
    }

    public final String getName() {
        return fluidName;
    }

    public Fluid setUnlocalizedName(String unlocalizedName) {
        this.unlocalizedName = unlocalizedName;
        return this;
    }

    public String getUnlocalizedName() {
        return "fluid." + unlocalizedName;
    }

    public String getUnlocalizedName(FluidStack stack) {
        return getUnlocalizedName();
    }

    public String getLocalizedName(FluidStack stack) {
        return getUnlocalizedName();
    }

    public Fluid setBlock(Block block) {
        this.block = block;
        return this;
    }

    public Block getBlock() {
        return block;
    }

    public boolean canBePlacedInWorld() {
        return block != null;
    }

    public Fluid setLuminosity(int luminosity) {
        this.luminosity = luminosity;
        return this;
    }

    public int getLuminosity() {
        return luminosity;
    }

    public Fluid setDensity(int density) {
        this.density = density;
        return this;
    }

    public int getDensity() {
        return density;
    }

    public Fluid setTemperature(int temperature) {
        this.temperature = temperature;
        return this;
    }

    public int getTemperature() {
        return temperature;
    }

    public Fluid setViscosity(int viscosity) {
        this.viscosity = viscosity;
        return this;
    }

    public int getViscosity() {
        return viscosity;
    }

    public Fluid setGaseous(boolean gaseous) {
        this.isGaseous = gaseous;
        return this;
    }

    public boolean isGaseous() {
        return isGaseous;
    }

    public Fluid setColor(int color) {
        this.color = color;
        return this;
    }

    public int getColor() {
        return color;
    }

    public ResourceLocation getStill() {
        return still;
    }

    public ResourceLocation getFlowing() {
        return flowing;
    }

    public ResourceLocation getOverlay() {
        return overlay;
    }

    public Fluid setRarity(Object rarity) {
        return this;
    }

    public Fluid setFillSound(Object sound) {
        return this;
    }

    public Fluid setEmptySound(Object sound) {
        return this;
    }

    public boolean doesVaporize(FluidStack stack) {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Fluid f && fluidName != null && fluidName.equals(f.fluidName);
    }

    @Override
    public int hashCode() {
        return fluidName == null ? 0 : fluidName.hashCode();
    }
}
