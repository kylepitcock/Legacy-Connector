package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** A fluid: its name, textures and physical properties (the modern fluid type is made from these when it is registered). */
public class Fluid {
    /** One bucket: 1000 millibuckets. */
    public static final int BUCKET_VOLUME = 1000;
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

    protected legacyconnector.v1_12_2.net.minecraft.item.EnumRarity rarity = legacyconnector.v1_12_2.net.minecraft.item.EnumRarity.COMMON;
    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent fillSound;
    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent emptySound;

    public Fluid setRarity(legacyconnector.v1_12_2.net.minecraft.item.EnumRarity rarity) {
        this.rarity = rarity;
        return this;
    }

    public Fluid setFillSound(legacyconnector.v1_12_2.net.minecraft.util.SoundEvent sound) {
        this.fillSound = sound;
        return this;
    }

    public Fluid setEmptySound(legacyconnector.v1_12_2.net.minecraft.util.SoundEvent sound) {
        this.emptySound = sound;
        return this;
    }

    public legacyconnector.v1_12_2.net.minecraft.item.EnumRarity getRarity() {
        return rarity;
    }

    /** The sound of filling a container with this fluid (vanilla's bucket sounds when unset: lava's for hot fluids). */
    public legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getFillSound() {
        if (fillSound == null) {
            fillSound = getTemperature() > 1000 ? legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.ITEM_BUCKET_FILL_LAVA
                    : legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.ITEM_BUCKET_FILL;
        }
        return fillSound;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getEmptySound() {
        if (emptySound == null) {
            emptySound = getTemperature() > 1000 ? legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.ITEM_BUCKET_EMPTY_LAVA
                    : legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.ITEM_BUCKET_EMPTY;
        }
        return emptySound;
    }

    // Forge's per-stack variants: the fluid's own values unless a mod overrides them.

    public int getLuminosity(FluidStack stack) {
        return getLuminosity();
    }

    public int getDensity(FluidStack stack) {
        return getDensity();
    }

    public int getTemperature(FluidStack stack) {
        return getTemperature();
    }

    public int getViscosity(FluidStack stack) {
        return getViscosity();
    }

    public boolean isGaseous(FluidStack stack) {
        return isGaseous();
    }

    public legacyconnector.v1_12_2.net.minecraft.item.EnumRarity getRarity(FluidStack stack) {
        return getRarity();
    }

    public int getColor(FluidStack stack) {
        return getColor();
    }

    public ResourceLocation getStill(FluidStack stack) {
        return getStill();
    }

    public ResourceLocation getFlowing(FluidStack stack) {
        return getFlowing();
    }

    public legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getFillSound(FluidStack stack) {
        return getFillSound();
    }

    public legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getEmptySound(FluidStack stack) {
        return getEmptySound();
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
