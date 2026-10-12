package mccompat.v1_7_10.net.minecraft.world.gen.structure;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.block.material.Material;
import mccompat.v1_7_10.net.minecraft.init.Blocks;
import mccompat.v1_7_10.net.minecraft.inventory.IInventory;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.tileentity.TileEntity;
import mccompat.v1_7_10.net.minecraft.util.WeightedRandomChestContent;
import mccompat.v1_7_10.net.minecraft.world.ChunkPosition;
import mccompat.v1_7_10.net.minecraft.world.World;

/**
 * One piece of a generated structure. Placement methods take coordinates relative to the piece and rotate them by
 * {@code coordBaseMode} (0 south, 1 west, 2 north, 3 east); blocks outside the chunk being generated are skipped.
 */
public abstract class StructureComponent {
    protected StructureBoundingBox boundingBox;
    protected int coordBaseMode;
    protected int componentType;

    public StructureComponent() {
    }

    protected StructureComponent(int componentType) {
        this.componentType = componentType;
    }

    public NBTTagCompound func_143010_b() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", MapGenStructureIO.func_143036_a(this));
        tag.setTag("BB", this.boundingBox.func_151535_h());
        tag.setInteger("O", this.coordBaseMode);
        tag.setInteger("GD", this.componentType);
        this.writeStructureToNBT(tag);
        return tag;
    }

    protected abstract void writeStructureToNBT(NBTTagCompound tag);

    public void func_143009_a(World world, NBTTagCompound tag) {
        if (tag.hasKey("BB")) this.boundingBox = new StructureBoundingBox(tag.getIntArray("BB"));
        this.coordBaseMode = tag.getInteger("O");
        this.componentType = tag.getInteger("GD");
        this.readStructureFromNBT(tag);
    }

    protected abstract void readStructureFromNBT(NBTTagCompound tag);

    public void buildComponent(StructureComponent parent, List list, Random rand) {
    }

    public abstract boolean addComponentParts(World world, Random rand, StructureBoundingBox box);

    public StructureBoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    public int getComponentType() {
        return this.componentType;
    }

    public static StructureComponent findIntersecting(List list, StructureBoundingBox box) {
        for (Object o : list) {
            StructureComponent c = (StructureComponent) o;
            if (c.getBoundingBox() != null && c.getBoundingBox().intersectsWith(box)) return c;
        }
        return null;
    }

    public ChunkPosition func_151553_a() {
        return new ChunkPosition(this.boundingBox.getCenterX(), this.boundingBox.getCenterY(), this.boundingBox.getCenterZ());
    }

    protected boolean isLiquidInStructureBoundingBox(World world, StructureBoundingBox box) {
        int i = Math.max(this.boundingBox.minX - 1, box.minX);
        int j = Math.max(this.boundingBox.minY - 1, box.minY);
        int k = Math.max(this.boundingBox.minZ - 1, box.minZ);
        int l = Math.min(this.boundingBox.maxX + 1, box.maxX);
        int i1 = Math.min(this.boundingBox.maxY + 1, box.maxY);
        int j1 = Math.min(this.boundingBox.maxZ + 1, box.maxZ);
        for (int x = i; x <= l; ++x) {
            for (int z = k; z <= j1; ++z) {
                if (world.getBlock(x, j, z).getMaterial().isLiquid()) return true;
                if (world.getBlock(x, i1, z).getMaterial().isLiquid()) return true;
            }
        }
        for (int x = i; x <= l; ++x) {
            for (int y = j; y <= i1; ++y) {
                if (world.getBlock(x, y, k).getMaterial().isLiquid()) return true;
                if (world.getBlock(x, y, j1).getMaterial().isLiquid()) return true;
            }
        }
        for (int z = k; z <= j1; ++z) {
            for (int y = j; y <= i1; ++y) {
                if (world.getBlock(i, y, z).getMaterial().isLiquid()) return true;
                if (world.getBlock(l, y, z).getMaterial().isLiquid()) return true;
            }
        }
        return false;
    }

    protected int getXWithOffset(int x, int z) {
        switch (this.coordBaseMode) {
            case 0:
            case 2:
                return this.boundingBox.minX + x;
            case 1:
                return this.boundingBox.maxX - z;
            case 3:
                return this.boundingBox.minX + z;
            default:
                return x;
        }
    }

    protected int getYWithOffset(int y) {
        return this.coordBaseMode == -1 ? y : y + this.boundingBox.minY;
    }

    protected int getZWithOffset(int x, int z) {
        switch (this.coordBaseMode) {
            case 0:
                return this.boundingBox.minZ + z;
            case 1:
            case 3:
                return this.boundingBox.minZ + x;
            case 2:
                return this.boundingBox.maxZ - z;
            default:
                return z;
        }
    }

    /** Rotates the metadata of directional blocks to the piece's orientation. */
    protected int getMetadataWithOffset(Block block, int meta) {
        if (block == Blocks.rail) {
            if (this.coordBaseMode == 1 || this.coordBaseMode == 3) return meta == 1 ? 0 : 1;
        } else if (block != Blocks.wooden_door && block != Blocks.iron_door) {
            if (block != Blocks.stone_stairs && block != Blocks.oak_stairs && block != Blocks.nether_brick_stairs
                    && block != Blocks.stone_brick_stairs && block != Blocks.sandstone_stairs) {
                if (block == Blocks.ladder) {
                    if (this.coordBaseMode == 0) {
                        if (meta == 2) return 3;
                        if (meta == 3) return 2;
                    } else if (this.coordBaseMode == 1) {
                        if (meta == 2) return 4;
                        if (meta == 3) return 5;
                        if (meta == 4) return 2;
                        if (meta == 5) return 3;
                    } else if (this.coordBaseMode == 3) {
                        if (meta == 2) return 5;
                        if (meta == 3) return 4;
                        if (meta == 4) return 3;
                        if (meta == 5) return 2;
                    }
                } else if (block == Blocks.stone_button) {
                    if (this.coordBaseMode == 0) {
                        if (meta == 3) return 4;
                        if (meta == 4) return 3;
                    } else if (this.coordBaseMode == 1) {
                        if (meta == 3) return 1;
                        if (meta == 4) return 2;
                        if (meta == 2) return 3;
                        if (meta == 1) return 4;
                    } else if (this.coordBaseMode == 3) {
                        if (meta == 3) return 2;
                        if (meta == 4) return 1;
                        if (meta == 2) return 3;
                        if (meta == 1) return 4;
                    }
                } else if (block != Blocks.tripwire_hook && !(block instanceof mccompat.v1_7_10.net.minecraft.block.BlockDirectional)) {
                    if (block == Blocks.piston || block == Blocks.sticky_piston || block == Blocks.lever || block == Blocks.dispenser) {
                        if (this.coordBaseMode == 0) {
                            if (meta == 2 || meta == 3) return Facing.oppositeSide(meta);
                        } else if (this.coordBaseMode == 1) {
                            if (meta == 2) return 4;
                            if (meta == 3) return 5;
                            if (meta == 4) return 2;
                            if (meta == 5) return 3;
                        } else if (this.coordBaseMode == 3) {
                            if (meta == 2) return 5;
                            if (meta == 3) return 4;
                            if (meta == 4) return 3;
                            if (meta == 5) return 2;
                        }
                    }
                } else if (this.coordBaseMode == 0) {
                    if (meta == 0 || meta == 2) return meta + 2 & 3;
                } else if (this.coordBaseMode == 1) {
                    return meta + 1 & 3;
                } else if (this.coordBaseMode == 3) {
                    return meta + 3 & 3;
                }
            } else if (this.coordBaseMode == 0) {
                if (meta == 2) return 3;
                if (meta == 3) return 2;
            } else if (this.coordBaseMode == 1) {
                if (meta == 0) return 2;
                if (meta == 1) return 3;
                if (meta == 2) return 0;
                if (meta == 3) return 1;
            } else if (this.coordBaseMode == 3) {
                if (meta == 0) return 2;
                if (meta == 1) return 3;
                if (meta == 2) return 1;
                if (meta == 3) return 0;
            }
        } else if (this.coordBaseMode == 0) {
            if (meta == 0) return 2;
            if (meta == 2) return 0;
        } else {
            if (this.coordBaseMode == 1) return meta + 1 & 3;
            if (this.coordBaseMode == 3) return meta + 3 & 3;
        }
        return meta;
    }

    private static final class Facing {
        static int oppositeSide(int side) {
            return side ^ 1;
        }
    }

    protected void placeBlockAtCurrentPosition(World world, Block block, int meta, int x, int y, int z, StructureBoundingBox box) {
        int i = this.getXWithOffset(x, z);
        int j = this.getYWithOffset(y);
        int k = this.getZWithOffset(x, z);
        if (box.isVecInside(i, j, k)) world.setBlock(i, j, k, block, meta, 2);
    }

    protected Block getBlockAtCurrentPosition(World world, int x, int y, int z, StructureBoundingBox box) {
        int i = this.getXWithOffset(x, z);
        int j = this.getYWithOffset(y);
        int k = this.getZWithOffset(x, z);
        return !box.isVecInside(i, j, k) ? Blocks.air : world.getBlock(i, j, k);
    }

    protected void fillWithAir(World world, StructureBoundingBox box, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int y = y1; y <= y2; ++y) {
            for (int x = x1; x <= x2; ++x) {
                for (int z = z1; z <= z2; ++z) this.placeBlockAtCurrentPosition(world, Blocks.air, 0, x, y, z, box);
            }
        }
    }

    protected void fillWithBlocks(World world, StructureBoundingBox box, int x1, int y1, int z1, int x2, int y2, int z2, Block outer,
            Block inner, boolean onlyReplaceSolid) {
        for (int y = y1; y <= y2; ++y) {
            for (int x = x1; x <= x2; ++x) {
                for (int z = z1; z <= z2; ++z) {
                    if (!onlyReplaceSolid || this.getBlockAtCurrentPosition(world, x, y, z, box).getMaterial() != Material.air) {
                        if (y != y1 && y != y2 && x != x1 && x != x2 && z != z1 && z != z2) {
                            this.placeBlockAtCurrentPosition(world, inner, 0, x, y, z, box);
                        } else {
                            this.placeBlockAtCurrentPosition(world, outer, 0, x, y, z, box);
                        }
                    }
                }
            }
        }
    }

    protected void fillWithMetadataBlocks(World world, StructureBoundingBox box, int x1, int y1, int z1, int x2, int y2, int z2, Block outer,
            int outerMeta, Block inner, int innerMeta, boolean onlyReplaceSolid) {
        for (int y = y1; y <= y2; ++y) {
            for (int x = x1; x <= x2; ++x) {
                for (int z = z1; z <= z2; ++z) {
                    if (!onlyReplaceSolid || this.getBlockAtCurrentPosition(world, x, y, z, box).getMaterial() != Material.air) {
                        if (y != y1 && y != y2 && x != x1 && x != x2 && z != z1 && z != z2) {
                            this.placeBlockAtCurrentPosition(world, inner, innerMeta, x, y, z, box);
                        } else {
                            this.placeBlockAtCurrentPosition(world, outer, outerMeta, x, y, z, box);
                        }
                    }
                }
            }
        }
    }

    protected void fillWithRandomizedBlocks(World world, StructureBoundingBox box, int x1, int y1, int z1, int x2, int y2, int z2,
            boolean onlyReplaceSolid, Random rand, BlockSelector selector) {
        for (int y = y1; y <= y2; ++y) {
            for (int x = x1; x <= x2; ++x) {
                for (int z = z1; z <= z2; ++z) {
                    if (!onlyReplaceSolid || this.getBlockAtCurrentPosition(world, x, y, z, box).getMaterial() != Material.air) {
                        selector.selectBlocks(rand, x, y, z, y == y1 || y == y2 || x == x1 || x == x2 || z == z1 || z == z2);
                        this.placeBlockAtCurrentPosition(world, selector.func_151561_a(), selector.getSelectedBlockMetaData(), x, y, z, box);
                    }
                }
            }
        }
    }

    protected void randomlyFillWithBlocks(World world, StructureBoundingBox box, Random rand, float chance, int x1, int y1, int z1, int x2,
            int y2, int z2, Block outer, Block inner, boolean onlyReplaceSolid) {
        for (int y = y1; y <= y2; ++y) {
            for (int x = x1; x <= x2; ++x) {
                for (int z = z1; z <= z2; ++z) {
                    if (rand.nextFloat() <= chance
                            && (!onlyReplaceSolid || this.getBlockAtCurrentPosition(world, x, y, z, box).getMaterial() != Material.air)) {
                        if (y != y1 && y != y2 && x != x1 && x != x2 && z != z1 && z != z2) {
                            this.placeBlockAtCurrentPosition(world, inner, 0, x, y, z, box);
                        } else {
                            this.placeBlockAtCurrentPosition(world, outer, 0, x, y, z, box);
                        }
                    }
                }
            }
        }
    }

    protected void func_151552_a(World world, StructureBoundingBox box, Random rand, float chance, int x, int y, int z, Block block, int meta) {
        if (rand.nextFloat() < chance) this.placeBlockAtCurrentPosition(world, block, meta, x, y, z, box);
    }

    protected void func_151547_a(World world, StructureBoundingBox box, int x1, int y1, int z1, int x2, int y2, int z2, Block block,
            boolean onlyReplaceSolid) {
        float f = (float) (x2 - x1 + 1);
        float f1 = (float) (y2 - y1 + 1);
        float f2 = (float) (z2 - z1 + 1);
        float f3 = (float) x1 + f / 2.0F;
        float f4 = (float) z1 + f2 / 2.0F;
        for (int y = y1; y <= y2; ++y) {
            float f5 = (float) (y - y1) / f1;
            for (int x = x1; x <= x2; ++x) {
                float f6 = ((float) x - f3) / (f * 0.5F);
                for (int z = z1; z <= z2; ++z) {
                    float f7 = ((float) z - f4) / (f2 * 0.5F);
                    if (!onlyReplaceSolid || this.getBlockAtCurrentPosition(world, x, y, z, box).getMaterial() != Material.air) {
                        float f8 = f6 * f6 + f5 * f5 + f7 * f7;
                        if (f8 <= 1.05F) this.placeBlockAtCurrentPosition(world, block, 0, x, y, z, box);
                    }
                }
            }
        }
    }

    protected void clearCurrentPositionBlocksUpwards(World world, int x, int y, int z, StructureBoundingBox box) {
        int i = this.getXWithOffset(x, z);
        int j = this.getYWithOffset(y);
        int k = this.getZWithOffset(x, z);
        if (box.isVecInside(i, j, k)) {
            while (!world.isAirBlock(i, j, k) && j < 255) {
                world.setBlock(i, j, k, Blocks.air, 0, 2);
                ++j;
            }
        }
    }

    protected void func_151554_b(World world, Block block, int meta, int x, int y, int z, StructureBoundingBox box) {
        int i = this.getXWithOffset(x, z);
        int j = this.getYWithOffset(y);
        int k = this.getZWithOffset(x, z);
        if (box.isVecInside(i, j, k)) {
            while ((world.isAirBlock(i, j, k) || world.getBlock(i, j, k).getMaterial().isLiquid()) && j > 1) {
                world.setBlock(i, j, k, block, meta, 2);
                --j;
            }
        }
    }

    protected boolean generateStructureChestContents(World world, StructureBoundingBox box, Random rand, int x, int y, int z,
            WeightedRandomChestContent[] contents, int rolls) {
        int i = this.getXWithOffset(x, z);
        int j = this.getYWithOffset(y);
        int k = this.getZWithOffset(x, z);
        if (box.isVecInside(i, j, k) && world.getBlock(i, j, k) != Blocks.chest) {
            world.setBlock(i, j, k, Blocks.chest, 0, 2);
            TileEntity te = world.getTileEntity(i, j, k);
            if (te instanceof IInventory inv) WeightedRandomChestContent.generateChestContents(rand, contents, inv, rolls);
            return true;
        }
        return false;
    }

    protected boolean generateStructureDispenserContents(World world, StructureBoundingBox box, Random rand, int x, int y, int z, int meta,
            WeightedRandomChestContent[] contents, int rolls) {
        int i = this.getXWithOffset(x, z);
        int j = this.getYWithOffset(y);
        int k = this.getZWithOffset(x, z);
        if (box.isVecInside(i, j, k) && world.getBlock(i, j, k) != Blocks.dispenser) {
            world.setBlock(i, j, k, Blocks.dispenser, this.getMetadataWithOffset(Blocks.dispenser, meta), 2);
            TileEntity te = world.getTileEntity(i, j, k);
            if (te instanceof IInventory inv) WeightedRandomChestContent.generateChestContents(rand, contents, inv, rolls);
            return true;
        }
        return false;
    }

    protected void placeDoorAtCurrentPosition(World world, StructureBoundingBox box, Random rand, int x, int y, int z, int meta) {
        int i = this.getXWithOffset(x, z);
        int j = this.getYWithOffset(y);
        int k = this.getZWithOffset(x, z);
        if (box.isVecInside(i, j, k)) {
            int rotation = this.getMetadataWithOffset(Blocks.wooden_door, meta) & 3;
            int dx = 0, dz = 0;
            if (rotation == 0) dz = 1;
            if (rotation == 1) dx = -1;
            if (rotation == 2) dz = -1;
            if (rotation == 3) dx = 1;
            Block door = Blocks.wooden_door;
            int left = (world.getBlock(i - dx, j, k - dz).isNormalCube() ? 1 : 0) + (world.getBlock(i - dx, j + 1, k - dz).isNormalCube() ? 1 : 0);
            int right = (world.getBlock(i + dx, j, k + dz).isNormalCube() ? 1 : 0) + (world.getBlock(i + dx, j + 1, k + dz).isNormalCube() ? 1 : 0);
            boolean doorLeft = world.getBlock(i - dx, j, k - dz) == door || world.getBlock(i - dx, j + 1, k - dz) == door;
            boolean doorRight = world.getBlock(i + dx, j, k + dz) == door || world.getBlock(i + dx, j + 1, k + dz) == door;
            boolean hinge = doorLeft && !doorRight || right > left;
            world.setBlock(i, j, k, door, rotation, 2);
            world.setBlock(i, j + 1, k, door, 8 | (hinge ? 1 : 0), 2);
        }
    }

    /** Chooses the block (and metadata) of each position of a randomized fill. */
    public abstract static class BlockSelector {
        protected Block field_151562_a = Blocks.air;
        protected int selectedBlockMetaData;

        public abstract void selectBlocks(Random rand, int x, int y, int z, boolean wall);

        public Block func_151561_a() {
            return this.field_151562_a;
        }

        public int getSelectedBlockMetaData() {
            return this.selectedBlockMetaData;
        }
    }
}
