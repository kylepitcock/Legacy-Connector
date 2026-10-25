package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;

/** 1.12.2 {@code Block}: a registry entry with a material, hardness/resistance, light and a creative tab. */
public class Block extends IForgeRegistryEntry.Impl<Block> {
    public static final legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespacedDefaultedByKey<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, Block> REGISTRY = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.view(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS, "air");
    protected final Material blockMaterial;
    protected float blockHardness;
    protected float blockResistance;
    protected int lightValue;
    protected int lightOpacity = 255;
    private String unlocalizedName;
    private CreativeTabs displayOnCreativeTab;
    /** The modern block registered for this one (set by the bridge). */
    public Object mcc$modern;

    protected SoundType blockSoundType = SoundType.STONE;
    protected final legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer blockState;
    private legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState defaultBlockState;

    public Block(Material material) {
        this.blockMaterial = material;
        legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer container = null;
        try {
            // overridden by mods before their own fields exist, as in 1.12; a state container that fails to build leaves the plain one
            container = createBlockState();
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Blocks").warn("The block state of {} could not be built: {}", getClass().getName(), e.toString());
        }
        this.blockState = container != null ? container : new legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer(this);
        setDefaultState(blockState.getBaseState());
    }

    protected legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer createBlockState() {
        return new legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer(this);
    }

    public legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer getBlockState() {
        return blockState;
    }

    protected final void setDefaultState(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        this.defaultBlockState = state;
    }

    public final legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getDefaultState() {
        return defaultBlockState;
    }

    /** The state saved as these 4 metadata bits; mods override both directions. */
    @SuppressWarnings("deprecation")
    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    public int getMetaFromState(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return 0;
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

    protected Block setSoundType(SoundType type) {
        this.blockSoundType = type;
        return this;
    }

    public SoundType getSoundType() {
        return blockSoundType;
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

    /** Forge: whether the state has a tile entity (BlockContainer says yes). */
    public boolean hasTileEntity(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return false;
    }

    public legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity createTileEntity(legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return null;
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
