package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

/** 1.12.2 {@code Block}: a registry entry with a material, hardness/resistance, light and a creative tab. */
public class Block extends IForgeRegistryEntry.Impl<Block> {
    protected final Material blockMaterial;
    protected float blockHardness;
    protected float blockResistance;
    protected int lightValue;
    protected int lightOpacity = 255;
    private String unlocalizedName;
    private CreativeTabs displayOnCreativeTab;
    /** The modern block registered for this one (set by the bridge). */
    public Object mcc$modern;

    public Block(Material material) {
        this.blockMaterial = material;
    }

    public Material getMaterialLegacy() {
        return blockMaterial;
    }

    public Block setUnlocalizedName(String name) {
        this.unlocalizedName = name;
        return this;
    }

    public String getUnlocalizedName() {
        return "tile." + unlocalizedName;
    }

    public String getTranslationKey() {
        return getUnlocalizedName();
    }

    public Block setHardness(float hardness) {
        this.blockHardness = hardness;
        if (this.blockResistance < hardness * 5.0F) this.blockResistance = hardness * 5.0F;
        return this;
    }

    public Block setBlockUnbreakable() {
        this.blockHardness = -1.0F;
        return this;
    }

    public Block setResistance(float resistance) {
        this.blockResistance = resistance * 3.0F;
        return this;
    }

    public Block setLightLevel(float level) {
        this.lightValue = (int) (15.0F * level);
        return this;
    }

    public Block setLightOpacity(int opacity) {
        this.lightOpacity = opacity;
        return this;
    }

    public Block setCreativeTab(CreativeTabs tab) {
        this.displayOnCreativeTab = tab;
        return this;
    }

    public CreativeTabs getCreativeTabToDisplayOn() {
        return displayOnCreativeTab;
    }

    public float mcc$hardness() {
        return blockHardness;
    }

    public float mcc$resistance() {
        return blockResistance;
    }

    public int mcc$light() {
        return lightValue;
    }
}
