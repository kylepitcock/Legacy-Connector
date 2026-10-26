package legacyconnector.v1_7_10.net.minecraft.world;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/**
 * A read-only view of the loaded chunks around an area (pathfinding and renderers use it): blocks in chunks that are not
 * loaded, or outside the area, read as air.
 */
public class ChunkCache implements IBlockAccess {
    public int chunkX;
    public int chunkZ;
    public Chunk[][] chunkArray;
    public boolean isEmpty;
    public World worldObj;

    public ChunkCache(World world, int x1, int y1, int z1, int x2, int y2, int z2, int expand) {
        this.worldObj = world;
        this.chunkX = x1 - expand >> 4;
        this.chunkZ = z1 - expand >> 4;
        int lastX = x2 + expand >> 4;
        int lastZ = z2 + expand >> 4;
        this.chunkArray = new Chunk[lastX - chunkX + 1][lastZ - chunkZ + 1];
        this.isEmpty = true;
        for (int cx = chunkX; cx <= lastX; cx++) {
            for (int cz = chunkZ; cz <= lastZ; cz++) {
                Chunk chunk = world == null ? null : world.getChunkFromChunkCoords(cx, cz);
                if (chunk == null) continue;
                chunkArray[cx - chunkX][cz - chunkZ] = chunk;
                if (!chunk.getAreLevelsEmpty(y1, y2)) isEmpty = false;
            }
        }
    }

    private Chunk mcc$chunk(int x, int y, int z) {
        if (y < 0 || y >= 256) return null;
        int i = (x >> 4) - chunkX;
        int j = (z >> 4) - chunkZ;
        if (i < 0 || i >= chunkArray.length || j < 0 || j >= chunkArray[i].length) return null;
        return chunkArray[i][j];
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        Chunk chunk = mcc$chunk(x, y, z);
        return chunk == null ? Blocks.air : chunk.getBlock(x & 15, y, z & 15);
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        Chunk chunk = mcc$chunk(x, y, z);
        return chunk == null ? null : chunk.func_150806_e(x & 15, y, z & 15);
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int min) {
        return worldObj == null ? min << 4 : worldObj.getLightBrightnessForSkyBlocks(x, y, z, min);
    }

    public int getSkyBlockTypeBrightness(EnumSkyBlock type, int x, int y, int z) {
        if (worldObj == null || mcc$chunk(x, Math.max(0, Math.min(255, y)), z) == null) return type == EnumSkyBlock.Sky ? 15 : 0;
        return worldObj.getSkyBlockTypeBrightness(type, x, Math.max(0, Math.min(255, y)), z);
    }

    public int getSpecialBlockBrightness(EnumSkyBlock type, int x, int y, int z) {
        return getSkyBlockTypeBrightness(type, x, y, z);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        Chunk chunk = mcc$chunk(x, y, z);
        return chunk == null ? 0 : chunk.getBlockMetadata(x & 15, y, z & 15);
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        return mcc$chunk(x, y, z) == null ? 0 : worldObj.isBlockProvidingPowerTo(x, y, z, side);
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return getBlock(x, y, z).isAir(this, x, y, z);
    }

    @Override
    public int getHeight() {
        return 256;
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return worldObj == null ? BiomeGenBase.plains : worldObj.getBiomeGenForCoords(x, z);
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return isEmpty;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean defaultValue) {
        if (x < -30000000 || z < -30000000 || x >= 30000000 || z >= 30000000) return defaultValue;
        Chunk chunk = mcc$chunk(x, y, z);
        return chunk == null ? defaultValue : worldObj.isSideSolid(x, y, z, side, defaultValue);
    }

    @Override
    public int mcc$blockTint(int x, int y, int z, int kind) {
        return worldObj == null ? 0xFFFFFF : worldObj.mcc$blockTint(x, y, z, kind);
    }
}
