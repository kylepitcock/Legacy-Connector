package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
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
    public static final legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB FULL_BLOCK_AABB = new legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    public static final legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB NULL_AABB = null;

    protected SoundType blockSoundType = SoundType.STONE;
    protected final legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer blockState;
    private legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState defaultBlockState;

    protected MapColor blockMapColor;

    public Block(Material material, MapColor color) {
        this(material);
        this.blockMapColor = color;
    }

    private static int MCC_STATE_FAILURES;

    public Block(Material material) {
        this.blockMaterial = material;
        legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer container = null;
        try {
            // overridden by mods before their own fields exist, as in 1.12; a state container that fails to build leaves the plain one
            container = createBlockState();
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Blocks").warn("The block state of {} could not be built: {}", getClass().getName(), e.toString(), MCC_STATE_FAILURES++ < 2 ? e : null);
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
        return blockMaterial != null ? blockMaterial : legacyconnector.v1_12_2.net.minecraft.block.material.Material.ROCK;
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

    public static Block getBlockFromName(String name) {
        if (name == null) return null;
        Block b = REGISTRY.getObject(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(name));
        if (b != null) return b;
        try {
            return getBlockById(Integer.parseInt(name));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Block getBlockById(int id) {
        return REGISTRY.getObjectById(id);
    }

    public static int getIdFromBlock(Block block) {
        return REGISTRY.getIDForObject(block);
    }

    public static Block getBlockFromItem(legacyconnector.v1_12_2.net.minecraft.item.Item item) {
        return item instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemBlock ib ? ib.getBlock() : null;
    }

    /**
     * 1.12 packs a state as {@code blockId + (meta << 12)}, which holds 4096 blocks; with more registered (big packs plus the vanilla
     * wrappers) the higher ids use a range of their own above 65535, so every state survives the round trip (BuildCraft's facades
     * check it).
     */
    private static final int MCC$WIDE_STATE_IDS = 65536;

    public static legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getStateById(int id) {
        int blockId = id >= MCC$WIDE_STATE_IDS ? (id - MCC$WIDE_STATE_IDS) >> 4 : id & 4095;
        int meta = id >= MCC$WIDE_STATE_IDS ? (id - MCC$WIDE_STATE_IDS) & 15 : id >> 12 & 15;
        Block block = getBlockById(blockId);
        return block == null ? null : block.getStateFromMeta(meta);
    }

    public static int getStateId(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        Block block = state.getBlock();
        int blockId = getIdFromBlock(block);
        int meta = block.getMetaFromState(state) & 15;
        return blockId < 4096 ? blockId + (meta << 12) : MCC$WIDE_STATE_IDS + (blockId << 4 | meta);
    }

    public Block setTranslationKey(String name) {
        return setUnlocalizedName(name);
    }

    /** Whether the block wants random ticks (recorded; mods read it back through {@link #getTickRandomly}). */
    protected boolean needsRandomTick;
    /** Friction of the block's top (0.6 normal, 0.98 ice). */
    public float slipperiness = 0.6F;

    public Block setTickRandomly(boolean shouldTick) {
        this.needsRandomTick = shouldTick;
        return this;
    }

    public boolean getTickRandomly() {
        return needsRandomTick;
    }

    /** Forge: sets {@link #slipperiness}. */
    public void setDefaultSlipperiness(float slipperiness) {
        this.slipperiness = slipperiness;
    }

    /** Vanilla's form: whether the block is a tile entity provider (Forge asks {@link #hasTileEntity(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState)}). */
    @Deprecated
    public boolean hasTileEntity() {
        return this instanceof ITileEntityProvider;
    }

    public String getLocalizedName() {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(getTranslationKey() + ".name");
    }

    public Block disableStats() {
        return this;
    }

    private final java.util.Map<String, Integer> harvest = new java.util.HashMap<>();

    public void setHarvestLevel(String toolClass, int level) {
        harvest.put(toolClass, level);
    }

    public void setHarvestLevel(String toolClass, int level, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        harvest.put(toolClass, level);
    }

    public String getHarvestTool(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return harvest.isEmpty() ? null : harvest.keySet().iterator().next();
    }

    public int getHarvestLevel(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        String tool = getHarvestTool(state);
        return tool == null ? -1 : harvest.get(tool);
    }

    public CreativeTabs getCreativeTab() {
        return displayOnCreativeTab;
    }

    public Material getMaterial(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return blockMaterial;
    }

    public int damageDropped(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return 0;
    }

    public float getBlockHardness(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return blockHardness;
    }

    public int getLightValue(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return lightValue;
    }

    public boolean isOpaqueCube(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return true;
    }

    public boolean isFullCube(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return true;
    }

    /** Vanilla: a block whose default state is an opaque cube is a full block. */
    public boolean isFullBlock(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return isOpaqueCube(getDefaultState());
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType getRenderType(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType.MODEL;
    }

    /** The resistance to explosions (a fifth of the value set with setResistance, which multiplied it by three). */
    public float getExplosionResistance(legacyconnector.v1_12_2.net.minecraft.entity.Entity exploder) {
        return blockResistance / 5.0F;
    }

    public void getSubBlocks(CreativeTabs tab, legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> items) {
        items.add(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(this));
    }

    // ------------------------------------------------------------------ gameplay hooks (1.12.2 defaults; mods override; the modern
    // block forwards the ones a mod overrides, see Legacy1122Block). Hooks taking an IBlockAccess wait for that interface (world area).

    /** The random blocks roll (drops). */
    protected static final java.util.Random RANDOM = new java.util.Random();
    /** Whether the block takes its light from its brightest neighbour (slabs, stairs, farmland). */
    protected boolean useNeighborBrightness;

    public boolean onBlockActivated(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY, float hitZ) {
        return false;
    }

    public void onBlockClicked(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
    }

    public void onBlockAdded(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
    }

    /** Called when the block is replaced; the modern block entity is removed by the game itself. */
    public void breakBlock(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
    }

    public void neighborChanged(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, Block block, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos fromPos) {
    }

    public void updateTick(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, java.util.Random rand) {
    }

    /** A random tick: an update tick unless the block says otherwise. */
    public void randomTick(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, java.util.Random random) {
        updateTick(world, pos, state, random);
    }

    public void randomDisplayTick(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, java.util.Random rand) {
    }

    public int tickRate(legacyconnector.v1_12_2.net.minecraft.world.World world) {
        return 10;
    }

    public void onBlockPlacedBy(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase placer,
            legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
    }

    public void onBlockHarvested(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
    }

    public void onPlayerDestroy(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
    }

    public void onEntityCollision(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
    }

    public void onEntityWalk(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
    }

    public void onFallenUpon(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity, float fallDistance) {
    }

    public void onLanded(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
    }

    public boolean eventReceived(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, int id, int param) {
        return false;
    }

    /** Whether the block may be placed here: the block there now can be replaced. */
    public boolean canPlaceBlockAt(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        var level = world == null ? null : world.mcc$level();
        return level == null || level.getBlockState(pos.mcc$modern()).canBeReplaced();
    }

    public boolean canPlaceBlockOnSide(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        return canPlaceBlockAt(world, pos);
    }

    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getStateForPlacement(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY,
            float hitZ, int meta, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase placer) {
        return getStateFromMeta(meta);
    }

    /** Forge's hand-aware form. */
    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getStateForPlacement(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing, float hitX, float hitY,
            float hitZ, int meta, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase placer, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer);
    }

    // ------------------------------------------------------------------ drops

    public legacyconnector.v1_12_2.net.minecraft.item.Item getItemDropped(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, java.util.Random rand, int fortune) {
        return legacyconnector.v1_12_2.net.minecraft.item.Item.getItemFromBlock(this);
    }

    public int quantityDropped(java.util.Random random) {
        return 1;
    }

    public int quantityDroppedWithBonus(int fortune, java.util.Random random) {
        return quantityDropped(random);
    }

    public int quantityDropped(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, int fortune, java.util.Random random) {
        return quantityDroppedWithBonus(fortune, random);
    }

    /** The drops 1.12's default getDrops computes: getItemDropped x quantityDropped, with damageDropped as metadata. */
    public java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> mcc$defaultDrops(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, int fortune) {
        java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> out = new java.util.ArrayList<>();
        java.util.Random rand = RANDOM;
        int count = quantityDropped(state, fortune, rand);
        for (int i = 0; i < count; i++) {
            legacyconnector.v1_12_2.net.minecraft.item.Item item = getItemDropped(state, rand, fortune);
            if (item != null && item != legacyconnector.v1_12_2.net.minecraft.init.Items.AIR) out.add(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item, 1, damageDropped(state)));
        }
        return out;
    }

    public final void dropBlockAsItem(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, int fortune) {
        dropBlockAsItemWithChance(world, pos, state, 1.0F, fortune);
    }

    public void dropBlockAsItemWithChance(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, float chance, int fortune) {
        if (world == null || world.isRemote) return;
        for (legacyconnector.v1_12_2.net.minecraft.item.ItemStack drop : mcc$drops(world, pos, state, fortune)) {
            if (world.rand.nextFloat() <= chance) spawnAsEntity(world, pos, drop);
        }
    }

    /** The block's drops through the modern block (which asks the mod's getDrops when it overrides it). */
    protected java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> mcc$drops(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, int fortune) {
        if (mcc$modern instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block b) return b.legacyDrops(world, pos, state, fortune);
        return mcc$defaultDrops(state, fortune);
    }

    /** Drops a stack as an item entity with 1.12's random offset in the block. */
    public static void spawnAsEntity(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        if (world == null || world.isRemote || stack == null || stack.isEmpty()) return;
        var level = world.mcc$level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel)) return;
        double dx = level.random.nextFloat() * 0.5F + 0.25D;
        double dy = level.random.nextFloat() * 0.5F + 0.25D;
        double dz = level.random.nextFloat() * 0.5F + 0.25D;
        var entity = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz,
                org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack));
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    /** What a player who breaks the block gets: the silk touch stack or the drops (the modern block handles harvesting itself). */
    public void harvestBlock(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state,
            legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity te, legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        int fortune = 0;
        boolean silk = false;
        if (stack != null && !stack.isEmpty()) {
            fortune = legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment.getEnchantmentByLocation("fortune"), stack);
            silk = legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment.getEnchantmentByLocation("silk_touch"), stack) > 0;
        }
        if (player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) p.causeFoodExhaustion(0.005F);
        if (silk && canSilkHarvest(world, pos, state, player)) {
            legacyconnector.v1_12_2.net.minecraft.item.ItemStack drop = getSilkTouchDrop(state);
            if (!drop.isEmpty()) spawnAsEntity(world, pos, drop);
        } else {
            dropBlockAsItem(world, pos, state, fortune);
        }
    }

    public boolean canSilkHarvest(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return canSilkHarvest() && !hasTileEntity(state);
    }

    @Deprecated
    protected boolean canSilkHarvest() {
        return getDefaultState().isFullCube() && !hasTileEntity(getDefaultState());
    }

    public legacyconnector.v1_12_2.net.minecraft.item.ItemStack mcc$silkTouchDrop(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return getSilkTouchDrop(state);
    }

    protected legacyconnector.v1_12_2.net.minecraft.item.ItemStack getSilkTouchDrop(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        legacyconnector.v1_12_2.net.minecraft.item.Item item = legacyconnector.v1_12_2.net.minecraft.item.Item.getItemFromBlock(this);
        if (item == null || item == legacyconnector.v1_12_2.net.minecraft.init.Items.AIR) return legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY;
        int meta = item.getHasSubtypes() ? getMetaFromState(state) : 0;
        return new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item, 1, meta);
    }

    /** The stack the pick-block key gives (no IBlockAccess form here: world blocks only). */
    public legacyconnector.v1_12_2.net.minecraft.item.ItemStack getItem(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        legacyconnector.v1_12_2.net.minecraft.item.Item item = legacyconnector.v1_12_2.net.minecraft.item.Item.getItemFromBlock(this);
        return item == null || item == legacyconnector.v1_12_2.net.minecraft.init.Items.AIR ? legacyconnector.v1_12_2.net.minecraft.item.ItemStack.EMPTY : new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(item, 1, damageDropped(state));
    }

    /** Forge: breaking by a player; true when the block was removed. */
    public boolean removedByPlayer(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            boolean willHarvest) {
        onBlockHarvested(world, pos, state, player);
        return world.setBlockState(pos, legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR.getDefaultState(), world.isRemote ? 11 : 3);
    }

    public boolean rotateBlock(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing axis) {
        var level = world == null ? null : world.mcc$level();
        if (level == null) return false;
        var mp = pos.mcc$modern();
        var state = level.getBlockState(mp);
        var rotated = state.rotate(level, mp, net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
        if (rotated == state) return false;
        level.setBlockAndUpdate(mp, rotated);
        return true;
    }

    public void onPlantGrow(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos source) {
    }

    public boolean getUseNeighborBrightness(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return useNeighborBrightness;
    }

    public SoundType getSoundType(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        return getSoundType();
    }

    public boolean canRenderInLayer(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer layer) {
        return getRenderLayer() == layer;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer getRenderLayer() {
        return legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer.SOLID;
    }

    public boolean isCollidable() {
        return true;
    }

    public boolean canCollideCheck(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, boolean hitIfLiquid) {
        return isCollidable();
    }

    public boolean canProvidePower(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return false;
    }

    public boolean hasComparatorInputOverride(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return false;
    }

    public int getComparatorInputOverride(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState blockState, legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return 0;
    }

    public boolean requiresUpdates() {
        return true;
    }

    public boolean isAssociatedBlock(Block other) {
        return this == other;
    }

    public static boolean isEqualTo(Block a, Block b) {
        return a != null && b != null && (a == b || a.isAssociatedBlock(b));
    }

    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState withRotation(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.util.Rotation rot) {
        return state;
    }

    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState withMirror(legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state, legacyconnector.v1_12_2.net.minecraft.util.Mirror mirror) {
        return state;
    }

    public boolean isToolEffective(String type, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return type != null && type.equals(getHarvestTool(state));
    }

    public float getEnchantPowerBonus(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return 0.0F;
    }
}
