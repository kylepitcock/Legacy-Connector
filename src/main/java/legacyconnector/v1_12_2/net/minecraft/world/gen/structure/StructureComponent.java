package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * One piece of a 1.12.2 structure. Placement methods take coordinates relative to the piece and turn them to the piece's facing
 * ({@link #getCoordBaseMode}); blocks outside the box being generated are skipped, and placed states are mirrored and rotated
 * like the piece.
 */
public abstract class StructureComponent {
    protected StructureBoundingBox boundingBox;
    private EnumFacing coordBaseMode;
    private Mirror mirror = Mirror.NONE;
    private Rotation rotation = Rotation.NONE;
    protected int componentType;

    public StructureComponent() {
    }

    protected StructureComponent(int type) {
        this.componentType = type;
    }

    private static final String NBT = "Llegacyconnector/v1_12_2/net/minecraft/nbt/NBTTagCompound;";

    public final NBTTagCompound createStructureBaseNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", MapGenStructureIO.getStructureComponentName(this));
        tag.setTag("BB", boundingBox.toNBTTagIntArray());
        EnumFacing facing = getCoordBaseMode();
        tag.setInteger("O", facing == null ? -1 : facing.getHorizontalIndex());
        tag.setInteger("GD", componentType);
        writeStructureToNBT(tag);
        return tag;
    }

    /** The piece's own data; a mod piece overrides it. */
    protected void writeStructureToNBT(NBTTagCompound tagCompound) {
    }

    public void readStructureBaseNBT(World world, NBTTagCompound tag) {
        if (tag.hasKey("BB")) boundingBox = new StructureBoundingBox(tag.getIntArray("BB"));
        int o = tag.getInteger("O");
        setCoordBaseMode(o == -1 ? null : EnumFacing.byHorizontalIndex(o));
        componentType = tag.getInteger("GD");
        // readStructureFromNBT(NBTTagCompound, TemplateManager) of the mod's piece
        try {
            readStructureFromNBT(tag, world == null ? null : world.getSaveHandler().getStructureTemplateManager());
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Structures").warn("Structure piece {} could not read its data: {}", getClass().getName(), e.toString());
        }
    }

    protected void readStructureFromNBT(NBTTagCompound tag, legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager manager) {
    }

    public void buildComponent(StructureComponent componentIn, List<StructureComponent> listIn, Random rand) {
    }

    public abstract boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox structureBoundingBoxIn);

    public StructureBoundingBox getBoundingBox() {
        return boundingBox;
    }

    public int getComponentType() {
        return componentType;
    }

    public static StructureComponent findIntersecting(List<StructureComponent> listIn, StructureBoundingBox boundingboxIn) {
        for (StructureComponent c : listIn) {
            if (c.getBoundingBox() != null && c.getBoundingBox().intersectsWith(boundingboxIn)) return c;
        }
        return null;
    }

    protected boolean isLiquidInStructureBoundingBox(World world, StructureBoundingBox box) {
        int i = Math.max(boundingBox.minX - 1, box.minX);
        int j = Math.max(boundingBox.minY - 1, box.minY);
        int k = Math.max(boundingBox.minZ - 1, box.minZ);
        int l = Math.min(boundingBox.maxX + 1, box.maxX);
        int i1 = Math.min(boundingBox.maxY + 1, box.maxY);
        int j1 = Math.min(boundingBox.maxZ + 1, box.maxZ);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = i; x <= l; ++x) {
            for (int z = k; z <= j1; ++z) {
                if (liquid(world, p.setPos(x, j, z)) || liquid(world, p.setPos(x, i1, z))) return true;
            }
        }
        for (int x = i; x <= l; ++x) {
            for (int y = j; y <= i1; ++y) {
                if (liquid(world, p.setPos(x, y, k)) || liquid(world, p.setPos(x, y, j1))) return true;
            }
        }
        for (int z = k; z <= j1; ++z) {
            for (int y = j; y <= i1; ++y) {
                if (liquid(world, p.setPos(i, y, z)) || liquid(world, p.setPos(l, y, z))) return true;
            }
        }
        return false;
    }

    private static boolean liquid(World world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        return s != null && s.getMaterial() != null && s.getMaterial().isLiquid();
    }

    protected int getXWithOffset(int x, int z) {
        EnumFacing f = getCoordBaseMode();
        if (f == null) return x;
        switch (f.getName()) {
            case "north":
            case "south":
                return boundingBox.minX + x;
            case "west":
                return boundingBox.maxX - z;
            case "east":
                return boundingBox.minX + z;
            default:
                return x;
        }
    }

    protected int getYWithOffset(int y) {
        return getCoordBaseMode() == null ? y : y + boundingBox.minY;
    }

    protected int getZWithOffset(int x, int z) {
        EnumFacing f = getCoordBaseMode();
        if (f == null) return z;
        switch (f.getName()) {
            case "north":
                return boundingBox.maxZ - z;
            case "south":
                return boundingBox.minZ + z;
            case "west":
            case "east":
                return boundingBox.minZ + x;
            default:
                return z;
        }
    }

    private static final String STATE = "Llegacyconnector/v1_12_2/net/minecraft/block/state/IBlockState;";

    /** The state turned like the piece: the block's own withMirror/withRotation (a vanilla block's modern state is turned). */
    private IBlockState turned(IBlockState state) {
        if (state == null) return null;
        if (mirror != Mirror.NONE) state = transform(state, "withMirror", "Llegacyconnector/v1_12_2/net/minecraft/util/Mirror;", mirror);
        if (rotation != Rotation.NONE) state = transform(state, "withRotation", "Llegacyconnector/v1_12_2/net/minecraft/util/Rotation;", rotation);
        return state;
    }

    private static IBlockState transform(IBlockState state, String name, String argDesc, Object arg) {
        try {
            Object own = org.kylepitcock.legacyconnector.core.synth.Synth.dispatch(state.getBlock(), name, "(" + STATE + argDesc + ")" + STATE,
                    new Object[] {state, arg});
            if (own instanceof IBlockState s) return s;
        } catch (Throwable e) {
            // keep the state as it is
        }
        return state;
    }

    protected void setBlockState(World worldIn, IBlockState blockstateIn, int x, int y, int z, StructureBoundingBox boundingboxIn) {
        BlockPos pos = new BlockPos(getXWithOffset(x, z), getYWithOffset(y), getZWithOffset(x, z));
        if (boundingboxIn.isVecInside(pos)) worldIn.setBlockState(pos, turned(blockstateIn), 2);
    }

    protected int getSkyBrightness(World worldIn, int x, int y, int z, StructureBoundingBox boundingboxIn) {
        BlockPos pos = new BlockPos(getXWithOffset(x, z), getYWithOffset(y + 1), getZWithOffset(x, z));
        return !boundingboxIn.isVecInside(pos) ? 15 : worldIn.getLight(pos);
    }

    protected IBlockState getBlockStateFromPos(World worldIn, int x, int y, int z, StructureBoundingBox boundingboxIn) {
        BlockPos pos = new BlockPos(getXWithOffset(x, z), getYWithOffset(y), getZWithOffset(x, z));
        return !boundingboxIn.isVecInside(pos) ? air() : worldIn.getBlockState(pos);
    }

    private static IBlockState air() {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
    }

    private boolean isAirAt(World world, int x, int y, int z, StructureBoundingBox box) {
        IBlockState s = getBlockStateFromPos(world, x, y, z, box);
        return s == null || s.getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.AIR;
    }

    protected void fillWithAir(World worldIn, StructureBoundingBox structurebb, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) setBlockState(worldIn, air(), x, y, z, structurebb);
            }
        }
    }

    protected void fillWithBlocks(World worldIn, StructureBoundingBox boundingboxIn, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax,
            IBlockState boundaryBlockState, IBlockState insideBlockState, boolean existingOnly) {
        for (int y = yMin; y <= yMax; ++y) {
            for (int x = xMin; x <= xMax; ++x) {
                for (int z = zMin; z <= zMax; ++z) {
                    if (!existingOnly || !isAirAt(worldIn, x, y, z, boundingboxIn)) {
                        boolean edge = y == yMin || y == yMax || x == xMin || x == xMax || z == zMin || z == zMax;
                        setBlockState(worldIn, edge ? boundaryBlockState : insideBlockState, x, y, z, boundingboxIn);
                    }
                }
            }
        }
    }

    protected void fillWithRandomizedBlocks(World worldIn, StructureBoundingBox boundingboxIn, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ, boolean alwaysReplace, Random rand, BlockSelector blockselector) {
        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    if (!alwaysReplace || !isAirAt(worldIn, x, y, z, boundingboxIn)) {
                        blockselector.selectBlocks(rand, x, y, z, y == minY || y == maxY || x == minX || x == maxX || z == minZ || z == maxZ);
                        setBlockState(worldIn, blockselector.getBlockState(), x, y, z, boundingboxIn);
                    }
                }
            }
        }
    }

    protected void generateMaybeBox(World worldIn, StructureBoundingBox sbb, Random rand, float chance, int minX, int minY, int minZ, int maxX,
            int maxY, int maxZ, IBlockState edgeState, IBlockState state, boolean requireNonAir, int requiredSkylight) {
        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    if (rand.nextFloat() <= chance && (!requireNonAir || !isAirAt(worldIn, x, y, z, sbb))
                            && (requiredSkylight <= 0 || getSkyBrightness(worldIn, x, y, z, sbb) < requiredSkylight)) {
                        boolean edge = y == minY || y == maxY || x == minX || x == maxX || z == minZ || z == maxZ;
                        setBlockState(worldIn, edge ? edgeState : state, x, y, z, sbb);
                    }
                }
            }
        }
    }

    protected void randomlyPlaceBlock(World worldIn, StructureBoundingBox boundingboxIn, Random rand, float chance, int x, int y, int z,
            IBlockState blockstateIn) {
        if (rand.nextFloat() < chance) setBlockState(worldIn, blockstateIn, x, y, z, boundingboxIn);
    }

    protected void randomlyRareFillWithBlocks(World worldIn, StructureBoundingBox boundingboxIn, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ, IBlockState blockstateIn, boolean excludeAir) {
        float f = maxX - minX + 1;
        float f1 = maxY - minY + 1;
        float f2 = maxZ - minZ + 1;
        float f3 = minX + f / 2.0F;
        float f4 = minZ + f2 / 2.0F;
        for (int y = minY; y <= maxY; ++y) {
            float f5 = (float) (y - minY) / f1;
            for (int x = minX; x <= maxX; ++x) {
                float f6 = (x - f3) / (f * 0.5F);
                for (int z = minZ; z <= maxZ; ++z) {
                    float f7 = (z - f4) / (f2 * 0.5F);
                    if (!excludeAir || !isAirAt(worldIn, x, y, z, boundingboxIn)) {
                        if (f6 * f6 + f5 * f5 + f7 * f7 <= 1.05F) setBlockState(worldIn, blockstateIn, x, y, z, boundingboxIn);
                    }
                }
            }
        }
    }

    protected void clearCurrentPositionBlocksUpwards(World worldIn, int x, int y, int z, StructureBoundingBox structurebb) {
        BlockPos pos = new BlockPos(getXWithOffset(x, z), getYWithOffset(y), getZWithOffset(x, z));
        if (structurebb.isVecInside(pos)) {
            while (!worldIn.isAirBlock(pos) && pos.getY() < 255) {
                worldIn.setBlockState(pos, air(), 2);
                pos = pos.up();
            }
        }
    }

    protected void replaceAirAndLiquidDownwards(World worldIn, IBlockState blockstateIn, int x, int y, int z, StructureBoundingBox boundingboxIn) {
        int i = getXWithOffset(x, z);
        int j = getYWithOffset(y);
        int k = getZWithOffset(x, z);
        if (boundingboxIn.mcc$isInside(i, j, k)) {
            IBlockState turned = turned(blockstateIn);
            while ((worldIn.isAirBlock(new BlockPos(i, j, k)) || liquid(worldIn, new BlockPos(i, j, k))) && j > 1) {
                worldIn.setBlockState(new BlockPos(i, j, k), turned, 2);
                --j;
            }
        }
    }

    protected boolean generateChest(World worldIn, StructureBoundingBox structurebb, Random randomIn, int x, int y, int z, ResourceLocation loot) {
        BlockPos pos = new BlockPos(getXWithOffset(x, z), getYWithOffset(y), getZWithOffset(x, z));
        return generateChest(worldIn, structurebb, randomIn, pos, loot, null);
    }

    /** Places a vanilla chest (unless one is there) and gives it the loot table. */
    protected boolean generateChest(World worldIn, StructureBoundingBox boundingBoxIn, Random rand, BlockPos posIn, ResourceLocation resourceLocationIn,
            IBlockState chestState) {
        if (!boundingBoxIn.isVecInside(posIn)) return false;
        var access = worldIn.mcc$access();
        if (access == null) return false;
        var p = new net.minecraft.core.BlockPos(posIn.getX(), posIn.getY(), posIn.getZ());
        if (access.getBlockState(p).is(net.minecraft.world.level.block.Blocks.CHEST)) return false;
        var state = chestState != null ? org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(chestState)
                : net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState();
        worldIn.setBlockState(posIn, chestState != null ? chestState : org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(state), 2);
        var be = access.getBlockEntity(p);
        if (be instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container && resourceLocationIn != null) {
            container.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(resourceLocationIn.getNamespace(), resourceLocationIn.getPath())), rand.nextLong());
        }
        return true;
    }

    protected boolean createDispenser(World worldIn, StructureBoundingBox sbb, Random rand, int x, int y, int z, EnumFacing facing, ResourceLocation lootTableIn) {
        BlockPos pos = new BlockPos(getXWithOffset(x, z), getYWithOffset(y), getZWithOffset(x, z));
        var access = worldIn.mcc$access();
        if (!sbb.isVecInside(pos) || access == null) return false;
        var p = new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ());
        if (access.getBlockState(p).is(net.minecraft.world.level.block.Blocks.DISPENSER)) return false;
        var state = net.minecraft.world.level.block.Blocks.DISPENSER.defaultBlockState();
        if (facing != null) state = state.setValue(net.minecraft.world.level.block.DispenserBlock.FACING, facing.mcc$modern());
        if (rotation != Rotation.NONE) state = state.rotate(modernRotation());
        access.setBlock(p, state, 2);
        var be = access.getBlockEntity(p);
        if (be instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container && lootTableIn != null) {
            container.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(lootTableIn.getNamespace(), lootTableIn.getPath())), rand.nextLong());
        }
        return true;
    }

    private net.minecraft.world.level.block.Rotation modernRotation() {
        return switch (rotation) {
            case CLOCKWISE_90 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
            case CLOCKWISE_180 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
            case COUNTERCLOCKWISE_90 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
            default -> net.minecraft.world.level.block.Rotation.NONE;
        };
    }

    /** Places both halves of a door facing the given way. */
    protected void generateDoor(World worldIn, StructureBoundingBox sbb, Random rand, int x, int y, int z, EnumFacing facing,
            legacyconnector.v1_12_2.net.minecraft.block.Block door) {
        BlockPos pos = new BlockPos(getXWithOffset(x, z), getYWithOffset(y), getZWithOffset(x, z));
        var access = worldIn.mcc$access();
        if (!sbb.isVecInside(pos) || access == null) return;
        var modern = door != null && door.mcc$modern instanceof net.minecraft.world.level.block.DoorBlock d ? d
                : (net.minecraft.world.level.block.DoorBlock) net.minecraft.world.level.block.Blocks.OAK_DOOR;
        var state = modern.defaultBlockState();
        if (facing != null && facing.mcc$modern().getAxis().isHorizontal()) state = state.setValue(net.minecraft.world.level.block.DoorBlock.FACING, facing.mcc$modern());
        if (rotation != Rotation.NONE) state = state.rotate(modernRotation());
        var p = new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ());
        access.setBlock(p, state.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), 2);
        access.setBlock(p.above(), state.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 2);
    }

    public void offset(int x, int y, int z) {
        boundingBox.offset(x, y, z);
    }

    public EnumFacing getCoordBaseMode() {
        return coordBaseMode;
    }

    public void setCoordBaseMode(EnumFacing facing) {
        this.coordBaseMode = facing;
        if (facing == null) {
            rotation = Rotation.NONE;
            mirror = Mirror.NONE;
            return;
        }
        switch (facing.getName()) {
            case "south":
                mirror = Mirror.LEFT_RIGHT;
                rotation = Rotation.NONE;
                break;
            case "west":
                mirror = Mirror.LEFT_RIGHT;
                rotation = Rotation.CLOCKWISE_90;
                break;
            case "east":
                mirror = Mirror.NONE;
                rotation = Rotation.CLOCKWISE_90;
                break;
            default:
                mirror = Mirror.NONE;
                rotation = Rotation.NONE;
        }
    }

    /** Chooses the state of each position of a randomized fill. */
    public abstract static class BlockSelector {
        protected IBlockState blockstate = org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();

        public BlockSelector() {
        }

        public abstract void selectBlocks(Random rand, int x, int y, int z, boolean wall);

        public IBlockState getBlockState() {
            return blockstate;
        }
    }
}
