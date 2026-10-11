package mccompat.v1_7_10.net.minecraft.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import mccompat.v1_7_10.net.minecraft.block.material.MapColor;
import mccompat.v1_7_10.net.minecraft.block.material.Material;
import mccompat.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import mccompat.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import mccompat.v1_7_10.net.minecraft.entity.item.EntityItem;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.tileentity.TileEntity;
import mccompat.v1_7_10.net.minecraft.util.AxisAlignedBB;
import mccompat.v1_7_10.net.minecraft.util.IIcon;
import mccompat.v1_7_10.net.minecraft.util.RegistryNamespaced;
import mccompat.v1_7_10.net.minecraft.util.StatCollector;
import mccompat.v1_7_10.net.minecraft.world.IBlockAccess;
import mccompat.v1_7_10.net.minecraft.world.World;
import mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import org.kylepitcock.mccompat.v1_7_10.bridge.BlockBridge;

/**
 * The 1.7.10 Block API. A legacy block object is pure logic: when it is registered, {@link BlockBridge} creates a
 * modern {@code LegacyBlockAdapter} (a real 1.21.1 block with a 0-15 {@code meta} property) that forwards every
 * gameplay callback back to this object using legacy coordinates, metadata and a legacy {@link World} view.
 * Vanilla blocks are represented by {@code VanillaBlock} subclasses that map metadata through the 1.13 flattening.
 */
public class Block {
    public static final RegistryNamespaced blockRegistry = BlockBridge.legacyRegistry();

    /** Forge: stable handle to this block. */
    public final mccompat.v1_7_10.cpw.mods.fml.common.registry.RegistryDelegate<Block> delegate =
            new mccompat.v1_7_10.cpw.mods.fml.common.registry.RegistryDelegate.Delegate<>(this, Block.class, blockRegistry);

    public static final SoundType soundTypeStone = new SoundType("stone", 1.0F, 1.0F);
    public static final SoundType soundTypeWood = new SoundType("wood", 1.0F, 1.0F);
    public static final SoundType soundTypeGravel = new SoundType("gravel", 1.0F, 1.0F);
    public static final SoundType soundTypeGrass = new SoundType("grass", 1.0F, 1.0F);
    public static final SoundType soundTypePiston = new SoundType("stone", 1.0F, 1.0F);
    public static final SoundType soundTypeMetal = new SoundType("stone", 1.0F, 1.5F);
    public static final SoundType soundTypeGlass = new SoundType("stone", 1.0F, 1.0F) {
        @Override
        public String getBreakSound() {
            return "dig.glass";
        }

        @Override
        public String getPlaceSound() {
            return "step.stone";
        }
    };
    public static final SoundType soundTypeCloth = new SoundType("cloth", 1.0F, 1.0F);
    public static final SoundType soundTypeSand = new SoundType("sand", 1.0F, 1.0F);
    public static final SoundType soundTypeSnow = new SoundType("snow", 1.0F, 1.0F);
    public static final SoundType soundTypeLadder = new SoundType("ladder", 1.0F, 1.0F) {
        @Override
        public String getBreakSound() {
            return "dig.wood";
        }
    };
    public static final SoundType soundTypeAnvil = new SoundType("anvil", 0.3F, 1.0F) {
        @Override
        public String getBreakSound() {
            return "dig.stone";
        }

        @Override
        public String getPlaceSound() {
            return "random.anvil_land";
        }
    };

    protected String textureName;
    protected boolean opaque;
    protected int lightOpacity;
    protected boolean canBlockGrass;
    protected int lightValue;
    protected boolean useNeighborBrightness;
    protected float blockHardness;
    protected float blockResistance;
    protected boolean blockConstructorCalled = true;
    protected boolean enableStats = true;
    protected boolean needsRandomTick;
    protected boolean isBlockContainer;
    protected double minX;
    protected double minY;
    protected double minZ;
    protected double maxX;
    protected double maxY;
    protected double maxZ;
    public SoundType stepSound;
    public float blockParticleGravity;
    protected final Material blockMaterial;
    public float slipperiness;
    private String unlocalizedName;
    protected IIcon blockIcon;
    private CreativeTabs displayOnCreativeTab;

