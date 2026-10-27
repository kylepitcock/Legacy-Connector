package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 village pieces: the base class mod village houses extend ({@link Village}: ground level, biome-specific blocks, villagers),
 * the piece weights of the village creation handlers and the village start. Modern villages are jigsaw structures; the pieces build
 * themselves wherever a legacy structure places them.
 */
public class StructureVillagePieces {
    public static void registerVillagePieces() {
    }

    /** How often and how many times a kind of village house may be placed. */
    public static class PieceWeight {
        public Class<? extends Village> villagePieceClass;
        public final int villagePieceWeight;
        public int villagePiecesSpawned;
        public int villagePiecesLimit;

        public PieceWeight(Class<? extends Village> pieceClass, int weight, int limit) {
            this.villagePieceClass = pieceClass;
            this.villagePieceWeight = weight;
            this.villagePiecesLimit = limit;
        }

        public boolean canSpawnMoreVillagePiecesOfType(int componentType) {
            return this.villagePiecesLimit == 0 || this.villagePiecesSpawned < this.villagePiecesLimit;
        }

        public boolean canSpawnMoreVillagePieces() {
            return this.villagePiecesLimit == 0 || this.villagePiecesSpawned < this.villagePiecesLimit;
        }
    }

    /** A village piece: knows its village's style (plains, desert, savanna, taiga) and ground level. */
    public abstract static class Village extends StructureComponent {
        protected int averageGroundLvl = -1;
        private int villagersSpawned;
        protected int structureType;
        protected boolean isZombieInfested;
        protected Start startPiece;

        public Village() {
        }

        protected Village(Start start, int type) {
            super(type);
            if (start != null) {
                this.structureType = start.structureType;
                this.isZombieInfested = start.isZombieInfested;
                this.startPiece = start;
            }
        }

        @Override
        protected void writeStructureToNBT(NBTTagCompound tagCompound) {
            tagCompound.setInteger("HPos", this.averageGroundLvl);
            tagCompound.setInteger("VCount", this.villagersSpawned);
            tagCompound.setByte("Type", (byte) this.structureType);
            tagCompound.setBoolean("Zombie", this.isZombieInfested);
        }

        @Override
        protected void readStructureFromNBT(NBTTagCompound tagCompound, legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager manager) {
            this.averageGroundLvl = tagCompound.getInteger("HPos");
            this.villagersSpawned = tagCompound.getInteger("VCount");
            this.structureType = tagCompound.getByte("Type");
            if (tagCompound.getBoolean("Desert")) this.structureType = 1;
            this.isZombieInfested = tagCompound.getBoolean("Zombie");
        }

        /** Branches of the village road network: placed by the village generator, which the modern game owns. */
        protected StructureComponent getNextComponentNN(Start start, List<StructureComponent> structureComponents, Random rand, int p_74891_4_, int p_74891_5_) {
            return null;
        }

        protected StructureComponent getNextComponentPP(Start start, List<StructureComponent> structureComponents, Random rand, int p_74894_4_, int p_74894_5_) {
            return null;
        }

        /** The average height of the top solid blocks under the piece (at least sea level), or -1 when none of it is in the box. */
        protected int getAverageGroundLevel(World worldIn, StructureBoundingBox structurebb) {
            int sum = 0;
            int count = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int k = this.boundingBox.minZ; k <= this.boundingBox.maxZ; ++k) {
                for (int l = this.boundingBox.minX; l <= this.boundingBox.maxX; ++l) {
                    pos.setPos(l, 64, k);
                    if (structurebb.isVecInside(pos)) {
                        sum += Math.max(worldIn.getTopSolidOrLiquidBlock(pos).getY(), worldIn.getSeaLevel() - 1);
                        ++count;
                    }
                }
            }
            return count == 0 ? -1 : sum / count;
        }

        protected static boolean canVillageGoDeeper(StructureBoundingBox structurebb) {
            return structurebb != null && structurebb.minY > 10;
        }

        /** Spawns up to {@code count} villagers inside the piece (zombie villagers in an infested village), once. */
        protected void spawnVillagers(World worldIn, StructureBoundingBox structurebb, int x, int y, int z, int count) {
            if (this.villagersSpawned >= count) return;
            for (int i = this.villagersSpawned; i < count; ++i) {
                int j = this.getXWithOffset(x + i, z);
                int k = this.getYWithOffset(y);
                int l = this.getZWithOffset(x + i, z);
                if (!structurebb.isVecInside(new BlockPos(j, k, l))) break;
                ++this.villagersSpawned;
                var access = worldIn.mcc$access();
                var level = worldIn.mcc$level();
                if (access == null || level == null) continue;
                var type = this.isZombieInfested ? net.minecraft.world.entity.EntityType.ZOMBIE_VILLAGER : net.minecraft.world.entity.EntityType.VILLAGER;
                var entity = type.create(level);
                if (entity == null) continue;
                entity.moveTo(j + 0.5D, k, l + 0.5D, 0.0F, 0.0F);
                if (entity instanceof net.minecraft.world.entity.Mob mob) mob.setPersistenceRequired();
                if (access instanceof net.minecraft.world.level.ServerLevelAccessor server && entity instanceof net.minecraft.world.entity.Mob mob) {
                    mob.finalizeSpawn(server, server.getCurrentDifficultyAt(entity.blockPosition()), net.minecraft.world.entity.MobSpawnType.STRUCTURE, null);
                }
                access.addFreshEntity(entity);
            }
        }

