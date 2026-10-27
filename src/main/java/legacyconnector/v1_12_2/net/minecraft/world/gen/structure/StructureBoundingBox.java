package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagIntArray;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3i;

/** An axis aligned box of block positions (both ends inclusive). */
public class StructureBoundingBox {
    public int minX;
    public int minY;
    public int minZ;
    public int maxX;
    public int maxY;
    public int maxZ;

    public StructureBoundingBox() {
    }

    public StructureBoundingBox(int[] coords) {
        if (coords.length == 6) {
            this.minX = coords[0];
            this.minY = coords[1];
            this.minZ = coords[2];
            this.maxX = coords[3];
            this.maxY = coords[4];
            this.maxZ = coords[5];
        }
    }

    public StructureBoundingBox(StructureBoundingBox o) {
        this(o.minX, o.minY, o.minZ, o.maxX, o.maxY, o.maxZ);
    }

    public StructureBoundingBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.minX = x1;
        this.minY = y1;
        this.minZ = z1;
        this.maxX = x2;
        this.maxY = y2;
        this.maxZ = z2;
    }

    public StructureBoundingBox(Vec3i a, Vec3i b) {
        this(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    public StructureBoundingBox(int x1, int z1, int x2, int z2) {
        this(x1, 1, z1, x2, 512, z2);
    }

    public static StructureBoundingBox getNewBoundingBox() {
        return new StructureBoundingBox(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    /** The box of a piece of the given size placed at an offset, turned to face the given direction. */
    public static StructureBoundingBox getComponentToAddBoundingBox(int x, int y, int z, int offX, int offY, int offZ, int width, int height,
            int depth, EnumFacing facing) {
        if (facing == null) return new StructureBoundingBox(x + offX, y + offY, z + offZ, x + width - 1 + offX, y + height - 1 + offY, z + depth - 1 + offZ);
        switch (facing.getName()) {
            case "north":
                return new StructureBoundingBox(x + offX, y + offY, z - depth + 1 + offZ, x + width - 1 + offX, y + height - 1 + offY, z + offZ);
            case "west":
                return new StructureBoundingBox(x - depth + 1 + offZ, y + offY, z + offX, x + offZ, y + height - 1 + offY, z + width - 1 + offX);
            case "east":
                return new StructureBoundingBox(x + offZ, y + offY, z + offX, x + depth - 1 + offZ, y + height - 1 + offY, z + width - 1 + offX);
            default:
                return new StructureBoundingBox(x + offX, y + offY, z + offZ, x + width - 1 + offX, y + height - 1 + offY, z + depth - 1 + offZ);
        }
    }

    public static StructureBoundingBox createProper(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new StructureBoundingBox(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    public boolean intersectsWith(StructureBoundingBox o) {
        return maxX >= o.minX && minX <= o.maxX && maxZ >= o.minZ && minZ <= o.maxZ && maxY >= o.minY && minY <= o.maxY;
    }

    public boolean intersectsWith(int x1, int z1, int x2, int z2) {
        return maxX >= x1 && minX <= x2 && maxZ >= z1 && minZ <= z2;
    }

    public void expandTo(StructureBoundingBox o) {
        minX = Math.min(minX, o.minX);
        minY = Math.min(minY, o.minY);
        minZ = Math.min(minZ, o.minZ);
        maxX = Math.max(maxX, o.maxX);
        maxY = Math.max(maxY, o.maxY);
        maxZ = Math.max(maxZ, o.maxZ);
    }

    public void offset(int x, int y, int z) {
        minX += x;
        minY += y;
        minZ += z;
        maxX += x;
        maxY += y;
        maxZ += z;
    }

    public boolean isVecInside(Vec3i vec) {
        return vec.getX() >= minX && vec.getX() <= maxX && vec.getZ() >= minZ && vec.getZ() <= maxZ && vec.getY() >= minY && vec.getY() <= maxY;
    }

    public boolean mcc$isInside(int x, int y, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ && y >= minY && y <= maxY;
    }

    public Vec3i getLength() {
        return new Vec3i(maxX - minX, maxY - minY, maxZ - minZ);
    }

    public int getXSize() {
        return maxX - minX + 1;
    }

    public int getYSize() {
        return maxY - minY + 1;
    }

    public int getZSize() {
        return maxZ - minZ + 1;
    }

    @Override
    public String toString() {
        return "(" + minX + ", " + minY + ", " + minZ + "; " + maxX + ", " + maxY + ", " + maxZ + ")";
    }

    public NBTTagIntArray toNBTTagIntArray() {
        return new NBTTagIntArray(new int[] {minX, minY, minZ, maxX, maxY, maxZ});
    }
}
