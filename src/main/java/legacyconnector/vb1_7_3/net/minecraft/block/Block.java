package legacyconnector.vb1_7_3.net.minecraft.block;

import java.util.Random;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.sound.BlockSoundGroup;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;

/**
 * Beta 1.7.3 {@code Block} (obfuscated {@code uu}; MCP {@code Block}). A block is a numeric id (0-255) in {@link #BLOCKS} plus 4 bits of
 * metadata in the world; its textures are sprite indices into terrain.png ({@link #textureId}, {@link #getTexture(int, int)}). The
 * constructor claims the id exactly as the original did (an occupied slot throws). Content of ModLoader mods becomes modern blocks after
 * the mods' lifecycle ({@code BridgeB173}); vanilla blocks are views of the modern ones ({@code VanillaB173}). Members that take worlds,
 * entities or render types are not here yet: they are synthesized and reach the mod's own overrides.
 */
public class Block {
    public static final BlockSoundGroup DEFAULT_SOUND_GROUP = new BlockSoundGroup("stone", 1.0F, 1.0F);
    public static final BlockSoundGroup WOOD_SOUND_GROUP = new BlockSoundGroup("wood", 1.0F, 1.0F);
    public static final BlockSoundGroup GRAVEL_SOUND_GROUP = new BlockSoundGroup("gravel", 1.0F, 1.0F);
    public static final BlockSoundGroup DIRT_SOUND_GROUP = new BlockSoundGroup("grass", 1.0F, 1.0F);
    public static final BlockSoundGroup STONE_SOUND_GROUP = new BlockSoundGroup("stone", 1.0F, 1.0F);
    public static final BlockSoundGroup METAL_SOUND_GROUP = new BlockSoundGroup("stone", 1.0F, 1.5F);
    public static final BlockSoundGroup GLASS_SOUND_GROUP = new BlockSoundGroup("stone", 1.0F, 1.0F) {
        @Override
        public String getBreakSound() {
            return "random.glass";
        }
    };
    public static final BlockSoundGroup WOOL_SOUND_GROUP = new BlockSoundGroup("cloth", 1.0F, 1.0F);
    public static final BlockSoundGroup SAND_SOUND_GROUP = new BlockSoundGroup("sand", 1.0F, 1.0F) {
        @Override
        public String getBreakSound() {
            return "step.gravel";
        }
    };

    public static Block[] BLOCKS = new Block[256];
    public static boolean[] BLOCKS_RANDOM_TICK = new boolean[256];
    public static boolean[] BLOCKS_OPAQUE = new boolean[256];
    public static boolean[] BLOCKS_WITH_ENTITY = new boolean[256];
    public static int[] BLOCKS_LIGHT_OPACITY = new int[256];
    public static boolean[] BLOCKS_ALLOW_VISION = new boolean[256];
    public static int[] BLOCKS_LIGHT_LUMINANCE = new int[256];
    public static boolean[] BLOCKS_IGNORE_META_UPDATE = new boolean[256];

    public int textureId;
    public int id;
    public float hardness;
    public float resistance;
    public boolean constructed = true;
    public boolean shouldTrackStatistics = true;
    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;
    public BlockSoundGroup soundGroup = DEFAULT_SOUND_GROUP;
    public float particleFallSpeedModifier = 1.0F;
    public Material material;
    public float slipperiness = 0.6F;
    public String translationKey;

    /** The modern block this one is (vanilla) or became (mods); null until the bridge registered it. */
    public net.minecraft.world.level.block.Block mcc$modern;
    /** The legacy mod that constructed the block, or null for vanilla. */
    public Object mcc$owner;

    protected Block(int id, Material material) {
        if (BLOCKS[id] != null) {
            throw new IllegalArgumentException("Slot " + id + " is already occupied by " + BLOCKS[id] + " when adding " + this);
        }
        this.material = material;
        BLOCKS[id] = this;
        this.id = id;
        setBoundingBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        BLOCKS_OPAQUE[id] = isOpaque();
        BLOCKS_LIGHT_OPACITY[id] = isOpaque() ? 255 : 0;
        BLOCKS_ALLOW_VISION[id] = material == null || !material.blocksVision();
        BLOCKS_WITH_ENTITY[id] = false;
        mcc$owner = ContentB173.claimBlock(this);
    }

