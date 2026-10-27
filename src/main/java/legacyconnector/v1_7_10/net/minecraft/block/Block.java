package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.MapColor;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.util.RegistryNamespaced;
import legacyconnector.v1_7_10.net.minecraft.util.StatCollector;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;

/**
 * The 1.7.10 Block API. A legacy block object is pure logic: when it is registered, {@link BlockBridge} creates a
 * modern {@code LegacyBlockAdapter} (a real 1.21.1 block with a 0-15 {@code meta} property) that forwards every
 * gameplay callback back to this object using legacy coordinates, metadata and a legacy {@link World} view.
 * Vanilla blocks are represented by {@code VanillaBlock} subclasses that map metadata through the 1.13 flattening.
 */
public class Block {
    public static final RegistryNamespaced blockRegistry = BlockBridge.legacyRegistry();

    /** Forge: stable handle to this block. */
    public final legacyconnector.v1_7_10.cpw.mods.fml.common.registry.RegistryDelegate<Block> delegate =
            new legacyconnector.v1_7_10.cpw.mods.fml.common.registry.RegistryDelegate.Delegate<>(this, Block.class, blockRegistry);

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
    /** Forge: whether this block provides a tile entity (mods reflect on it). */
    protected boolean isTileProvider = this instanceof ITileEntityProvider;
    protected IIcon blockIcon;
    private CreativeTabs displayOnCreativeTab;

    // Forge 1.7.10 harvest levels, indexed by metadata.
    private final String[] mcc$harvestTool = new String[16];
    private final int[] mcc$harvestLevel = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

    /** Modern block registered for this legacy block; null until registration (or for unregistered blocks). */
    public net.minecraft.world.level.block.Block mcc$modern;
    /** Non-null for wrappers of vanilla (or modern-only) blocks. */
    public org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo mcc$vanilla;

    /** Wrapper constructor for vanilla blocks; properties come from the modern default state. */
    public Block(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
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
        if (mcc$plainVanilla()) return mcc$modernSolid(true);
        return org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaBlockConstants.get(getClass(), "func_149662_c", 1) != 0;
    }

    public boolean renderAsNormalBlock() {
        if (mcc$plainVanilla()) return mcc$modernSolid(true);
        return org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaBlockConstants.get(getClass(), "func_149686_d", 1) != 0;
    }

    /** A vanilla block wrapped as a plain Block: the constant table only knows Block's own defaults for it. */
    private boolean mcc$plainVanilla() {
        return mcc$vanilla != null && getClass() == Block.class;
    }

