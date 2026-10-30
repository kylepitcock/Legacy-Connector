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
        MapGenStructureIO.registerStructureComponent(House1.class, "ViBH");
        MapGenStructureIO.registerStructureComponent(Field1.class, "ViDF");
        MapGenStructureIO.registerStructureComponent(Church.class, "ViST");
        MapGenStructureIO.registerStructureComponent(Path.class, "ViSR");
        MapGenStructureIO.registerStructureComponent(Well.class, "ViW");
        MapGenStructureIO.registerStructureComponent(Start.class, "ViStart");
    }

    /** A modern block state as a legacy one. */
    static IBlockState mcc$state(net.minecraft.world.level.block.state.BlockState state) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(state);
    }

    static IBlockState mcc$of(net.minecraft.world.level.block.Block block) {
        return mcc$state(block.defaultBlockState());
    }

    static net.minecraft.world.level.block.state.BlockState mcc$modern(IBlockState state) {
        return state == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state);
    }

    static IBlockState mcc$stairs(net.minecraft.world.level.block.Block stairs, net.minecraft.core.Direction facing) {
        return mcc$state(stairs.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, facing));
    }

    /**
     * Adds a road piece leading away from {@code x, y, z} (3 wide, 7 to 35 long, as long as it fits) to the village; null when the
     * village is too far out or nothing fits. The houses along roads are the modern village's, so a road adds no houses of its own.
     */
    public static StructureComponent generateAndAddRoadPiece(Start start, List<StructureComponent> list, Random rand, int x, int y, int z, EnumFacing facing,
            int type) {
        if (start == null || type > 3 + start.terrainType) return null;
        if (Math.abs(x - start.getBoundingBox().minX) > 112 || Math.abs(z - start.getBoundingBox().minZ) > 112) return null;
        StructureBoundingBox box = Path.findPieceBox(start, list, rand, x, y, z, facing);
        if (box == null || box.minY <= 10) return null;
        Path path = new Path(start, type, rand, box, facing);
        list.add(path);
        start.pendingRoads.add(path);
        return path;
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

        /** Places the village's door (in the village's wood) facing north in piece coordinates, unless the village is infested. */
        protected void createVillageDoor(World worldIn, StructureBoundingBox sbb, Random rand, int x, int y, int z, EnumFacing facing) {
            if (!this.isZombieInfested) this.generateDoor(worldIn, sbb, rand, x, y, z, EnumFacing.NORTH, this.biomeDoor());
        }

        /** The door of the village's style: acacia in savanna villages, spruce in taiga villages, oak elsewhere. */
        protected legacyconnector.v1_12_2.net.minecraft.block.Block biomeDoor() {
            var door = switch (this.structureType) {
                case 2 -> net.minecraft.world.level.block.Blocks.ACACIA_DOOR;
                case 3 -> net.minecraft.world.level.block.Blocks.SPRUCE_DOOR;
                default -> net.minecraft.world.level.block.Blocks.OAK_DOOR;
            };
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.legacyBlock(door.defaultBlockState());
        }

        /** Whether the block at a piece position is air. */
        protected boolean mcc$isAir(World worldIn, int x, int y, int z, StructureBoundingBox sbb) {
            return mcc$modern(this.getBlockStateFromPos(worldIn, x, y, z, sbb)).isAir();
        }

        /** 1.12's front step: stairs in front of the door where the ground drops, turning a path below back into grass. */
        protected void mcc$frontStep(World worldIn, IBlockState stairs, int x, StructureBoundingBox sbb) {
            if (mcc$isAir(worldIn, x, 0, -1, sbb) && !mcc$isAir(worldIn, x, -1, -1, sbb)) {
                this.setBlockState(worldIn, stairs, x, 0, -1, sbb);
                if (mcc$modern(this.getBlockStateFromPos(worldIn, x, -1, -1, sbb)).is(net.minecraft.world.level.block.Blocks.DIRT_PATH)) {
                    this.setBlockState(worldIn, mcc$of(net.minecraft.world.level.block.Blocks.GRASS_BLOCK), x, -1, -1, sbb);
                }
            }
        }

        protected void setStructureType(int type) {
            this.structureType = type;
        }
    }

    /** A village road (a path piece): turns grass into path, water into planks and sand into gravel on cobblestone along its length. */
    public abstract static class Road extends Village {
        public Road() {
        }

        protected Road(Start start, int type) {
            super(start, type);
        }
    }

    public static class Path extends Road {
        private int length;

        public Path() {
        }

        public Path(Start start, int type, Random rand, StructureBoundingBox box, EnumFacing facing) {
            super(start, type);
            this.setCoordBaseMode(facing);
            this.boundingBox = box;
            this.length = Math.max(box.getXSize(), box.getZSize());
        }

        @Override
        protected void writeStructureToNBT(NBTTagCompound tagCompound) {
            super.writeStructureToNBT(tagCompound);
            tagCompound.setInteger("Length", this.length);
        }

        @Override
        protected void readStructureFromNBT(NBTTagCompound tagCompound, legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager manager) {
            super.readStructureFromNBT(tagCompound, manager);
            this.length = tagCompound.getInteger("Length");
        }

        /** The longest road (a multiple of 7, from 21-35 down to 7) that fits from the position without crossing a piece. */
        public static StructureBoundingBox findPieceBox(Start start, List<StructureComponent> list, Random rand, int x, int y, int z, EnumFacing facing) {
            for (int i = 7 * (3 + rand.nextInt(3)); i >= 7; i -= 7) {
                StructureBoundingBox box = StructureBoundingBox.getComponentToAddBoundingBox(x, y, z, 0, 0, 0, 3, 3, i, facing);
                if (StructureComponent.findIntersecting(list, box) == null) return box;
            }
            return null;
        }

        @Override
        public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox sbb) {
            var access = worldIn.mcc$access();
            if (access == null) return true;
            var path = net.minecraft.world.level.block.Blocks.DIRT_PATH.defaultBlockState();
            var planks = mcc$modern(this.getBiomeSpecificBlockState(mcc$state(net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState())));
            var gravel = mcc$modern(this.getBiomeSpecificBlockState(mcc$state(net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState())));
            var cobble = mcc$modern(this.getBiomeSpecificBlockState(mcc$state(net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState())));
            int sea = worldIn.getSeaLevel();
            for (int i = this.boundingBox.minX; i <= this.boundingBox.maxX; ++i) {
                for (int j = this.boundingBox.minZ; j <= this.boundingBox.maxZ; ++j) {
                    BlockPos column = new BlockPos(i, 64, j);
                    if (!sbb.isVecInside(column)) continue;
                    var pos = worldIn.getTopSolidOrLiquidBlock(column).down().mcc$modern();
                    if (pos.getY() < sea) pos = new net.minecraft.core.BlockPos(pos.getX(), sea - 1, pos.getZ());
                    while (pos.getY() >= sea - 1) {
                        var state = access.getBlockState(pos);
                        if (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) && access.isEmptyBlock(pos.above())) {
                            access.setBlock(pos, path, 2);
                            break;
                        }
                        if (!state.getFluidState().isEmpty()) {
                            access.setBlock(pos, planks, 2);
                            break;
                        }
                        if (state.is(net.minecraft.world.level.block.Blocks.SAND) || state.is(net.minecraft.world.level.block.Blocks.SANDSTONE)
                                || state.is(net.minecraft.world.level.block.Blocks.RED_SANDSTONE)) {
                            access.setBlock(pos, gravel, 2);
                            access.setBlock(pos.below(), cobble, 2);
                            break;
                        }
                        pos = pos.below();
                    }
                }
            }
            return true;
        }
    }

    /** The village church: a cobblestone tower with a ladder to its top, glass windows and torches; one cleric lives there. */
    public static class Church extends Village {
        public Church() {
        }

        public Church(Start start, int type, Random rand, StructureBoundingBox box, EnumFacing facing) {
            super(start, type);
            this.setCoordBaseMode(facing);
            this.boundingBox = box;
        }

        public static Church createPiece(Start start, List<StructureComponent> list, Random rand, int x, int y, int z, EnumFacing facing, int type) {
            StructureBoundingBox box = StructureBoundingBox.getComponentToAddBoundingBox(x, y, z, 0, 0, 0, 5, 12, 9, facing);
            return canVillageGoDeeper(box) && StructureComponent.findIntersecting(list, box) == null ? new Church(start, type, rand, box, facing) : null;
        }

        @Override
        public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox sbb) {
            if (this.averageGroundLvl < 0) {
                this.averageGroundLvl = this.getAverageGroundLevel(worldIn, sbb);
                if (this.averageGroundLvl < 0) return true;
                this.boundingBox.offset(0, this.averageGroundLvl - this.boundingBox.maxY + 12 - 1, 0);
            }
            IBlockState air = mcc$of(net.minecraft.world.level.block.Blocks.AIR);
            IBlockState cobble = this.getBiomeSpecificBlockState(mcc$of(net.minecraft.world.level.block.Blocks.COBBLESTONE));
            IBlockState stairsN = this.getBiomeSpecificBlockState(mcc$stairs(net.minecraft.world.level.block.Blocks.COBBLESTONE_STAIRS, net.minecraft.core.Direction.NORTH));
            IBlockState stairsW = this.getBiomeSpecificBlockState(mcc$stairs(net.minecraft.world.level.block.Blocks.COBBLESTONE_STAIRS, net.minecraft.core.Direction.WEST));
            IBlockState stairsE = this.getBiomeSpecificBlockState(mcc$stairs(net.minecraft.world.level.block.Blocks.COBBLESTONE_STAIRS, net.minecraft.core.Direction.EAST));
            IBlockState glass = mcc$of(net.minecraft.world.level.block.Blocks.GLASS_PANE);
            this.fillWithBlocks(worldIn, sbb, 1, 1, 1, 3, 3, 7, air, air, false);
            this.fillWithBlocks(worldIn, sbb, 1, 5, 1, 3, 9, 3, air, air, false);
            this.fillWithBlocks(worldIn, sbb, 1, 0, 0, 3, 0, 8, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 1, 1, 0, 3, 10, 0, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 0, 1, 1, 0, 10, 3, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 4, 1, 1, 4, 10, 3, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 0, 0, 4, 0, 4, 7, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 4, 0, 4, 4, 4, 7, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 1, 1, 8, 3, 4, 8, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 1, 5, 4, 3, 10, 4, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 1, 5, 5, 3, 5, 7, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 0, 9, 0, 4, 9, 4, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 0, 4, 0, 4, 4, 4, cobble, cobble, false);
            this.setBlockState(worldIn, cobble, 0, 11, 2, sbb);
            this.setBlockState(worldIn, cobble, 4, 11, 2, sbb);
            this.setBlockState(worldIn, cobble, 2, 11, 0, sbb);
            this.setBlockState(worldIn, cobble, 2, 11, 4, sbb);
            this.setBlockState(worldIn, cobble, 1, 1, 6, sbb);
            this.setBlockState(worldIn, cobble, 1, 1, 7, sbb);
            this.setBlockState(worldIn, cobble, 2, 1, 7, sbb);
            this.setBlockState(worldIn, cobble, 3, 1, 6, sbb);
            this.setBlockState(worldIn, cobble, 3, 1, 7, sbb);
            this.setBlockState(worldIn, stairsN, 1, 1, 5, sbb);
            this.setBlockState(worldIn, stairsN, 2, 1, 6, sbb);
            this.setBlockState(worldIn, stairsN, 3, 1, 5, sbb);
            this.setBlockState(worldIn, stairsW, 1, 2, 7, sbb);
            this.setBlockState(worldIn, stairsE, 3, 2, 7, sbb);
            int[][] panes = {{0, 2, 2}, {0, 3, 2}, {4, 2, 2}, {4, 3, 2}, {0, 6, 2}, {0, 7, 2}, {4, 6, 2}, {4, 7, 2}, {2, 6, 0}, {2, 7, 0}, {2, 6, 4},
                    {2, 7, 4}, {0, 3, 6}, {4, 3, 6}, {2, 3, 8}};
            for (int[] p : panes) this.setBlockState(worldIn, glass, p[0], p[1], p[2], sbb);
            this.placeTorch(worldIn, EnumFacing.SOUTH, 2, 4, 7, sbb);
            this.placeTorch(worldIn, EnumFacing.EAST, 1, 4, 6, sbb);
            this.placeTorch(worldIn, EnumFacing.WEST, 3, 4, 6, sbb);
            this.placeTorch(worldIn, EnumFacing.NORTH, 2, 4, 5, sbb);
            IBlockState ladder = mcc$state(net.minecraft.world.level.block.Blocks.LADDER.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.WEST));
            for (int i = 1; i <= 9; ++i) this.setBlockState(worldIn, ladder, 3, i, 3, sbb);
            this.setBlockState(worldIn, air, 2, 1, 0, sbb);
            this.setBlockState(worldIn, air, 2, 2, 0, sbb);
            this.createVillageDoor(worldIn, sbb, randomIn, 2, 1, 0, EnumFacing.NORTH);
            this.mcc$frontStep(worldIn, stairsN, 2, sbb);
            for (int k = 0; k < 9; ++k) {
                for (int j = 0; j < 5; ++j) {
                    this.clearCurrentPositionBlocksUpwards(worldIn, j, 12, k, sbb);
                    this.replaceAirAndLiquidDownwards(worldIn, cobble, j, -1, k, sbb);
                }
            }
            this.spawnVillagers(worldIn, sbb, 2, 1, 2, 1);
            return true;
        }

        @Override
        protected int chooseProfession(int villagersSpawnedIn, int currentVillagerProfession) {
            return 2;
        }
    }

    /** The village library: bookshelves, a crafting table and two chairs with pressure plate tables; a librarian lives there. */
    public static class House1 extends Village {
        public House1() {
        }

        public House1(Start start, int type, Random rand, StructureBoundingBox box, EnumFacing facing) {
            super(start, type);
            this.setCoordBaseMode(facing);
            this.boundingBox = box;
        }

        public static House1 createPiece(Start start, List<StructureComponent> list, Random rand, int x, int y, int z, EnumFacing facing, int type) {
            StructureBoundingBox box = StructureBoundingBox.getComponentToAddBoundingBox(x, y, z, 0, 0, 0, 9, 9, 6, facing);
            return canVillageGoDeeper(box) && StructureComponent.findIntersecting(list, box) == null ? new House1(start, type, rand, box, facing) : null;
        }

        @Override
        public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox sbb) {
            if (this.averageGroundLvl < 0) {
                this.averageGroundLvl = this.getAverageGroundLevel(worldIn, sbb);
                if (this.averageGroundLvl < 0) return true;
                this.boundingBox.offset(0, this.averageGroundLvl - this.boundingBox.maxY + 9 - 1, 0);
            }
            IBlockState air = mcc$of(net.minecraft.world.level.block.Blocks.AIR);
            IBlockState cobble = this.getBiomeSpecificBlockState(mcc$of(net.minecraft.world.level.block.Blocks.COBBLESTONE));
            IBlockState stairsN = this.getBiomeSpecificBlockState(mcc$stairs(net.minecraft.world.level.block.Blocks.OAK_STAIRS, net.minecraft.core.Direction.NORTH));
            IBlockState stairsS = this.getBiomeSpecificBlockState(mcc$stairs(net.minecraft.world.level.block.Blocks.OAK_STAIRS, net.minecraft.core.Direction.SOUTH));
            IBlockState stairsE = this.getBiomeSpecificBlockState(mcc$stairs(net.minecraft.world.level.block.Blocks.OAK_STAIRS, net.minecraft.core.Direction.EAST));
            IBlockState planks = this.getBiomeSpecificBlockState(mcc$of(net.minecraft.world.level.block.Blocks.OAK_PLANKS));
            IBlockState stoneStairsN = this.getBiomeSpecificBlockState(mcc$stairs(net.minecraft.world.level.block.Blocks.COBBLESTONE_STAIRS, net.minecraft.core.Direction.NORTH));
            IBlockState fence = this.getBiomeSpecificBlockState(mcc$of(net.minecraft.world.level.block.Blocks.OAK_FENCE));
            IBlockState glass = mcc$of(net.minecraft.world.level.block.Blocks.GLASS_PANE);
            IBlockState shelf = mcc$of(net.minecraft.world.level.block.Blocks.BOOKSHELF);
            IBlockState plate = mcc$of(net.minecraft.world.level.block.Blocks.OAK_PRESSURE_PLATE);
            this.fillWithBlocks(worldIn, sbb, 1, 1, 1, 7, 5, 4, air, air, false);
            this.fillWithBlocks(worldIn, sbb, 0, 0, 0, 8, 0, 5, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 0, 5, 0, 8, 5, 5, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 0, 6, 1, 8, 6, 4, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 0, 7, 2, 8, 7, 3, cobble, cobble, false);
            for (int i = -1; i <= 2; ++i) {
                for (int j = 0; j <= 8; ++j) {
                    this.setBlockState(worldIn, stairsN, j, 6 + i, i, sbb);
                    this.setBlockState(worldIn, stairsS, j, 6 + i, 5 - i, sbb);
                }
            }
            this.fillWithBlocks(worldIn, sbb, 0, 1, 0, 0, 1, 5, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 1, 1, 5, 8, 1, 5, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 8, 1, 0, 8, 1, 4, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 2, 1, 0, 7, 1, 0, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 0, 2, 0, 0, 4, 0, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 0, 2, 5, 0, 4, 5, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 8, 2, 5, 8, 4, 5, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 8, 2, 0, 8, 4, 0, cobble, cobble, false);
            this.fillWithBlocks(worldIn, sbb, 0, 2, 1, 0, 4, 4, planks, planks, false);
            this.fillWithBlocks(worldIn, sbb, 1, 2, 5, 7, 4, 5, planks, planks, false);
            this.fillWithBlocks(worldIn, sbb, 8, 2, 1, 8, 4, 4, planks, planks, false);
            this.fillWithBlocks(worldIn, sbb, 1, 2, 0, 7, 4, 0, planks, planks, false);
            int[][] panes = {{4, 2, 0}, {5, 2, 0}, {6, 2, 0}, {4, 3, 0}, {5, 3, 0}, {6, 3, 0}, {0, 2, 2}, {0, 2, 3}, {0, 3, 2}, {0, 3, 3}, {8, 2, 2},
                    {8, 2, 3}, {8, 3, 2}, {8, 3, 3}, {2, 2, 5}, {3, 2, 5}, {5, 2, 5}, {6, 2, 5}};
            for (int[] p : panes) this.setBlockState(worldIn, glass, p[0], p[1], p[2], sbb);
            this.fillWithBlocks(worldIn, sbb, 1, 4, 1, 7, 4, 1, planks, planks, false);
            this.fillWithBlocks(worldIn, sbb, 1, 4, 4, 7, 4, 4, planks, planks, false);
            this.fillWithBlocks(worldIn, sbb, 1, 3, 4, 7, 3, 4, shelf, shelf, false);
            this.setBlockState(worldIn, planks, 7, 1, 4, sbb);
            this.setBlockState(worldIn, stairsE, 7, 1, 3, sbb);
            this.setBlockState(worldIn, stairsN, 6, 1, 4, sbb);
            this.setBlockState(worldIn, stairsN, 5, 1, 4, sbb);
            this.setBlockState(worldIn, stairsN, 4, 1, 4, sbb);
            this.setBlockState(worldIn, stairsN, 3, 1, 4, sbb);
            this.setBlockState(worldIn, fence, 6, 1, 3, sbb);
            this.setBlockState(worldIn, plate, 6, 2, 3, sbb);
            this.setBlockState(worldIn, fence, 4, 1, 3, sbb);
            this.setBlockState(worldIn, plate, 4, 2, 3, sbb);
            this.setBlockState(worldIn, mcc$of(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE), 7, 1, 1, sbb);
            this.setBlockState(worldIn, air, 1, 1, 0, sbb);
            this.setBlockState(worldIn, air, 1, 2, 0, sbb);
            this.createVillageDoor(worldIn, sbb, randomIn, 1, 1, 0, EnumFacing.NORTH);
            this.mcc$frontStep(worldIn, stoneStairsN, 1, sbb);
            for (int l = 0; l < 6; ++l) {
                for (int k = 0; k < 9; ++k) {
                    this.clearCurrentPositionBlocksUpwards(worldIn, k, 9, l, sbb);
                    this.replaceAirAndLiquidDownwards(worldIn, cobble, k, -1, l, sbb);
                }
            }
            this.spawnVillagers(worldIn, sbb, 2, 1, 2, 1);
            return true;
        }

        @Override
        protected int chooseProfession(int villagersSpawnedIn, int currentVillagerProfession) {
            return villagersSpawnedIn == 0 ? 4 : super.chooseProfession(villagersSpawnedIn, currentVillagerProfession);
        }
    }

    /** The large village farm: four double rows of crops (wheat mostly, carrots, potatoes, beetroot) with two water channels, framed by logs. */
    public static class Field1 extends Village {
        private net.minecraft.world.level.block.Block cropTypeA = net.minecraft.world.level.block.Blocks.WHEAT;
        private net.minecraft.world.level.block.Block cropTypeB = net.minecraft.world.level.block.Blocks.WHEAT;
        private net.minecraft.world.level.block.Block cropTypeC = net.minecraft.world.level.block.Blocks.WHEAT;
        private net.minecraft.world.level.block.Block cropTypeD = net.minecraft.world.level.block.Blocks.WHEAT;

        public Field1() {
        }

        public Field1(Start start, int type, Random rand, StructureBoundingBox box, EnumFacing facing) {
            super(start, type);
            this.setCoordBaseMode(facing);
            this.boundingBox = box;
            this.cropTypeA = mcc$randomCrop(rand);
            this.cropTypeB = mcc$randomCrop(rand);
            this.cropTypeC = mcc$randomCrop(rand);
            this.cropTypeD = mcc$randomCrop(rand);
        }

        private static net.minecraft.world.level.block.Block mcc$randomCrop(Random rand) {
            return switch (rand.nextInt(10)) {
                case 0, 1 -> net.minecraft.world.level.block.Blocks.CARROTS;
                case 2, 3 -> net.minecraft.world.level.block.Blocks.POTATOES;
                case 4 -> net.minecraft.world.level.block.Blocks.BEETROOTS;
                default -> net.minecraft.world.level.block.Blocks.WHEAT;
            };
        }

        private static net.minecraft.world.level.block.Block mcc$crop(String id) {
            var rl = net.minecraft.resources.ResourceLocation.tryParse(id);
            var b = rl == null ? null : net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(rl);
            return b instanceof net.minecraft.world.level.block.CropBlock ? b : net.minecraft.world.level.block.Blocks.WHEAT;
        }

        private static String mcc$name(net.minecraft.world.level.block.Block b) {
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).toString();
        }

        @Override
        protected void writeStructureToNBT(NBTTagCompound tagCompound) {
            super.writeStructureToNBT(tagCompound);
            tagCompound.setString("CA", mcc$name(cropTypeA));
            tagCompound.setString("CB", mcc$name(cropTypeB));
            tagCompound.setString("CC", mcc$name(cropTypeC));
            tagCompound.setString("CD", mcc$name(cropTypeD));
        }

        @Override
        protected void readStructureFromNBT(NBTTagCompound tagCompound, legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager manager) {
            super.readStructureFromNBT(tagCompound, manager);
            this.cropTypeA = mcc$crop(tagCompound.getString("CA"));
            this.cropTypeB = mcc$crop(tagCompound.getString("CB"));
            this.cropTypeC = mcc$crop(tagCompound.getString("CC"));
            this.cropTypeD = mcc$crop(tagCompound.getString("CD"));
        }

        public static Field1 createPiece(Start start, List<StructureComponent> list, Random rand, int x, int y, int z, EnumFacing facing, int type) {
            StructureBoundingBox box = StructureBoundingBox.getComponentToAddBoundingBox(x, y, z, 0, 0, 0, 13, 4, 9, facing);
            return canVillageGoDeeper(box) && StructureComponent.findIntersecting(list, box) == null ? new Field1(start, type, rand, box, facing) : null;
        }

        private static IBlockState mcc$cropState(net.minecraft.world.level.block.Block crop, Random rand) {
            if (!(crop instanceof net.minecraft.world.level.block.CropBlock c)) return mcc$of(crop);
            int max = c.getMaxAge();
            int min = max / 3;
            return mcc$state(c.getStateForAge(min + rand.nextInt(max - min + 1)));
        }

        @Override
        public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox sbb) {
            if (this.averageGroundLvl < 0) {
                this.averageGroundLvl = this.getAverageGroundLevel(worldIn, sbb);
                if (this.averageGroundLvl < 0) return true;
                this.boundingBox.offset(0, this.averageGroundLvl - this.boundingBox.maxY + 4 - 1, 0);
            }
            IBlockState air = mcc$of(net.minecraft.world.level.block.Blocks.AIR);
            IBlockState log = this.getBiomeSpecificBlockState(mcc$of(net.minecraft.world.level.block.Blocks.OAK_LOG));
            IBlockState farmland = mcc$of(net.minecraft.world.level.block.Blocks.FARMLAND);
            IBlockState water = mcc$of(net.minecraft.world.level.block.Blocks.WATER);
            this.fillWithBlocks(worldIn, sbb, 0, 1, 0, 12, 4, 8, air, air, false);
            this.fillWithBlocks(worldIn, sbb, 1, 0, 1, 2, 0, 7, farmland, farmland, false);
            this.fillWithBlocks(worldIn, sbb, 4, 0, 1, 5, 0, 7, farmland, farmland, false);
            this.fillWithBlocks(worldIn, sbb, 7, 0, 1, 8, 0, 7, farmland, farmland, false);
            this.fillWithBlocks(worldIn, sbb, 10, 0, 1, 11, 0, 7, farmland, farmland, false);
            this.fillWithBlocks(worldIn, sbb, 0, 0, 0, 0, 0, 8, log, log, false);
            this.fillWithBlocks(worldIn, sbb, 6, 0, 0, 6, 0, 8, log, log, false);
            this.fillWithBlocks(worldIn, sbb, 12, 0, 0, 12, 0, 8, log, log, false);
            this.fillWithBlocks(worldIn, sbb, 1, 0, 0, 11, 0, 0, log, log, false);
            this.fillWithBlocks(worldIn, sbb, 1, 0, 8, 11, 0, 8, log, log, false);
            this.fillWithBlocks(worldIn, sbb, 3, 0, 1, 3, 0, 7, water, water, false);
            this.fillWithBlocks(worldIn, sbb, 9, 0, 1, 9, 0, 7, water, water, false);
            for (int i = 1; i <= 7; ++i) {
                this.setBlockState(worldIn, mcc$cropState(cropTypeA, randomIn), 1, 1, i, sbb);
                this.setBlockState(worldIn, mcc$cropState(cropTypeA, randomIn), 2, 1, i, sbb);
                this.setBlockState(worldIn, mcc$cropState(cropTypeB, randomIn), 4, 1, i, sbb);
                this.setBlockState(worldIn, mcc$cropState(cropTypeB, randomIn), 5, 1, i, sbb);
                this.setBlockState(worldIn, mcc$cropState(cropTypeC, randomIn), 7, 1, i, sbb);
                this.setBlockState(worldIn, mcc$cropState(cropTypeC, randomIn), 8, 1, i, sbb);
                this.setBlockState(worldIn, mcc$cropState(cropTypeD, randomIn), 10, 1, i, sbb);
                this.setBlockState(worldIn, mcc$cropState(cropTypeD, randomIn), 11, 1, i, sbb);
            }
            IBlockState dirt = mcc$of(net.minecraft.world.level.block.Blocks.DIRT);
            for (int z = 0; z < 9; ++z) {
                for (int x = 0; x < 13; ++x) {
                    this.clearCurrentPositionBlocksUpwards(worldIn, x, 4, z, sbb);
                    this.replaceAirAndLiquidDownwards(worldIn, dirt, x, -1, z, sbb);
                }
            }
            return true;
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