    protected Block(int id, int textureId, Material material) {
        this(id, material);
        this.textureId = textureId;
    }

    /** For the vanilla views, which are allocated without a constructor ({@code VanillaB173}). */
    public void mcc$initVanilla(int id, int textureId, Material material, float hardness, BlockSoundGroup sound, String key) {
        this.id = id;
        this.textureId = textureId;
        this.material = material;
        this.hardness = hardness;
        this.resistance = hardness * 5.0F;
        this.soundGroup = sound == null ? DEFAULT_SOUND_GROUP : sound;
        this.translationKey = key;
        this.constructed = true;
        this.shouldTrackStatistics = true;
        this.slipperiness = 0.6F;
        this.particleFallSpeedModifier = 1.0F;
        this.maxX = this.maxY = this.maxZ = 1.0;
    }

    protected Block ignoreMetaUpdates() {
        BLOCKS_IGNORE_META_UPDATE[id] = true;
        return this;
    }

    protected void init() {}

    protected Block setSoundGroup(BlockSoundGroup group) {
        this.soundGroup = group;
        return this;
    }

    protected Block setOpacity(int opacity) {
        BLOCKS_LIGHT_OPACITY[id] = opacity;
        return this;
    }

    protected Block setLuminance(float luminance) {
        BLOCKS_LIGHT_LUMINANCE[id] = (int) (15.0F * luminance);
        return this;
    }

    protected Block setResistance(float resistance) {
        this.resistance = resistance * 3.0F;
        return this;
    }

    public boolean isFullCube() {
        return true;
    }

    public int getRenderType() {
        return 0;
    }

    protected Block setHardness(float hardness) {
        this.hardness = hardness;
        if (this.resistance < hardness * 5.0F) this.resistance = hardness * 5.0F;
        return this;
    }

    protected Block setUnbreakable() {
        setHardness(-1.0F);
        return this;
    }

    public float getHardness() {
        return hardness;
    }

    /** Forge's metadata-aware hardness. */
    public float getHardness(int meta) {
        return hardness;
    }

    protected Block setTickRandomly(boolean ticks) {
        BLOCKS_RANDOM_TICK[id] = ticks;
        return this;
    }

    public void setBoundingBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public int getTexture(int side, int meta) {
        return getTexture(side);
    }

    public int getTexture(int side) {
        return textureId;
    }

    public boolean isOpaque() {
        return true;
    }

    public boolean hasCollision(int meta, boolean allowLiquids) {
        return hasCollision();
    }

    public boolean hasCollision() {
        return true;
    }

    public int getTickRate() {
        return 10;
    }

    public int getDroppedItemCount(Random random) {
        return 1;
    }

    public int getDroppedItemId(int meta, Random random) {
        return id;
    }

    public int getDroppedItemMeta(int meta) {
        return 0;
    }

    public int getRenderLayer() {
        return 0;
    }

    public int getColor(int meta) {
        return 0xFFFFFF;
    }

    public boolean canEmitRedstonePower() {
        return false;
    }

    public void setupRenderBoundingBox() {}

    public Block setTranslationKey(String key) {
        this.translationKey = "tile." + key;
        return this;
    }

