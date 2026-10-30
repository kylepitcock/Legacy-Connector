package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 generator of the "Monument" structure. The modern game places these structures itself, so this one starts none of its own;
 * nearest/inside queries answer from the modern structures (see {@code Structures1122}).
 */
public class StructureOceanMonument extends MapGenStructure {
    public StructureOceanMonument() {
    }

    private final java.util.List<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry> monsters = new java.util.ArrayList<>(java.util.List.of(new legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry(legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityGuardian.class, 1, 2, 4)));

    /** The mobs that spawn in monuments (guardians). */
    public java.util.List<legacyconnector.v1_12_2.net.minecraft.world.biome.Biome.SpawnListEntry> getMonsters() {
        return monsters;
    }

    @Override
    public String getStructureName() {
        return "Monument";
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
        return false;
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        return new StructureStart(chunkX, chunkZ);
    }

    @Override
    public BlockPos getNearestStructurePos(World worldIn, BlockPos pos, boolean findUnexplored) {
        this.world = worldIn;
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.nearest(worldIn, "Monument", pos, findUnexplored);
    }

    @Override
    public boolean isInsideStructure(BlockPos pos) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.inside(this.world, "Monument", pos) || super.isInsideStructure(pos);
    }

    @Override
    public boolean isPositionInStructure(World worldIn, BlockPos pos) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Structures1122.inside(worldIn, "Monument", pos) || super.isPositionInStructure(worldIn, pos);
    }

    /** A monument a mod's generator starts in a chunk: one monument building centred on the chunk, facing a random way (1.12's seeding). */
    public static class StartMonument extends StructureStart {
        private final java.util.Set<legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos> processed = new java.util.HashSet<>();
        private boolean wasCreated;

        public StartMonument() {
        }

        public StartMonument(World worldIn, java.util.Random random, int chunkX, int chunkZ) {
            super(chunkX, chunkZ);
            this.create(worldIn, random, chunkX, chunkZ);
        }

        private void create(World worldIn, java.util.Random random, int chunkX, int chunkZ) {
            random.setSeed(worldIn.getSeed());
            long i = random.nextLong();
            long j = random.nextLong();
            long k = (long) chunkX * i;
            long l = (long) chunkZ * j;
            random.setSeed(k ^ l ^ worldIn.getSeed());
            int x = chunkX * 16 + 8 - 29;
            int z = chunkZ * 16 + 8 - 29;
            var facing = legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.Plane.HORIZONTAL.random(random);
            this.components.add(new StructureOceanMonumentPieces.MonumentBuilding(random, x, z, facing));
            this.updateBoundingBox();
            this.wasCreated = true;
        }

        @Override
        public void generateStructure(World worldIn, java.util.Random rand, StructureBoundingBox structurebb) {
            if (!this.wasCreated) {
                this.components.clear();
                this.create(worldIn, rand, this.getChunkPosX(), this.getChunkPosZ());
            }
            super.generateStructure(worldIn, rand, structurebb);
        }

        @Override
        public boolean isValidForPostProcess(legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos pair) {
            return !this.processed.contains(pair) && super.isValidForPostProcess(pair);
        }

        @Override
        public void notifyPostProcessAt(legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos pair) {
            super.notifyPostProcessAt(pair);
            this.processed.add(pair);
        }

        @Override
        public void writeToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tagCompound) {
            super.writeToNBT(tagCompound);
            var list = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList();
            for (var pos : this.processed) {
                var tag = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound();
                tag.setInteger("X", pos.x);
                tag.setInteger("Z", pos.z);
                list.appendTag(tag);
            }
            tagCompound.setTag("Processed", list);
        }

        @Override
        public void readFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tagCompound) {
            super.readFromNBT(tagCompound);
            this.wasCreated = true;
            if (tagCompound.hasKey("Processed", 9)) {
                var list = tagCompound.getTagList("Processed", 10);
                for (int i = 0; i < list.tagCount(); ++i) {
                    var tag = list.getCompoundTagAt(i);
                    this.processed.add(new legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos(tag.getInteger("X"), tag.getInteger("Z")));
                }
            }
        }
    }
}
