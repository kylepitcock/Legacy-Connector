package legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureBoundingBox;

/** How a template is placed: mirror, rotation, bounds, integrity, replaced block. */
public class PlacementSettings {
    private Mirror mirror = Mirror.NONE;
    private Rotation rotation = Rotation.NONE;
    private boolean ignoreEntities;
    private Block replacedBlock;
    private ChunkPos chunk;
    private StructureBoundingBox boundingBox;
    private boolean ignoreStructureBlock = true;
    private float integrity = 1.0F;
    private Random random;
    private Long setSeed;

    public PlacementSettings() {
    }

    public PlacementSettings copy() {
        PlacementSettings p = new PlacementSettings();
        p.mirror = mirror;
        p.rotation = rotation;
        p.ignoreEntities = ignoreEntities;
        p.replacedBlock = replacedBlock;
        p.chunk = chunk;
        p.boundingBox = boundingBox;
        p.ignoreStructureBlock = ignoreStructureBlock;
        p.integrity = integrity;
        p.random = random;
        p.setSeed = setSeed;
        return p;
    }

    public PlacementSettings setMirror(Mirror mirrorIn) {
        this.mirror = mirrorIn;
        return this;
    }

    public PlacementSettings setRotation(Rotation rotationIn) {
        this.rotation = rotationIn;
        return this;
    }

    public PlacementSettings setIgnoreEntities(boolean ignoreEntitiesIn) {
        this.ignoreEntities = ignoreEntitiesIn;
        return this;
    }

    public PlacementSettings setReplacedBlock(Block replacedBlockIn) {
        this.replacedBlock = replacedBlockIn;
        return this;
    }

    public PlacementSettings setChunk(ChunkPos chunkPosIn) {
        this.chunk = chunkPosIn;
        return this;
    }

    public PlacementSettings setBoundingBox(StructureBoundingBox boundingBoxIn) {
        this.boundingBox = boundingBoxIn;
        return this;
    }

    public PlacementSettings setSeed(Long seedIn) {
        this.setSeed = seedIn;
        return this;
    }

    public PlacementSettings setRandom(Random randomIn) {
        this.random = randomIn;
        return this;
    }

    public PlacementSettings setIntegrity(float integrityIn) {
        this.integrity = integrityIn;
        return this;
    }

    public PlacementSettings setIgnoreStructureBlock(boolean ignoreStructureBlockIn) {
        this.ignoreStructureBlock = ignoreStructureBlockIn;
        return this;
    }

    public Mirror getMirror() {
        return mirror;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public Random getRandom(BlockPos seed) {
        if (random != null) return random;
        if (setSeed != null) return setSeed == 0L ? new Random(System.currentTimeMillis()) : new Random(setSeed);
        if (seed == null) return new Random(System.currentTimeMillis());
        int i = seed.getX();
        int j = seed.getZ();
        return new Random(i * i * 4987142 + i * 5947611 + j * j * 4392871L + j * 389711 ^ 987234911L);
    }

    public float getIntegrity() {
        return integrity;
    }

    public boolean getIgnoreEntities() {
        return ignoreEntities;
    }

    public Block getReplacedBlock() {
        return replacedBlock;
    }

    public StructureBoundingBox getBoundingBox() {
        if (boundingBox == null && chunk != null) {
            int x = chunk.x * 16;
            int z = chunk.z * 16;
            boundingBox = new StructureBoundingBox(x, 0, z, x + 15, 255, z + 15);
        }
        return boundingBox;
    }

    public boolean getIgnoreStructureBlock() {
        return ignoreStructureBlock;
    }
}
