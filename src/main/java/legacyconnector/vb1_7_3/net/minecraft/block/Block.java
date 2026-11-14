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
    public legacyconnector.vb1_7_3.net.minecraft.util.math.Box getCollisionShape(legacyconnector.vb1_7_3.net.minecraft.world.World world,int x,int y,int z) {
        if(mcc$owner!=null) return getBoundingBox(world,x,y,z);
        if (world.mcc$level()==null) return null;
        var pos=new net.minecraft.core.BlockPos(x,y,z);
        var shape=world.mcc$level().getBlockState(pos).getCollisionShape(world.mcc$level(),pos);
        if(shape.isEmpty())return null;
        var b=shape.bounds().move(x,y,z);
        return legacyconnector.vb1_7_3.net.minecraft.util.math.Box.create(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ);
    }
    public legacyconnector.vb1_7_3.net.minecraft.util.math.Box getBoundingBox(legacyconnector.vb1_7_3.net.minecraft.world.World world,int x,int y,int z) {
        return legacyconnector.vb1_7_3.net.minecraft.util.math.Box.create(x+minX,y+minY,z+minZ,x+maxX,y+maxY,z+maxZ);
    }
    /** Selection bounds are independent of the collision box; subclasses may customize this callback. */
    public legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult raycast(
            legacyconnector.vb1_7_3.net.minecraft.world.World world,int x,int y,int z,
            legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d from,legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d to) {
        updateBoundingBox(world,x,y,z);
        var hit=legacyconnector.vb1_7_3.net.minecraft.util.math.Box.create(x+minX,y+minY,z+minZ,x+maxX,y+maxY,z+maxZ).raycast(from,to);
        return hit==null?null:new legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult(x,y,z,hit.side,hit.pos);
    }
    public void addIntersectingBoundingBox(legacyconnector.vb1_7_3.net.minecraft.world.World world,int x,int y,int z,
            legacyconnector.vb1_7_3.net.minecraft.util.math.Box mask,java.util.ArrayList<legacyconnector.vb1_7_3.net.minecraft.util.math.Box> boxes) {
        var box=getCollisionShape(world,x,y,z);if(box!=null && box.intersects(mask))boxes.add(box);
    }
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
    public float getBlastResistance(legacyconnector.vb1_7_3.net.minecraft.entity.Entity source){return mcc$owner==null&&mcc$modern!=null?mcc$modern.getExplosionResistance():resistance/5F;}
    /** The vanilla base hook is empty; original block subclasses override it (for example priming explosives). */
    public void onDestroyedByExplosion(legacyconnector.vb1_7_3.net.minecraft.world.World world,int x,int y,int z){
        if(mcc$owner==null&&mcc$modern instanceof net.minecraft.world.level.block.TntBlock&&world.mcc$level()!=null&&!world.mcc$level().isClientSide){
            var tnt=new net.minecraft.world.entity.item.PrimedTnt(world.mcc$level(),x+.5,y+.5,z+.5,null);tnt.setFuse(world.random.nextInt(tnt.getFuse()/4)+tnt.getFuse()/8);world.mcc$spawnNative(tnt);
        }
    }

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
        if(mcc$owner==null&&mcc$modern!=null&&!(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block)){
            Boolean original=org.kylepitcock.legacyconnector.vb1_7_3.VanillaB173.fullCube(id);
            if(original!=null)return original;
        }
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
        return mcc$modern != null && !(mcc$modern instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block)
                && mcc$modern.defaultBlockState().isSignalSource();
    }

    public void setupRenderBoundingBox() {}

    public Block setTranslationKey(String key) {
        this.translationKey = "tile." + key;
        return this;
    }

    public String getTranslatedName() {
        String key = getTranslationKey();
        String name = key == null ? null : ContentB173.translate(key + ".name");
        // a base-game view has no Beta language entry here: the native name is the one the player sees
        if ((name == null || name.equals(key + ".name")) && mcc$modern != null) return mcc$modern.getName().getString();
        return name == null ? "" : name;
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
    public void onTick(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z, Random random) {
        if(mcc$owner==null&&(id==8||id==10))world.mcc$scheduleVanillaFluidTick(x,y,z,id);
    }

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

    /** Original superclass harvest baseline, including subclasses that delegate their ordinary-tool branch here. */
    public void afterBreak(legacyconnector.vb1_7_3.net.minecraft.world.World world, legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player,
            int x, int y, int z, int meta) {
        player.increaseStat(legacyconnector.vb1_7_3.net.minecraft.stat.Stats.MINE_BLOCK[id],1);
        dropStacks(world,x,y,z,meta);
    }

    /** MCP {@code canPlaceBlockAt}. */
    public boolean canPlaceAt(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z) {
        int id = world.getBlockId(x, y, z);
        return id == 0 || BLOCKS[id] == null || BLOCKS[id].material == null || BLOCKS[id].material.isReplaceable();
    }

    /** MCP {@code canPlaceBlockOnSide}. */
    public boolean canPlaceAt(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z, int side) {
        return canPlaceAt(world, x, y, z);
    }

    /** MCP {@code canBlockStay}; despite the mapped name, this is a survival check, not a growth tick. */
    public boolean canGrow(legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z) {
        return true;
    }

    /** MCP {@code shouldSideBeRendered}; coordinates identify the neighbour on the queried face. */
    public boolean isSideVisible(legacyconnector.vb1_7_3.net.minecraft.world.BlockView view, int x, int y, int z, int side) {
        if (side == 0 && minY > 0 || side == 1 && maxY < 1 || side == 2 && minZ > 0
                || side == 3 && maxZ < 1 || side == 4 && minX > 0 || side == 5 && maxX < 1) return true;
        return !view.method_1783(x, y, z);
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
    /** The modern chunk renderer supplies world lighting after geometry capture. */
    public float getLuminance(legacyconnector.vb1_7_3.net.minecraft.world.BlockView view,int x,int y,int z){return 1.0F;}

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
        // A vanilla block served to original code carries no drop logic of its own: its drops are the native block's (seeds from grass,
        // sticks from a dead bush), as Beta's were for the same block.
        if (mcc$owner == null && mcc$modern != null && world.mcc$level() instanceof net.minecraft.server.level.ServerLevel level) {
            var pos = new net.minecraft.core.BlockPos(x, y, z);
            var state = level.getBlockState(pos);
            if (state.getBlock() == mcc$modern) {
                net.minecraft.world.level.block.Block.dropResources(state, level, pos);
                return;
            }
        }
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