        protected int chooseProfession(int villagersSpawnedIn, int currentVillagerProfession) {
            return currentVillagerProfession;
        }

        /** The block a village of this style uses instead of a plains block (Forge's GetVillageBlockID event first). */
        protected IBlockState getBiomeSpecificBlockState(IBlockState blockstateIn) {
            var biome = this.startPiece == null ? null : this.startPiece.biome;
            var event = new legacyconnector.v1_12_2.net.minecraftforge.event.terraingen.BiomeEvent.GetVillageBlockID(biome, blockstateIn);
            legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.TERRAIN_GEN_BUS.post(event);
            if (event.getResult() == legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) return event.getReplacement();
            if (this.structureType == 0 || blockstateIn == null) return blockstateIn;
            var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(blockstateIn);
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(modern.getBlock()).getPath();
            String replaced = switch (this.structureType) {
                case 1 -> switch (id) {
                    case "oak_log", "cobblestone", "gravel" -> "sandstone";
                    case "oak_planks" -> "smooth_sandstone";
                    case "oak_stairs", "cobblestone_stairs" -> "sandstone_stairs";
                    default -> null;
                };
                case 2 -> id.startsWith("oak_") ? "acacia_" + id.substring(4) : id.equals("cobblestone") ? "acacia_log" : null;
                case 3 -> id.startsWith("oak_") ? "spruce_" + id.substring(4) : null;
                default -> null;
            };
            if (replaced == null) return blockstateIn;
            var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(replaced));
            var legacy = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(block.withPropertiesOf(modern));
            return legacy == null ? blockstateIn : legacy;
        }

        @Override
        protected void replaceAirAndLiquidDownwards(World worldIn, IBlockState blockstateIn, int x, int y, int z, StructureBoundingBox boundingboxIn) {
            super.replaceAirAndLiquidDownwards(worldIn, this.getBiomeSpecificBlockState(blockstateIn), x, y, z, boundingboxIn);
        }

        protected void placeTorch(World worldIn, EnumFacing facing, int x, int y, int z, StructureBoundingBox structurebb) {
            var torch = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(facing == null || facing.getAxis() == EnumFacing.Axis.Y
                    ? net.minecraft.world.level.block.Blocks.TORCH.defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.WALL_TORCH.defaultBlockState().setValue(net.minecraft.world.level.block.WallTorchBlock.FACING,
                            net.minecraft.core.Direction.from3DDataValue(facing.getIndex())));
            if (torch != null) this.setBlockState(worldIn, torch, x, y, z, structurebb);
        }

        protected void setStructureType(int type) {
            this.structureType = type;
        }
    }

    /** The well at a village's centre; the village start extends it. */
    public static class Well extends Village {
        public Well() {
        }

        public Well(Start start, int type, Random rand, int x, int z) {
            super(start, type);
            this.setCoordBaseMode(EnumFacing.Plane.HORIZONTAL.random(rand));
            this.boundingBox = new StructureBoundingBox(x, 64, z, x + 6 - 1, 78, z + 6 - 1);
        }

        @Override
        public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox structureBoundingBoxIn) {
            return true;
        }
    }

    /** A village's first piece: its style, biome and the house kinds still to place. */
    public static class Start extends Well {
        public legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeProvider biomeProvider;
        public int terrainType;
        public PieceWeight lastPlaced;
        public List<PieceWeight> structureVillageWeightedPieceList;
        public List<StructureComponent> pendingHouses = new ArrayList<>();
        public List<StructureComponent> pendingRoads = new ArrayList<>();
        public legacyconnector.v1_12_2.net.minecraft.world.biome.Biome biome;

        public Start() {
        }

        public Start(legacyconnector.v1_12_2.net.minecraft.world.biome.BiomeProvider provider, int type, Random rand, int x, int z, List<PieceWeight> pieces,
                int terrainType) {
            super(null, 0, rand, x, z);
            this.biomeProvider = provider;
            this.structureVillageWeightedPieceList = pieces;
            this.terrainType = terrainType;
            this.biome = provider == null ? null : provider.getBiome(new BlockPos(x, 0, z), legacyconnector.v1_12_2.net.minecraft.init.Biomes.DEFAULT);
            this.startPiece = this;
            var desert = legacyconnector.v1_12_2.net.minecraft.init.Biomes.DESERT;
            if (biome == desert || biome == legacyconnector.v1_12_2.net.minecraft.init.Biomes.DESERT_HILLS) this.setStructureType(1);
            else if (biome == legacyconnector.v1_12_2.net.minecraft.init.Biomes.SAVANNA || biome == legacyconnector.v1_12_2.net.minecraft.init.Biomes.SAVANNA_PLATEAU) this.setStructureType(2);
            else if (biome == legacyconnector.v1_12_2.net.minecraft.init.Biomes.TAIGA || biome == legacyconnector.v1_12_2.net.minecraft.init.Biomes.TAIGA_HILLS) this.setStructureType(3);
            this.isZombieInfested = rand.nextInt(50) == 0;
        }
    }
}