    // Forge 1.7.10 harvest levels, indexed by metadata.
    private final String[] mcc$harvestTool = new String[16];
    private final int[] mcc$harvestLevel = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

    /** Modern block registered for this legacy block; null until registration (or for unregistered blocks). */
    public net.minecraft.world.level.block.Block mcc$modern;
    /** Non-null for wrappers of vanilla (or modern-only) blocks. */
    public final org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo mcc$vanilla;

    /** Wrapper constructor for vanilla blocks; properties come from the modern default state. */
    public Block(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        this.mcc$vanilla = info;
        this.blockMaterial = BlockBridge.vanillaMaterial(info);
        this.stepSound = soundTypeStone;
        this.blockParticleGravity = 1.0F;
        this.slipperiness = 0.6F;
        this.unlocalizedName = info.legacyName();
        this.textureName = info.legacyName();
        BlockBridge.initVanilla(this, info);
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        this.opaque = this.isOpaqueCube();
        this.canBlockGrass = !blockMaterial.getCanBlockGrass();
    }

    /** Lets the bridge copy modern properties into the protected legacy fields of a vanilla wrapper. */
    public void mcc$setVanillaProperties(float hardness, float resistance, int light, int opacity, boolean randomTicks) {
        this.blockHardness = hardness;
        this.blockResistance = resistance * 5.0F;
        this.lightValue = light;
        this.lightOpacity = opacity;
        this.needsRandomTick = randomTicks;
    }

    protected Block(Material material) {
        if (material == null) material = Material.rock; // synthesized subclasses may pass none
        this.mcc$vanilla = null;
        this.stepSound = soundTypeStone;
        this.blockParticleGravity = 1.0F;
        this.slipperiness = 0.6F;
        this.blockMaterial = material;
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        this.opaque = this.isOpaqueCube();
        this.lightOpacity = this.isOpaqueCube() ? 255 : 0;
        this.canBlockGrass = !material.getCanBlockGrass();
    }

    // ------------------------------------------------------------------ static lookups

    public static int getIdFromBlock(Block block) {
        return BlockBridge.idOf(block);
    }

    public static Block getBlockById(int id) {
        return BlockBridge.byId(id);
    }

    public static Block getBlockFromItem(Item item) {
        return BlockBridge.fromItem(item);
    }

    public static Block getBlockFromName(String name) {
        return BlockBridge.byName(name);
    }

    public static boolean isEqualTo(Block a, Block b) {
        return a != null && b != null && (a == b || a.isAssociatedBlock(b));
    }

    public static void registerBlocks() {
        // vanilla registration happens in the modern game
    }

    // ------------------------------------------------------------------ builder-style setters

    public Block setBlockName(String name) {
        this.unlocalizedName = name;
        return this;
    }

    public Block setBlockTextureName(String name) {
        this.textureName = name;
        return this;
    }

    public Block setHardness(float hardness) {
        this.blockHardness = hardness;
        if (this.blockResistance < hardness * 5.0F) this.blockResistance = hardness * 5.0F;
        return this;
    }

    public Block setResistance(float resistance) {
        this.blockResistance = resistance * 3.0F;
        return this;
    }

    public Block setBlockUnbreakable() {
        setHardness(-1.0F);
        return this;
    }

    public Block setStepSound(SoundType sound) {
        this.stepSound = sound;
        return this;
    }

    public Block setLightOpacity(int opacity) {
        this.lightOpacity = opacity;
        return this;
    }

    public Block setLightLevel(float level) {
        this.lightValue = (int) (15.0F * level);
        return this;
    }

    public Block setTickRandomly(boolean ticks) {
        this.needsRandomTick = ticks;
        return this;
    }

    public Block setCreativeTab(CreativeTabs tab) {
        this.displayOnCreativeTab = tab;
        return this;
    }

    public Block disableStats() {
        this.enableStats = false;
        return this;
    }

