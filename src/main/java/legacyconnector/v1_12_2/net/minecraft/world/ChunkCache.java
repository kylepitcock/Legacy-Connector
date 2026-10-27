package legacyconnector.v1_12_2.net.minecraft.world;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;

/**
 * 1.12.2 read-only snapshot area of a world (path finding, client chunk rendering): blocks, tile entities, light and biomes of the
 * chunks in a box, answered from the world (positions outside the loaded chunks read as air).
 */
public class ChunkCache implements IBlockAccess {
    protected int chunkX;
    protected int chunkZ;
    protected boolean empty;
    protected World world;

    public ChunkCache(World worldIn, BlockPos posFromIn, BlockPos posToIn, int subIn) {
        this.world = worldIn;
        this.chunkX = posFromIn.getX() - subIn >> 4;
        this.chunkZ = posFromIn.getZ() - subIn >> 4;
        int maxX = posToIn.getX() + subIn >> 4;
        int maxZ = posToIn.getZ() + subIn >> 4;
        this.empty = true;
        for (int x = chunkX; x <= maxX && empty; x++) {
            for (int z = chunkZ; z <= maxZ && empty; z++) {
                if (worldIn != null && worldIn.isBlockLoaded(new BlockPos(x << 4, 64, z << 4))
                        && !worldIn.getChunk(x, z).isEmptyBetween(posFromIn.getY(), posToIn.getY())) {
                    this.empty = false;
                }
            }
        }
    }

    public boolean isEmpty() {
        return empty;
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return getTileEntity(pos, Chunk.EnumCreateEntityType.IMMEDIATE);
    }

    public TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType type) {
        if (world == null || !world.isBlockLoaded(pos)) return null;
        return world.getTileEntity(pos);
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        int sky = getLightFor(EnumSkyBlock.SKY, pos);
        int block = Math.max(getLightFor(EnumSkyBlock.BLOCK, pos), lightValue);
        return sky << 20 | block << 4;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        if (world == null || pos.getY() < 0 || pos.getY() >= 256 || !world.isBlockLoaded(pos)) {
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
        }
        return world.getBlockState(pos);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return world == null ? legacyconnector.v1_12_2.net.minecraft.init.Biomes.PLAINS : world.getBiome(pos);
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        return getBlockState(pos).getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.AIR;
    }

    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        if (world == null || !world.isBlockLoaded(pos)) return type.defaultLightValue;
        return world.getLightFor(type, pos);
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return world == null ? 0 : world.getStrongPower(pos, direction);
    }

    @Override
    public WorldType getWorldType() {
        return world == null ? WorldType.DEFAULT : world.getWorldType();
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        if (world == null || !world.isBlockLoaded(pos)) return _default;
        return world.isSideSolid(pos, side, _default);
    }
}