    public String getTranslatedName() {
        return ContentB173.translate(getTranslationKey() + ".name");
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean isTrackingStatistics() {
        return shouldTrackStatistics;
    }

    protected Block disableTrackingStatistics() {
        shouldTrackStatistics = false;
        return this;
    }

    public int getPistonBehavior() {
        return material == null ? 0 : material.getPistonBehavior();
    }

    /** Forge. */
    public boolean isLadder() {
        return false;
    }

    // ------------------------------------------------------------------------------------------------- world callbacks
    // b1.7.3's defaults; the modern block (LegacyB173Block) calls them with a legacy view of the level.

    /** MCP {@code onBlockAdded}. */
    public void onPlaced(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z) {}

    /** MCP {@code onBlockRemoval}. */
    public void onBreak(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z) {}

    /** MCP {@code onNeighborBlockChange}: {@code neighborId} is the neighbour's block id. */
    public void neighborUpdate(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z, int neighborId) {}

    /** MCP {@code updateTick}: scheduled and random ticks. */
    public void onTick(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z, Random random) {}

    /** MCP {@code randomDisplayTick}: client particles. */
    public void randomDisplayTick(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z, Random random) {}

    /** MCP {@code onBlockDestroyedByPlayer}. */
    public void onMetadataChange(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z, int meta) {}

    /** MCP {@code blockActivated}: right click; true if the block used the click. */
    public boolean onUse(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z,
            legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player) {
        return false;
    }

    /** MCP {@code onBlockClicked}: left click. */
    public void onBlockBreakStart(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z,
            legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player) {}

    /** MCP {@code onEntityWalking}. */
    public void onSteppedOn(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z,
            legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity) {}

    /** MCP {@code onEntityCollidedWithBlock}. */
    public void onEntityCollision(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z,
            legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity) {}

    /** MCP {@code onBlockPlaced}: {@code side} is the face of the block clicked to place it. */
    public void onPlaced(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z, int side) {}

    /** MCP {@code onBlockPlacedBy}. */
    public void onPlaced(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z,
            legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity placer) {}

    /** MCP {@code playBlock}: note blocks, pistons. */
    public void onBlockAction(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z, int a, int b) {}

    /** MCP {@code harvestBlock}: the drops are the modern block's ({@code getDroppedItemId/Count/Meta}). */
    public void afterBreak(legacyconnector.vb1_7_3.net.minecraft.world.World world, legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player,
            int x, int y, int z, int meta) {}

    /** MCP {@code canPlaceBlockAt}. */
    public boolean canPlaceAt(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z) {
        int id = world.getBlockId(x, y, z);
        return id == 0 || BLOCKS[id] == null || BLOCKS[id].material == null || BLOCKS[id].material.isReplaceable();
    }

    /** MCP {@code canPlaceBlockOnSide}. */
    public boolean canPlaceAt(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z, int side) {
        return canPlaceAt(world, x, y, z);
    }

    /** MCP {@code isPoweringTo}: weak power towards {@code side}. */
    public boolean isEmittingRedstonePowerInDirection(legacyconnector.vb1_7_3.net.minecraft.world.BlockView view, int x, int y, int z, int side) {
        return false;
    }

    /** MCP {@code isIndirectlyPoweringTo}: strong power towards {@code side}. */
    public boolean canTransferPowerInDirection(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z, int side) {
        return false;
    }

    /** MCP {@code setBlockBoundsBasedOnState}. */
    public void updateBoundingBox(legacyconnector.vb1_7_3.net.minecraft.world.BlockView view, int x, int y, int z) {}

    public int getColorMultiplier(legacyconnector.vb1_7_3.net.minecraft.world.BlockView view, int x, int y, int z) {
        return 0xFFFFFF;
    }

    /** MCP {@code getBlockTexture}: the sprite of a side as placed in the world. */
    public int getTextureId(legacyconnector.vb1_7_3.net.minecraft.world.BlockView view, int x, int y, int z, int side) {
        return getTexture(side, view.getBlockMeta(x, y, z));
    }

    /** Forge's position-aware light. */
    public int getLightValue(legacyconnector.vb1_7_3.net.minecraft.world.BlockView view, int x, int y, int z) {
        return BLOCKS_LIGHT_LUMINANCE[id];
    }

    /** MCP {@code dropBlockAsItem}. */
    public final void dropStacks(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z, int meta) {
        dropStacks(world, x, y, z, meta, 1.0F);
    }

    /** MCP {@code dropBlockAsItemWithChance}. */
    public void dropStacks(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z, int meta, float chance) {
        if (world.isRemote) return;
        int count = getDroppedItemCount(world.random);
        for (int i = 0; i < count; i++) {
            if (world.random.nextFloat() > chance) continue;
            int dropped = getDroppedItemId(meta, world.random);
            if (dropped > 0) dropStack(world, x, y, z, new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(dropped, 1, getDroppedItemMeta(meta)));
        }
    }

    /** MCP {@code dropBlockAsItem_do}: an item entity at the block. */
    protected void dropStack(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z,
            legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack) {
        world.mcc$dropStack(x, y, z, stack);
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + id + (translationKey != null ? ", " + translationKey : "") + "]";
    }
}