    /**
     * A vanilla block wrapped as a plain Block (air, water, torches, rails, crops...: 87 of them) has no constant: whether it is
     * a solid opaque cube comes from the modern block (air read as opaque, so mods saw no room above anything).
     */
    private boolean mcc$modernSolid(boolean fallback) {
        if (mcc$vanilla == null) return fallback;
        try {
            var state = mcc$modern != null ? mcc$modern.defaultBlockState()
                    : org.kylepitcock.legacyconnector.core.flatten.VanillaFlattening.state(mcc$vanilla.legacyId(), 0);
            if (state == null) return fallback;
            return state.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO);
        } catch (RuntimeException e) {
            return fallback;
        }
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
        // Forge: isTileProvider, set from "this instanceof ITileEntityProvider"; BlockContainer also sets isBlockContainer.
        return this.isBlockContainer || this instanceof ITileEntityProvider;
    }

    public boolean hasTileEntity(int metadata) {
        return hasTileEntity();
    }

    public TileEntity createTileEntity(World world, int metadata) {
        return this instanceof ITileEntityProvider provider ? provider.createNewTileEntity(world, metadata) : null;
    }

    public int getRenderType() {
        return org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaBlockConstants.get(getClass(), "func_149645_b", 0);
    }

    public int getRenderBlockPass() {
        return org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaBlockConstants.get(getClass(), "func_149701_w", 0);
    }

    /** Forge: whether the block is drawn in world render pass 0 (cut out) or 1 (translucent); some draw in both. */
    public boolean canRenderInPass(int pass) {
        return pass == getRenderBlockPass();
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

    // ---- Forge hooks 1.7.10 mods override or call (defaults as Forge had them)

    /** Wrench/rotation: rotates this block about {@code axis}; false when it cannot. */
    public boolean rotateBlock(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, ForgeDirection axis) {
        return false;
    }

    public ForgeDirection[] getValidRotations(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return null;
    }

    public boolean recolourBlock(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, ForgeDirection side, int colour) {
        return false;
    }

    /** Whether ray traces may hit this block for this metadata ({@code boolean}: the player is holding the block's item). */
    public boolean canCollideCheck(int meta, boolean includeLiquid) {
        return isCollidable();
    }

    public void beginLeavesDecay(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
    }

    public float getAmbientOcclusionLightValue() {
        return isNormalCube() ? 0.2F : 1.0F;
    }

    public legacyconnector.v1_7_10.net.minecraft.item.ItemStack getPickBlock(legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition target,
            legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        Item item = Item.getItemFromBlock(this);
        if (item == null) return null;
        return new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item, 1, getDamageValue(world, x, y, z));
    }

    public legacyconnector.v1_7_10.net.minecraft.item.ItemStack getPickBlock(legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition target,
            legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        return getPickBlock(target, world, x, y, z);
    }

    public boolean isFireSource(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, ForgeDirection side) {
        return this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.netherrack && side == ForgeDirection.UP;
    }

    public boolean isFertile(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        return this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.farmland && world.getBlockMetadata(x, y, z) > 0;
    }

    public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
        return !getMaterial().blocksMovement();
    }

    public boolean canEntityDestroy(IBlockAccess world, int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        return true;
    }

    public void velocityToAddToEntity(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, legacyconnector.v1_7_10.net.minecraft.util.Vec3 velocity) {
    }

    public boolean isBed(IBlockAccess world, int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase player) {
        return this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.bed;
    }

    /** Forge: as Forge had it, true for the half with the head bit (8) set. */
    public boolean isBedFoot(IBlockAccess world, int x, int y, int z) {
        return BlockBed.isBlockHeadOfBed(world.getBlockMetadata(x, y, z));
    }

    public int getBedDirection(IBlockAccess world, int x, int y, int z) {
        return BlockDirectional.getDirection(world.getBlockMetadata(x, y, z));
    }

    public legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates getBedSpawnPosition(IBlockAccess world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        return world instanceof World w ? BlockBed.getSafeExitLocation(w, x, y, z, 0) : null;
    }

    /** Forge: marks the bed occupied (metadata bit 4) or free. */
    public void setBedOccupied(IBlockAccess world, int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player,
            boolean occupied) {
        if (world instanceof World w) BlockBed.setBedOccupied(w, x, y, z, occupied);
    }

    public boolean isBurning(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    public boolean shouldCheckWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return isNormalCube();
    }

    public boolean getWeakChanges(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    public boolean addDestroyEffects(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int meta,
            legacyconnector.v1_7_10.net.minecraft.client.particle.EffectRenderer effects) {
        return false;
    }

    public boolean isLeaves(IBlockAccess world, int x, int y, int z) {
        return getMaterial() == Material.leaves;
    }

    public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
        return getFlammability(world, x, y, z, face) > 0;
    }

    public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
        return legacyconnector.v1_7_10.net.minecraft.init.Blocks.fire.getFlammability(this);
    }

    public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
        return legacyconnector.v1_7_10.net.minecraft.init.Blocks.fire.getEncouragement(this);
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
        if (this.blockIcon == null && mcc$vanilla != null) return mcc$vanillaBlockIcon(side, meta);
        return this.blockIcon;
    }

    /** The icon of a vanilla block's face under its 1.7.10 name (see {@code VanillaIcons}). */
    protected final IIcon mcc$vanillaBlockIcon(int side, int meta) {
        return mcc$vanillaIcon(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaIcons.name(this, textureName, side, meta));
    }

    private static final legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap VANILLA_ICONS =
            new legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap(0, "textures/blocks");

    /** The icon of a vanilla texture under its 1.7.10 name (mods ask vanilla blocks for their icons). */
    protected static IIcon mcc$vanillaIcon(String name) {
        return name == null ? null : VANILLA_ICONS.registerIcon("minecraft:" + name);
    }

    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        return getIcon(side, world.getBlockMetadata(x, y, z));
    }

    /** The icon the block's item draws with (glass panes override it); the side's icon by default. */
    public IIcon getItemIcon(int side, int meta) {
        return getIcon(side, meta);
    }

    public final IIcon getBlockTextureFromSide(int side) {
        return getIcon(side, 0);
    }

    public void registerBlockIcons(IIconRegister register) {
        this.blockIcon = register.registerIcon(getTextureName());
    }

    /** 1.7.10: a side shows unless it is flush with the block's cell and the neighbour (at x, y, z) is an opaque cube. */
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        if (side == 0 && minY > 0.0D || side == 1 && maxY < 1.0D || side == 2 && minZ > 0.0D || side == 3 && maxZ < 1.0D
                || side == 4 && minX > 0.0D || side == 5 && maxX < 1.0D) return true;
        Block neighbour = world.getBlock(x, y, z);
        return neighbour == null || !neighbour.isOpaqueCube();
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

    /** Called now and then while it rains on the block (cauldrons fill up). */
    public void fillWithRain(World world, int x, int y, int z) {
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

    /**
     * 1.7.10 had one update for scheduled and random ticks. For a vanilla block it runs the modern block's random tick (crop
     * and sapling growth, which growth lamps and accelerators call it for) or, for blocks without one, its scheduled tick.
     */
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (mcc$vanilla == null || world == null || !(world.mcc$liveLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        var pos = new net.minecraft.core.BlockPos(x, y, z);
        var state = level.getBlockState(pos);
        if (state.getBlock() != org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.modernBlock(this)) return;
        if (state.isRandomlyTicking()) state.randomTick(level, pos, level.random);
        else state.tick(level, pos, level.random);
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

    /** Forge: how the block is removed when a player breaks it (return false to keep it). */
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
        return world.setBlockToAir(x, y, z);
    }

    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        return removedByPlayer(world, player, x, y, z);
    }

    /** Vanilla: adds the collision boxes that intersect {@code mask} (blocks with several boxes override this). */
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
        AxisAlignedBB box = getCollisionBoundingBoxFromPool(world, x, y, z);
        if (box != null && mask.intersectsWith(box)) list.add(box);
    }

    /** Forge: whether entities can climb this block. */
    public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity) {
        return false;
    }

    /** Forge: whether the plant can grow on this block from the given side (the default rules of 1.7.10 Forge). */
    public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction,
            legacyconnector.v1_7_10.net.minecraftforge.common.IPlantable plantable) {
        Block plant = plantable.getPlant(world, x, y + direction.offsetY, z);
        legacyconnector.v1_7_10.net.minecraftforge.common.EnumPlantType type = plantable.getPlantType(world, x, y + direction.offsetY, z);
        if (plant == legacyconnector.v1_7_10.net.minecraft.init.Blocks.cactus && this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.cactus) return true;
        if (plant == legacyconnector.v1_7_10.net.minecraft.init.Blocks.reeds && this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.reeds) return true;
        if (plantable instanceof BlockBush bush && bush.canPlaceBlockOn(this)) return true;
        if (type == null) return false;
        switch (type) {
            case Desert:
                return this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.sand;
            case Nether:
                return this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.soul_sand;
            case Crop:
                return this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.farmland;
            case Cave:
                return isSideSolid(world, x, y, z, ForgeDirection.UP);
            case Plains:
                return this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.grass || this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.dirt
                        || this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.farmland;
            case Water:
                return world.getBlock(x, y, z).getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.water
                        && world.getBlockMetadata(x, y, z) == 0;
            case Beach: {
                boolean isBeach = this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.grass || this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.dirt
                        || this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.sand;
                boolean hasWater = world.getBlock(x - 1, y, z).getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.water
                        || world.getBlock(x + 1, y, z).getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.water
                        || world.getBlock(x, y, z - 1).getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.water
                        || world.getBlock(x, y, z + 1).getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.water;
                return isBeach && hasWater;
            }
            default:
                return false;
        }
    }

    /** Forge: whether a creature type can spawn on this block. */
    public boolean canCreatureSpawn(legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        return isSideSolid(world, x, y, z, ForgeDirection.UP);
    }

    /** Forge: the block is destroyed by an explosion. */
    public void onBlockExploded(World world, int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.world.Explosion explosion) {
        world.setBlockToAir(x, y, z);
        onBlockDestroyedByExplosion(world, x, y, z, explosion);
    }

    public void onBlockDestroyedByExplosion(World world, int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.world.Explosion explosion) {
    }

    public boolean canDropFromExplosion(legacyconnector.v1_7_10.net.minecraft.world.Explosion explosion) {
        return true;
    }

    /** Forge: how much this block adds to an enchanting table's power (bookshelves: 1). */
    public float getEnchantPowerBonus(World world, int x, int y, int z) {
        return this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.bookshelf ? 1.0F : 0.0F;
    }

    /** Forge: whether the block can be part of a beacon pyramid (vanilla: emerald, gold, diamond and iron blocks). */
    public boolean isBeaconBase(IBlockAccess world, int x, int y, int z, int beaconX, int beaconY, int beaconZ) {
        return this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.emerald_block || this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.gold_block
                || this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.diamond_block || this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.iron_block;
    }

    public boolean isFoliage(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    public boolean canBeReplacedByLeaves(IBlockAccess world, int x, int y, int z) {
        // vanilla blocks are wrappers that do not know their shape: air and leaves are replaceable whatever isOpaqueCube says
        return !isOpaqueCube() || this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.air || getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.air
                || getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.leaves;
    }

    public boolean canSustainLeaves(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    /** Vanilla: whether the block at the position (a neighbour, as liquids ask) is of a solid material. */
    public boolean isBlockSolid(IBlockAccess world, int x, int y, int z, int side) {
        Block block = world == null ? null : world.getBlock(x, y, z);
        return (block != null ? block : this).getMaterial().isSolid();
    }

    public int getMixedBrightnessForBlock(IBlockAccess world, int x, int y, int z) {
        return world.getLightBrightnessForSkyBlocks(x, y, z, getLightValue());
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

    /** Ray against the block's bounds; the hit is in world coordinates, null when the ray misses. */
    public legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition collisionRayTrace(World world, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.util.Vec3 start, legacyconnector.v1_7_10.net.minecraft.util.Vec3 end) {
        setBlockBoundsBasedOnState(world, x, y, z);
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(minX, minY, minZ, maxX, maxY, maxZ);
        var from = new net.minecraft.world.phys.Vec3(start.xCoord - x, start.yCoord - y, start.zCoord - z);
        var to = new net.minecraft.world.phys.Vec3(end.xCoord - x, end.yCoord - y, end.zCoord - z);
        var hit = net.minecraft.world.phys.AABB.clip(java.util.List.of(box), from, to, net.minecraft.core.BlockPos.ZERO);
        if (hit == null) return null;
        var at = hit.getLocation();
        return new legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition(x, y, z, hit.getDirection().get3DDataValue(),
                legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(at.x + x, at.y + y, at.z + z));
    }

    public boolean canPlaceTorchOnTop(World world, int x, int y, int z) {
        if (isSideSolid(world, x, y, z, ForgeDirection.UP)) return true;
        for (Class<?> c = getClass(); c != null; c = c.getSuperclass()) if (c.getSimpleName().equals("BlockFence")) return true;
        return this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.glass || this == legacyconnector.v1_7_10.net.minecraft.init.Blocks.cobblestone_wall;
    }

    public boolean canReplace(World world, int x, int y, int z, int side, ItemStack stack) {
        return blockMaterial.isReplaceable();
    }

    /** Forge: called after a plant on this block grows. */
    public void onPlantGrow(World world, int x, int y, int z, int sourceX, int sourceY, int sourceZ) {
    }

    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
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

    /** Forge: while capturing (see {@link #captureDrops}), drops are collected instead of spawned. */
    protected ThreadLocal<Boolean> captureDrops = ThreadLocal.withInitial(() -> Boolean.FALSE);
    protected ThreadLocal<java.util.List<ItemStack>> capturedDrops = ThreadLocal.withInitial(java.util.ArrayList::new);

    /** Forge: start capturing drops (returns null), or stop and return what was captured. */
    protected java.util.List<ItemStack> captureDrops(boolean start) {
        if (start) {
            captureDrops.set(Boolean.TRUE);
            capturedDrops.get().clear();
            return null;
        }
        captureDrops.set(Boolean.FALSE);
        return capturedDrops.get();
    }

    protected void dropBlockAsItem(World world, int x, int y, int z, ItemStack stack) {
        if (world.isRemote || stack == null || stack.mcc$count() <= 0) return;
        if (captureDrops.get()) {
            capturedDrops.get().add(stack);
            return;
        }
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

    /** Forge: the player harvesting the block, set around getDrops/harvestBlock. */
    protected ThreadLocal<legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer> harvesters = new ThreadLocal<>();

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
        if (target == legacyconnector.v1_7_10.net.minecraft.init.Blocks.stone && mcc$modern != null) {
            var state = mcc$modern.defaultBlockState();
            return state.is(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES) || state.is(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        }
        return false;
    }

    public boolean addHitEffects(legacyconnector.v1_7_10.net.minecraft.world.World world, legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition target,
            legacyconnector.v1_7_10.net.minecraft.client.particle.EffectRenderer effects) {
        return false;
    }

    public boolean isFlowerPot() {
        return false;
    }
}
