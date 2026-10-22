package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A structure being generated: its pieces, laid out when the structure is first needed and placed chunk by chunk. */
public abstract class StructureStart {
    protected LinkedList components = new LinkedList();
    protected StructureBoundingBox boundingBox;
    private int field_143024_c;
    private int field_143023_d;

    public StructureStart() {
    }

    public StructureStart(int chunkX, int chunkZ) {
        this.field_143024_c = chunkX;
        this.field_143023_d = chunkZ;
    }

    public StructureBoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    public LinkedList getComponents() {
        return this.components;
    }

    public void generateStructure(World world, Random rand, StructureBoundingBox box) {
        Iterator it = this.components.iterator();
        while (it.hasNext()) {
            StructureComponent c = (StructureComponent) it.next();
            if (c.getBoundingBox().intersectsWith(box) && !c.addComponentParts(world, rand, box)) it.remove();
        }
    }

    protected void updateBoundingBox() {
        this.boundingBox = StructureBoundingBox.getNewBoundingBox();
        for (Object o : this.components) this.boundingBox.expandTo(((StructureComponent) o).getBoundingBox());
    }

    public NBTTagCompound func_143021_a(int chunkX, int chunkZ) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", MapGenStructureIO.func_143033_a(this));
        tag.setInteger("ChunkX", chunkX);
        tag.setInteger("ChunkZ", chunkZ);
        tag.setTag("BB", this.boundingBox.func_151535_h());
        NBTTagList list = new NBTTagList();
        for (Object o : this.components) list.appendTag(((StructureComponent) o).func_143010_b());
        tag.setTag("Children", list);
        this.func_143022_a(tag);
        return tag;
    }

    public void func_143022_a(NBTTagCompound tag) {
    }

    public void func_143020_a(World world, NBTTagCompound tag) {
        this.field_143024_c = tag.getInteger("ChunkX");
        this.field_143023_d = tag.getInteger("ChunkZ");
        if (tag.hasKey("BB")) this.boundingBox = new StructureBoundingBox(tag.getIntArray("BB"));
        NBTTagList list = tag.getTagList("Children", 10);
        for (int i = 0; i < list.tagCount(); ++i) {
            StructureComponent c = MapGenStructureIO.func_143032_b(list.getCompoundTagAt(i), world);
            if (c != null) this.components.add(c);
        }
        this.func_143017_b(tag);
    }

    public void func_143017_b(NBTTagCompound tag) {
    }

    protected void markAvailableHeight(World world, Random rand, int offset) {
        int k = 63 - offset;
        int l = this.boundingBox.getYSize() + 1;
        if (l < k) l += rand.nextInt(k - l);
        int i1 = l - this.boundingBox.maxY;
        this.boundingBox.offset(0, i1, 0);
        for (Object o : this.components) ((StructureComponent) o).getBoundingBox().offset(0, i1, 0);
    }

    protected void setRandomHeight(World world, Random rand, int min, int max) {
        int k = max - min + 1 - this.boundingBox.getYSize();
        int l;
        if (k > 1) l = min + rand.nextInt(k);
        else l = min;
        int i1 = l - this.boundingBox.minY;
        this.boundingBox.offset(0, i1, 0);
        for (Object o : this.components) ((StructureComponent) o).getBoundingBox().offset(0, i1, 0);
    }

    public boolean isSizeableStructure() {
        return true;
    }

    public int func_143019_e() {
        return this.field_143024_c;
    }

    public int func_143018_f() {
        return this.field_143023_d;
    }
}