    public final void setBlockBounds(float x1, float y1, float z1, float x2, float y2, float z2) {
        this.minX = x1;
        this.minY = y1;
        this.minZ = z1;
        this.maxX = x2;
        this.maxY = y2;
        this.maxZ = z2;
    }

    public void setHarvestLevel(String toolClass, int level) {
        for (int m = 0; m < 16; m++) setHarvestLevel(toolClass, level, m);
    }

    public void setHarvestLevel(String toolClass, int level, int metadata) {
        if (metadata < 0 || metadata > 15) return;
        mcc$harvestTool[metadata] = toolClass;
        mcc$harvestLevel[metadata] = level;
    }

    public String getHarvestTool(int metadata) {
        return mcc$harvestTool[metadata & 15];
    }

    public int getHarvestLevel(int metadata) {
        return mcc$harvestLevel[metadata & 15];
    }

    public boolean isToolEffective(String type, int metadata) {
        return type != null && type.equals(getHarvestTool(metadata));
    }

    // ------------------------------------------------------------------ getters

    public String getUnlocalizedName() {
        return "tile." + this.unlocalizedName;
    }

    public String getLocalizedName() {
        return StatCollector.translateToLocal(getUnlocalizedName() + ".name");
    }

    public String getTextureName() {
        return this.textureName == null ? "MISSING_ICON_BLOCK_" + getIdFromBlock(this) + "_" + this.unlocalizedName : this.textureName;
    }

    public String getItemIconName() {
        return null;
    }

    public Material getMaterial() {
        return this.blockMaterial;
    }

    public MapColor getMapColor(int meta) {
        return blockMaterial.getMaterialMapColor();
    }

    public float getBlockHardness(World world, int x, int y, int z) {
        return this.blockHardness;
    }

    public float getExplosionResistance(Entity entity) {
        return this.blockResistance / 5.0F;
    }

    public float getExplosionResistance(Entity entity, World world, int x, int y, int z, double ex, double ey, double ez) {
        return getExplosionResistance(entity);
    }

    public boolean getTickRandomly() {
        return this.needsRandomTick;
    }

    public boolean getEnableStats() {
        return this.enableStats;
    }

    public int getLightOpacity() {
        return this.lightOpacity;
    }

    public int getLightOpacity(IBlockAccess world, int x, int y, int z) {
        return getLightOpacity();
    }

