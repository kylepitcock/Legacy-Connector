package mccompat.v1_7_10.net.minecraft.world.gen.structure;

import mccompat.v1_7_10.net.minecraft.nbt.NBTTagIntArray;

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

    public StructureBoundingBox(int[] a) {
        if (a.length == 6) {
            this.minX = a[0];
            this.minY = a[1];
            this.minZ = a[2];
            this.maxX = a[3];
            this.maxY = a[4];
            this.maxZ = a[5];
        }
    }

    public static StructureBoundingBox getNewBoundingBox() {
        return new StructureBoundingBox(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    /** The box of a component of the given size placed at an offset, rotated by the direction (0 south, 1 west, 2 north, 3 east). */
    public static StructureBoundingBox getComponentToAddBoundingBox(int x, int y, int z, int offX, int offY, int offZ, int width, int height,
            int depth, int direction) {
        switch (direction) {
            case 2:
                return new StructureBoundingBox(x + offX, y + offY, z - depth + 1 + offZ, x + width - 1 + offX, y + height - 1 + offY, z + offZ);
            case 0:
                return new StructureBoundingBox(x + offX, y + offY, z + offZ, x + width - 1 + offX, y + height - 1 + offY, z + depth - 1 + offZ);
            case 1:
                return new StructureBoundingBox(x - depth + 1 + offZ, y + offY, z + offX, x + offZ, y + height - 1 + offY, z + width - 1 + offX);
            case 3:
                return new StructureBoundingBox(x + offZ, y + offY, z + offX, x + depth - 1 + offZ, y + height - 1 + offY, z + width - 1 + offX);
            default:
                return new StructureBoundingBox(x + offX, y + offY, z + offZ, x + width - 1 + offX, y + height - 1 + offY, z + depth - 1 + offZ);
        }
    }

    public StructureBoundingBox(StructureBoundingBox o) {
        this.minX = o.minX;
        this.minY = o.minY;
        this.minZ = o.minZ;
        this.maxX = o.maxX;
        this.maxY = o.maxY;
        this.maxZ = o.maxZ;
    }

    public StructureBoundingBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.minX = x1;
        this.minY = y1;
        this.minZ = z1;
        this.maxX = x2;
        this.maxY = y2;
        this.maxZ = z2;
    }

    public StructureBoundingBox(int x1, int z1, int x2, int z2) {
        this.minX = x1;
        this.minZ = z1;
        this.maxX = x2;
        this.maxZ = z2;
        this.minY = 1;
        this.maxY = 512;
    }

    public boolean intersectsWith(StructureBoundingBox o) {
        return this.maxX >= o.minX && this.minX <= o.maxX && this.maxZ >= o.minZ && this.minZ <= o.maxZ && this.maxY >= o.minY
                && this.minY <= o.maxY;
    }

    public boolean intersectsWith(int minX, int minZ, int maxX, int maxZ) {
        return this.maxX >= minX && this.minX <= maxX && this.maxZ >= minZ && this.minZ <= maxZ;
    }

    public void expandTo(StructureBoundingBox o) {
        this.minX = Math.min(this.minX, o.minX);
        this.minY = Math.min(this.minY, o.minY);
        this.minZ = Math.min(this.minZ, o.minZ);
        this.maxX = Math.max(this.maxX, o.maxX);
        this.maxY = Math.max(this.maxY, o.maxY);
        this.maxZ = Math.max(this.maxZ, o.maxZ);
    }

    public void offset(int x, int y, int z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
    }

    public boolean isVecInside(int x, int y, int z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ && y >= this.minY && y <= this.maxY;
    }

    public int getXSize() {
        return this.maxX - this.minX + 1;
    }

    public int getYSize() {
        return this.maxY - this.minY + 1;
    }

    public int getZSize() {
        return this.maxZ - this.minZ + 1;
    }

    public int getCenterX() {
        return this.minX + (this.maxX - this.minX + 1) / 2;
    }

    public int getCenterY() {
        return this.minY + (this.maxY - this.minY + 1) / 2;
    }

    public int getCenterZ() {
        return this.minZ + (this.maxZ - this.minZ + 1) / 2;
    }

    @Override
    public String toString() {
        return "(" + this.minX + ", " + this.minY + ", " + this.minZ + "; " + this.maxX + ", " + this.maxY + ", " + this.maxZ + ")";
    }

    public NBTTagIntArray func_151535_h() {
        return new NBTTagIntArray(new int[] {this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ});
    }
}
