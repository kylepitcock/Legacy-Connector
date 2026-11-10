package legacyconnector.vb1_7_3.net.minecraft.world.chunk;

import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta 1.7.3 {@code Chunk} as a view: a 16x16 column of the world at chunk {@link #x}, {@link #z}; reads and writes go to the world with
 * chunk-relative coordinates. Mods mostly ask it to be saved ({@link #markDirty}, RedPower after changing a tile entity).
 */
public class Chunk {
    public World world;
    public final int x;
    public final int z;
    public boolean dirty;
    public boolean loaded = true;
    /** Generated storage is retained, since providers fill it after constructing the chunk. */
    public byte[] blocks;
    private byte[] metadata;
    private final java.util.Map<Integer,BlockEntity> detachedBlockEntities=new java.util.HashMap<>();
    public byte[] heightmap = new byte[256];
    public boolean terrainPopulated;

    public Chunk(World world, byte[] blocks, int x, int z) {
        this(world,x,z);
        if(blocks == null || blocks.length != 32768)throw new IllegalArgumentException("Beta chunks require 32768 block bytes");
        this.blocks=blocks;
        this.metadata=new byte[16384];
    }
    public boolean mcc$hasArrays(){return blocks!=null;}
    public byte[] mcc$blocks(){return blocks;}
    public int mcc$arrayHeight(){return 128;}
    public int mcc$metadata(int index){return metadata==null?0:(metadata[index>>1]>>((index&1)*4))&15;}
    private static boolean valid(int x,int y,int z){return x>=0&&x<16&&z>=0&&z<16&&y>=0&&y<128;}
    private static int index(int x,int y,int z){return x<<11|z<<7|y;}
    public void populateHeightMapOnly(){
        if(blocks==null)return;
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            int y=128;
            while(y>0){int id=blocks[index(x,y-1,z)]&255;
                if(id!=0&&(id>=legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS_LIGHT_OPACITY.length||legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS_LIGHT_OPACITY[id]>0))break;
                y--;
            }
            heightmap[z<<4|x]=(byte)y;
        }
    }
    public void populateHeightMap(){populateHeightMapOnly();}


    public Chunk(World world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public boolean chunkPosEquals(int x, int z) {
        return this.x == x && this.z == z;
    }

    public int getHeight(int x, int z) {
        if(blocks!=null)return heightmap[z<<4|x]&255;
        return world.getTopY((this.x << 4) + x, (this.z << 4) + z);
    }

    public int getBlockId(int x, int y, int z) {
        if(blocks!=null)return valid(x,y,z)?blocks[index(x,y,z)]&255:0;
        return world.getBlockId((this.x << 4) + x, y, (this.z << 4) + z);
    }

    public int getBlockMeta(int x, int y, int z) {
        if(blocks!=null)return valid(x,y,z)?mcc$metadata(index(x,y,z)):0;
        return world.getBlockMeta((this.x << 4) + x, y, (this.z << 4) + z);
    }

    public boolean setBlock(int x, int y, int z, int id) {
        if(blocks!=null)return setBlock(x,y,z,id,0);
        return world.setBlockWithoutNotifyingNeighbors((this.x << 4) + x, y, (this.z << 4) + z, id);
    }

    public boolean setBlock(int x, int y, int z, int id, int meta) {
        if(blocks!=null){
            if(!valid(x,y,z)||id<0||id>255)return false;
            int i=index(x,y,z);if((blocks[i]&255)==id&&mcc$metadata(i)==(meta&15))return false;
            blocks[i]=(byte)id;setBlockMeta(x,y,z,meta);populateHeightMapOnly();dirty=true;return true;
        }
        return world.setBlockWithoutNotifyingNeighbors((this.x << 4) + x, y, (this.z << 4) + z, id, meta);
    }

    public void setBlockMeta(int x, int y, int z, int meta) {
        if(blocks!=null){if(valid(x,y,z)){int i=index(x,y,z),shift=(i&1)*4;metadata[i>>1]=(byte)((metadata[i>>1]&~(15<<shift))|((meta&15)<<shift));dirty=true;}return;}
        world.setBlockMetaWithoutNotifyingNeighbors((this.x << 4) + x, y, (this.z << 4) + z, meta);
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        if(blocks!=null)return valid(x,y,z)?detachedBlockEntities.get(index(x,y,z)):null;
        return world.getBlockEntity((this.x << 4) + x, y, (this.z << 4) + z);
    }

    public void setBlockEntity(int x, int y, int z, BlockEntity te) {
        if(blocks!=null){if(valid(x,y,z))detachedBlockEntities.put(index(x,y,z),te);return;}
        world.setBlockEntity((this.x << 4) + x, y, (this.z << 4) + z, te);
    }

    public void removeBlockEntityAt(int x, int y, int z) {
        if(blocks!=null){if(valid(x,y,z))detachedBlockEntities.remove(index(x,y,z));return;}
        world.removeBlockEntity((this.x << 4) + x, y, (this.z << 4) + z);
    }

    /** MCP {@code setChunkModified}: the modern chunk is saved. */
    public void markDirty() {
        dirty = true;
        net.minecraft.world.level.Level level = blocks!=null||world == null ? null : world.mcc$level();
        if (level != null && level.hasChunk(x, z)) level.getChunk(x, z).setUnsaved(true);
    }

    public boolean isEmpty() {
        if(blocks!=null){for(byte id:blocks)if(id!=0)return false;return true;}
        return false;
    }

    public void load() {}

    public void unload() {}
}
