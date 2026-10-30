package legacyconnector.v1_7_10.net.minecraft.world.chunk;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A chunk with nothing in it that ignores every change (what a provider returns for a chunk it does not have). */
public class EmptyChunk extends Chunk {
    public EmptyChunk(World world, int x, int z) {
        super(world, x, z);
    }

    @Override
    public boolean isAtLocation(int x, int z) {
        return x == xPosition && z == zPosition;
    }

    @Override
    public int getHeightValue(int x, int z) {
        return 0;
    }

    @Override
    public void generateHeightMap() {
    }

    @Override
    public void generateSkylightMap() {
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return Blocks.air;
    }

    @Override
    public int getBlockLightOpacity(int x, int y, int z) {
        return 255;
    }

    @Override
    /** The SRG name of {@link #setBlockIDWithMetadata} (1.7.10's mappings name it {@code setBlockIDWithMetadata}); shim code still calls this one. */
    public boolean func_150807_a(int x, int y, int z, Block block, int meta) {
        return setBlockIDWithMetadata(x, y, z, block, meta);
    }

    public boolean setBlockIDWithMetadata(int x, int y, int z, Block block, int meta) {
        return true;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return 0;
    }

    @Override
    public boolean setBlockMetadata(int x, int y, int z, int meta) {
        return false;
    }

    @Override
    public int getSavedLightValue(legacyconnector.v1_7_10.net.minecraft.world.EnumSkyBlock type, int x, int y, int z) {
        return 0;
    }

    @Override
    public void setLightValue(legacyconnector.v1_7_10.net.minecraft.world.EnumSkyBlock type, int x, int y, int z, int value) {
    }

    @Override
    public int getBlockLightValue(int x, int y, int z, int skylightSubtracted) {
        return 0;
    }

    @Override
    public void addEntity(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
    }

    @Override
    public void removeEntity(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
    }

    @Override
    public boolean canBlockSeeTheSky(int x, int y, int z) {
        return false;
    }

    @Override
    /** The SRG name of {@link #getBlockTileEntityInChunk} (1.7.10's mappings name it {@code getBlockTileEntityInChunk}); shim code still calls this one. */
    public TileEntity func_150806_e(int x, int y, int z) {
        return getBlockTileEntityInChunk(x, y, z);
    }

    public TileEntity getBlockTileEntityInChunk(int x, int y, int z) {
        return null;
    }

    @Override
    public void addTileEntity(TileEntity te) {
    }

    @Override
    public void setBlockTileEntityInChunk(int x, int y, int z, TileEntity te) {
    }

    @Override
    public void removeTileEntity(int x, int y, int z) {
    }

    @Override
    public void onChunkLoad() {
    }

    @Override
    public void onChunkUnload() {
    }

    @Override
    public void setChunkModified() {
    }

    @Override
    public boolean needsSaving(boolean all) {
        return false;
    }

    @Override
    public Random getRandomWithSeed(long seed) {
        return new Random(worldObj == null ? seed : worldObj.getSeed() + (long) xPosition * xPosition * 4987142 + xPosition * 5947611L
                + (long) zPosition * zPosition * 4392871L + zPosition * 389711L ^ seed);
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean getAreLevelsEmpty(int minY, int maxY) {
        return true;
    }
}
