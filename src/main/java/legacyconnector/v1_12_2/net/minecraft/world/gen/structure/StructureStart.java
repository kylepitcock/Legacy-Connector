package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** The pieces of one structure, generated chunk by chunk. */
public class StructureStart {
    protected List<StructureComponent> components = new LinkedList<>();
    protected StructureBoundingBox boundingBox;
    private int chunkPosX;
    private int chunkPosZ;

    public StructureStart() {
    }

    public StructureStart(int chunkX, int chunkZ) {
        this.chunkPosX = chunkX;
        this.chunkPosZ = chunkZ;
    }

    public StructureBoundingBox getBoundingBox() {
        return boundingBox;
    }

    public List<StructureComponent> getComponents() {
        return components;
    }

    /** Builds the pieces inside {@code box} (one chunk at a time); pieces that refuse to generate are dropped. */
    public void generateStructure(World worldIn, Random rand, StructureBoundingBox structurebb) {
        Iterator<StructureComponent> it = components.iterator();
        while (it.hasNext()) {
            StructureComponent c = it.next();
            if (c.getBoundingBox().intersectsWith(structurebb) && !c.addComponentParts(worldIn, rand, structurebb)) it.remove();
        }
    }

    protected void updateBoundingBox() {
        boundingBox = StructureBoundingBox.getNewBoundingBox();
        for (StructureComponent c : components) boundingBox.expandTo(c.getBoundingBox());
    }

    public NBTTagCompound writeStructureComponentsToNBT(int chunkX, int chunkZ) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", MapGenStructureIO.getStructureStartName(this));
        tag.setInteger("ChunkX", chunkX);
        tag.setInteger("ChunkZ", chunkZ);
        tag.setTag("BB", boundingBox.toNBTTagIntArray());
        NBTTagList children = new NBTTagList();
        for (StructureComponent c : components) children.appendTag(c.createStructureBaseNBT());
        tag.setTag("Children", children);
        writeToNBT(tag);
        return tag;
    }

    public void writeToNBT(NBTTagCompound tagCompound) {
    }

    public void readStructureComponentsFromNBT(World worldIn, NBTTagCompound tagCompound) {
        chunkPosX = tagCompound.getInteger("ChunkX");
        chunkPosZ = tagCompound.getInteger("ChunkZ");
        if (tagCompound.hasKey("BB")) boundingBox = new StructureBoundingBox(tagCompound.getIntArray("BB"));
        NBTTagList children = tagCompound.getTagList("Children", 10);
        for (int i = 0; i < children.tagCount(); ++i) {
            StructureComponent c = MapGenStructureIO.getStructureComponent(children.getCompoundTagAt(i), worldIn);
            if (c != null) components.add(c);
        }
        readFromNBT(tagCompound);
    }

    public void readFromNBT(NBTTagCompound tagCompound) {
    }

    protected void markAvailableHeight(World worldIn, Random rand, int p_75067_3_) {
        int i = worldIn.getSeaLevel() - p_75067_3_;
        int j = boundingBox.getYSize() + 1;
        if (j < i) j += rand.nextInt(i - j);
        int k = j - boundingBox.maxY;
        boundingBox.offset(0, k, 0);
        for (StructureComponent c : components) c.offset(0, k, 0);
    }

    protected void setRandomHeight(World worldIn, Random rand, int p_75070_3_, int p_75070_4_) {
        int i = p_75070_4_ - p_75070_3_ + 1 - boundingBox.getYSize();
        int j = i > 1 ? p_75070_3_ + rand.nextInt(i) : p_75070_3_;
        int k = j - boundingBox.minY;
        boundingBox.offset(0, k, 0);
        for (StructureComponent c : components) c.offset(0, k, 0);
    }

    public boolean isSizeableStructure() {
        return true;
    }

    public boolean isValidForPostProcess(ChunkPos pair) {
        return true;
    }

    public void notifyPostProcessAt(ChunkPos pair) {
    }

    public int getChunkPosX() {
        return chunkPosX;
    }

    public int getChunkPosZ() {
        return chunkPosZ;
    }
}