    public int getLightValue() {
        return this.lightValue;
    }

    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return getLightValue();
    }

    public boolean getCanBlockGrass() {
        return this.canBlockGrass;
    }

    public boolean getUseNeighborBrightness() {
        return this.useNeighborBrightness;
    }

    public CreativeTabs getCreativeTabToDisplayOn() {
        return this.displayOnCreativeTab;
    }

    public boolean isOpaqueCube() {
        return true;
    }

    public boolean renderAsNormalBlock() {
        return true;
    }

    public boolean isNormalCube() {
        return this.blockMaterial.isOpaque() && this.renderAsNormalBlock() && !this.canProvidePower();
    }

    public boolean isBlockNormalCube() {
        return this.blockMaterial.blocksMovement() && this.renderAsNormalBlock();
    }

    public boolean isNormalCube(IBlockAccess world, int x, int y, int z) {
        return isNormalCube();
    }

    public boolean isFullBlock() {
        return this.opaque;
    }

    public boolean isCollidable() {
        return true;
    }

    public boolean hasTileEntity() {
        return this.isBlockContainer;
    }

    public boolean hasTileEntity(int metadata) {
        return hasTileEntity();
    }

    public TileEntity createTileEntity(World world, int metadata) {
        return this instanceof ITileEntityProvider provider ? provider.createNewTileEntity(world, metadata) : null;
    }

    public int getRenderType() {
        return 0;
    }

    public int getRenderBlockPass() {
        return 0;
    }

    public int getMobilityFlag() {
        return this.blockMaterial.getMaterialMobility();
    }

    public int tickRate(World world) {
        return 10;
    }

    public boolean isAssociatedBlock(Block other) {
        return this == other;
    }

    public boolean isAir(IBlockAccess world, int x, int y, int z) {
        return getMaterial() == Material.air;
    }

    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return blockMaterial.isReplaceable();
    }

    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return isNormalCube();
    }

    public boolean isWood(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    public boolean isLeaves(IBlockAccess world, int x, int y, int z) {
        return getMaterial() == Material.leaves;
    }

    public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
        return getMaterial().getCanBurn();
    }

    public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
        return getMaterial().getCanBurn() ? 20 : 0;
    }

    public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
        return getMaterial().getCanBurn() ? 5 : 0;
    }

    public final double getBlockBoundsMinX() {
        return minX;
    }

    public final double getBlockBoundsMaxX() {
        return maxX;
    }

    public final double getBlockBoundsMinY() {
        return minY;
    }

    public final double getBlockBoundsMaxY() {
        return maxY;
    }

    public final double getBlockBoundsMinZ() {
        return minZ;
    }

    public final double getBlockBoundsMaxZ() {
        return maxZ;
    }

    // ------------------------------------------------------------------ rendering (client)

    public IIcon getIcon(int side, int meta) {
        return this.blockIcon;
    }

    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        return getIcon(side, world.getBlockMetadata(x, y, z));
    }

    public final IIcon getBlockTextureFromSide(int side) {
        return getIcon(side, 0);
    }

    public void registerBlockIcons(IIconRegister register) {
        this.blockIcon = register.registerIcon(getTextureName());
    }

    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }

    public int getBlockColor() {
        return 16777215;
    }

    public int getRenderColor(int meta) {
        return 16777215;
    }

    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return 16777215;
    }

    public void setBlockBoundsForItemRender() {
    }

    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
    }

    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
    }

    // ------------------------------------------------------------------ gameplay callbacks (called by the adapter)

    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        return false;
    }

    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
    }

    public void onBlockAdded(World world, int x, int y, int z) {
    }

    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (hasTileEntity(meta) && !(this instanceof BlockContainer)) world.removeTileEntity(x, y, z);
    }

    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
    }

    public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tileX, int tileY, int tileZ) {
    }

    public void updateTick(World world, int x, int y, int z, Random random) {
    }

    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        return meta;
    }

    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
    }

    public void onPostBlockPlaced(World world, int x, int y, int z, int meta) {
    }

    public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
    }

    public void onBlockDestroyedByPlayer(World world, int x, int y, int z, int meta) {
    }

    public void onBlockPreDestroy(World world, int x, int y, int z, int meta) {
    }

    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        return world.setBlockToAir(x, y, z);
    }

    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta) {
        dropBlockAsItem(world, x, y, z, meta, 0);
    }

    public boolean canHarvestBlock(EntityPlayer player, int meta) {
        return getMaterial().isToolNotRequired();
    }

    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
    }

    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
    }

    public void onFallenUpon(World world, int x, int y, int z, Entity entity, float fallDistance) {
    }

    public boolean onBlockEventReceived(World world, int x, int y, int z, int id, int param) {
        return false;
    }

    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.getBlock(x, y, z).isReplaceable(world, x, y, z);
    }

    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
        return canPlaceBlockAt(world, x, y, z);
    }

    public boolean canBlockStay(World world, int x, int y, int z) {
        return true;
    }

    public boolean canProvidePower() {
        return false;
    }

    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return 0;
    }

    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        return 0;
    }

    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
        return canProvidePower() && side != -1;
    }

    public boolean hasComparatorInputOverride() {
        return false;
    }

    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        return 0;
    }

    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
    }

    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
    }

    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
    }

    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, int x, int y, int z) {
        float hardness = getBlockHardness(world, x, y, z);
        if (hardness < 0.0F) return 0.0F;
        return player.mcc$breakSpeed(this, world.getBlockMetadata(x, y, z)) / hardness / 30.0F;
    }

    // ------------------------------------------------------------------ drops

    public int quantityDropped(Random random) {
        return 1;
    }

    public int quantityDroppedWithBonus(int fortune, Random random) {
        return quantityDropped(random);
    }

    public int quantityDropped(int meta, int fortune, Random random) {
        return quantityDroppedWithBonus(fortune, random);
    }

    public Item getItemDropped(int meta, Random random, int fortune) {
        return Item.getItemFromBlock(this);
    }

    public int damageDropped(int meta) {
        return 0;
    }

    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList<>();
        int count = quantityDropped(metadata, fortune, world.rand);
        for (int i = 0; i < count; i++) {
            Item item = getItemDropped(metadata, world.rand, fortune);
            if (item != null) ret.add(new ItemStack(item, 1, damageDropped(metadata)));
        }
        return ret;
    }

    public final void dropBlockAsItem(World world, int x, int y, int z, int meta, int fortune) {
        dropBlockAsItemWithChance(world, x, y, z, meta, 1.0F, fortune);
    }

    public void dropBlockAsItemWithChance(World world, int x, int y, int z, int meta, float chance, int fortune) {
        if (world.isRemote) return;
        for (ItemStack stack : getDrops(world, x, y, z, meta, fortune)) {
            if (world.rand.nextFloat() <= chance) dropBlockAsItem(world, x, y, z, stack);
        }
    }

    protected void dropBlockAsItem(World world, int x, int y, int z, ItemStack stack) {
        if (world.isRemote || stack == null || stack.mcc$count() <= 0) return;
        float f = 0.7F;
        double dx = world.rand.nextFloat() * f + (1.0F - f) * 0.5D;
        double dy = world.rand.nextFloat() * f + (1.0F - f) * 0.5D;
        double dz = world.rand.nextFloat() * f + (1.0F - f) * 0.5D;
        EntityItem item = new EntityItem(world, x + dx, y + dy, z + dz, stack);
        EntityItem.mcc$set$delayBeforeCanPickup(item, 10);
        world.spawnEntityInWorld(item);
    }

    public void dropXpOnBlockBreak(World world, int x, int y, int z, int amount) {
        world.mcc$dropXp(x, y, z, amount);
    }

    public int getExpDrop(IBlockAccess world, int metadata, int fortune) {
        return 0;
    }

    public boolean canSilkHarvest() {
        return this.renderAsNormalBlock() && !this.isBlockContainer;
    }

    public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        return canSilkHarvest() && !hasTileEntity(metadata);
    }

    protected ItemStack createStackedBlock(int meta) {
        Item item = Item.getItemFromBlock(this);
        return item == null ? null : new ItemStack(item, 1, item.getHasSubtypes() ? meta : 0);
    }

    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(this);
    }

    public int getDamageValue(World world, int x, int y, int z) {
        return damageDropped(world.getBlockMetadata(x, y, z));
    }

    public float mcc$hardness() {
        return blockHardness;
    }

    public ItemStack mcc$createStackedBlock(int meta) {
        return createStackedBlock(meta);
    }

    @Override
    public String toString() {
        return "LegacyBlock{" + unlocalizedName + "}";
    }

    // ------------------------------------------------------------------ sounds

    public static class SoundType {
        public final String soundName;
        public final float volume;
        public final float frequency;

        public SoundType(String name, float volume, float frequency) {
            this.soundName = name;
            this.volume = volume;
            this.frequency = frequency;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return frequency;
        }

        public String getBreakSound() {
            return "dig." + soundName;
        }

        public String getStepResourcePath() {
            return "step." + soundName;
        }

        public String getPlaceSound() {
            return getBreakSound();
        }
    }

    /**
     * Forge: may ore generation targeting {@code target} replace this block? Generation that targets stone also
     * replaces the modern stone variants (granite, deepslate, tuff, ...) that took its place underground.
     */
    public boolean isReplaceableOreGen(World world, int x, int y, int z, Block target) {
        if (this == target) return true;
        if (target == mccompat.v1_7_10.net.minecraft.init.Blocks.stone && mcc$modern != null) {
            var state = mcc$modern.defaultBlockState();
            return state.is(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES) || state.is(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        }
        return false;
    }
}
