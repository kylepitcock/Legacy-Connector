package legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.gen.structure.StructureBoundingBox;

/**
 * A 1.12.2 structure template (the {@code .nbt} format of structure blocks). Vanilla blocks of the palette are also upgraded to their
 * modern states by the game's data fixer, so stairs, logs and slabs keep their properties when placed; mod blocks are read through
 * their own state containers. Placement mirrors, rotates, applies the processor and integrity, and fills tile entities from the
 * template's data. Entities in templates are not spawned.
 */
public class Template {
    private final List<BlockInfo> blocks = new ArrayList<>();
    private final List<NBTTagCompound> entities = new ArrayList<>();
    private BlockPos size = BlockPos.ORIGIN;
    private String author = "?";

    public Template() {
    }

    public BlockPos getSize() {
        return size;
    }

    public void setAuthor(String authorIn) {
        this.author = authorIn;
    }

    public String getAuthor() {
        return author;
    }

    /** Copies a region of the world into the template (blocks and tile entity data; air is skipped when asked). */
    public void takeBlocksFromWorld(World worldIn, BlockPos startPos, BlockPos endPos, boolean takeEntities, Block toIgnore) {
        if (endPos.getX() < 1 || endPos.getY() < 1 || endPos.getZ() < 1) return;
        BlockPos end = startPos.add(endPos).add(-1, -1, -1);
        blocks.clear();
        for (BlockPos.MutableBlockPos p : BlockPos.getAllInBoxMutable(startPos, end)) {
            BlockPos rel = p.subtract(startPos);
            IBlockState state = worldIn.getBlockState(p);
            if (toIgnore != null && toIgnore == state.getBlock()) continue;
            TileEntity te = worldIn.getTileEntity(p);
            NBTTagCompound tag = null;
            if (te != null) {
                tag = te.writeToNBT(new NBTTagCompound());
                tag.removeTag("x");
                tag.removeTag("y");
                tag.removeTag("z");
            }
            var modern = worldIn.mcc$access() == null ? null : worldIn.mcc$access().getBlockState(new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()));
            BlockInfo info = new BlockInfo(rel, state, tag);
            info.mcc$modern = modern;
            blocks.add(info);
        }
        size = endPos;
    }

    /** Positions of the template's data structure blocks (their custom name), transformed to the placement. */
    public Map<BlockPos, String> getDataBlocks(BlockPos pos, PlacementSettings placementIn) {
        Map<BlockPos, String> out = new HashMap<>();
        StructureBoundingBox box = placementIn.getBoundingBox();
        for (BlockInfo info : blocks) {
            boolean structureBlock = info.mcc$modern != null ? info.mcc$modern.is(net.minecraft.world.level.block.Blocks.STRUCTURE_BLOCK)
                    : info.blockState != null && info.blockState.getBlock() != null && "structure_block".equals(path(info.blockState.getBlock()));
            if (!structureBlock || info.tileentityData == null) continue;
            String mode = info.tileentityData.getString("mode");
            if (!"DATA".equals(mode)) continue;
            BlockPos at = transformedBlockPos(placementIn, info.pos).add(pos);
            if (box == null || box.isVecInside(at)) out.put(at, info.tileentityData.getString("metadata"));
        }
        return out;
    }

    private static String path(Block block) {
        var name = block.getRegistryName();
        return name == null ? "" : name.getPath();
    }

    public BlockPos calculateConnectedPos(PlacementSettings placementIn, BlockPos p_186262_2_, PlacementSettings p_186262_3_, BlockPos p_186262_4_) {
        BlockPos a = transformedBlockPos(placementIn, p_186262_2_);
        BlockPos b = transformedBlockPos(p_186262_3_, p_186262_4_);
        return a.subtract(b);
    }

    public static BlockPos transformedBlockPos(PlacementSettings placementIn, BlockPos pos) {
        return getTransformedPos(pos, placementIn.getMirror(), placementIn.getRotation());
    }

    public void addBlocksToWorldChunk(World worldIn, BlockPos pos, PlacementSettings placementIn) {
        placementIn.setBoundingBox(null);
        addBlocksToWorld(worldIn, pos, placementIn);
    }

    public void addBlocksToWorld(World worldIn, BlockPos pos, PlacementSettings placementIn) {
        addBlocksToWorld(worldIn, pos, new BlockRotationProcessor(pos, placementIn), placementIn, 2);
    }

    public void addBlocksToWorld(World worldIn, BlockPos pos, PlacementSettings placementIn, int flags) {
        addBlocksToWorld(worldIn, pos, new BlockRotationProcessor(pos, placementIn), placementIn, flags);
    }

    public void addBlocksToWorld(World worldIn, BlockPos pos, ITemplateProcessor templateProcessor, PlacementSettings placementIn, int flags) {
        if (blocks.isEmpty() || size.getX() < 1 || size.getY() < 1 || size.getZ() < 1) return;
        Block replaced = placementIn.getReplacedBlock();
        StructureBoundingBox box = placementIn.getBoundingBox();
        var access = worldIn.mcc$access();
        for (BlockInfo info : blocks) {
            BlockPos at = transformedBlockPos(placementIn, info.pos).add(pos);
            BlockInfo processed = templateProcessor != null ? templateProcessor.processBlock(worldIn, at, info) : info;
            if (processed == null) continue;
            Block block = processed.blockState == null ? null : processed.blockState.getBlock();
            if (replaced != null && replaced == block) continue;
            if (placementIn.getIgnoreStructureBlock() && processed.mcc$modern != null
                    && processed.mcc$modern.is(net.minecraft.world.level.block.Blocks.STRUCTURE_BLOCK)) continue;
            if (box != null && !box.isVecInside(at)) continue;
            boolean placed;
            if (processed.mcc$modern != null && processed.blockState == info.blockState && access != null) {
                // the vanilla state, turned like the placement, straight into the modern world
                var state = processed.mcc$modern.mirror(modernMirror(placementIn.getMirror())).rotate(modernRotation(placementIn.getRotation()));
                placed = worldIn.mcc$setModernState(at, state, flags);
            } else {
                IBlockState state = turned(processed.blockState, placementIn);
                placed = state != null && worldIn.setBlockState(at, state, flags);
            }
            if (placed && processed.tileentityData != null) fillTileEntity(worldIn, at, processed.tileentityData, placementIn);
        }
    }

    private static void fillTileEntity(World world, BlockPos at, NBTTagCompound data, PlacementSettings settings) {
        TileEntity te = world.getTileEntity(at);
        if (te != null) {
            NBTTagCompound tag = data.copy();
            tag.setInteger("x", at.getX());
            tag.setInteger("y", at.getY());
            tag.setInteger("z", at.getZ());
            try {
                te.readFromNBT(tag);
                dispatch(te, "mirror", "(Llegacyconnector/v1_12_2/net/minecraft/util/Mirror;)V", settings.getMirror());
                dispatch(te, "rotate", "(Llegacyconnector/v1_12_2/net/minecraft/util/Rotation;)V", settings.getRotation());
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Structures").debug("Template tile entity data at {} failed: {}", at, e.toString());
            }
            return;
        }
        // a vanilla container: its loot table (item lists are in the 1.12 format)
        if (data.hasKey("LootTable") && world.mcc$access() != null) {
            var be = world.mcc$access().getBlockEntity(new net.minecraft.core.BlockPos(at.getX(), at.getY(), at.getZ()));
            if (be instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity c) {
                var id = net.minecraft.resources.ResourceLocation.tryParse(data.getString("LootTable"));
                if (id != null) c.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, id), data.getLong("LootTableSeed"));
            }
        }
    }

    private static void dispatch(Object target, String name, String desc, Object arg) {
        try {
            org.kylepitcock.legacyconnector.core.synth.Synth.dispatch(target, name, desc, new Object[] {arg});
        } catch (Throwable e) {
            // the tile entity keeps its orientation
        }
    }

    private static final String STATE = "Llegacyconnector/v1_12_2/net/minecraft/block/state/IBlockState;";

    private static IBlockState turned(IBlockState state, PlacementSettings settings) {
        if (state == null) return null;
        if (settings.getMirror() != Mirror.NONE) state = transform(state, "withMirror", "Llegacyconnector/v1_12_2/net/minecraft/util/Mirror;", settings.getMirror());
        if (settings.getRotation() != Rotation.NONE) state = transform(state, "withRotation", "Llegacyconnector/v1_12_2/net/minecraft/util/Rotation;", settings.getRotation());
        return state;
    }

    private static IBlockState transform(IBlockState state, String name, String argDesc, Object arg) {
        try {
            Object own = org.kylepitcock.legacyconnector.core.synth.Synth.dispatch(state.getBlock(), name, "(" + STATE + argDesc + ")" + STATE, new Object[] {state, arg});
            if (own instanceof IBlockState s) return s;
        } catch (Throwable e) {
            // unchanged
        }
        return state;
    }

    static net.minecraft.world.level.block.Rotation modernRotation(Rotation r) {
        if (r == null) return net.minecraft.world.level.block.Rotation.NONE;
        return switch (r) {
            case CLOCKWISE_90 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
            case CLOCKWISE_180 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
            case COUNTERCLOCKWISE_90 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
            default -> net.minecraft.world.level.block.Rotation.NONE;
        };
    }

    static net.minecraft.world.level.block.Mirror modernMirror(Mirror m) {
        if (m == null) return net.minecraft.world.level.block.Mirror.NONE;
        return switch (m) {
            case LEFT_RIGHT -> net.minecraft.world.level.block.Mirror.LEFT_RIGHT;
            case FRONT_BACK -> net.minecraft.world.level.block.Mirror.FRONT_BACK;
            default -> net.minecraft.world.level.block.Mirror.NONE;
        };
    }

    public BlockPos transformedSize(Rotation rotationIn) {
        switch (rotationIn) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                return new BlockPos(size.getZ(), size.getY(), size.getX());
            default:
                return size;
        }
    }

    private static BlockPos getTransformedPos(BlockPos pos, Mirror mirrorIn, Rotation rotationIn) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        boolean flag = true;
        switch (mirrorIn) {
            case LEFT_RIGHT:
                k = -k;
                break;
            case FRONT_BACK:
                i = -i;
                break;
            default:
                flag = false;
        }
        switch (rotationIn) {
            case COUNTERCLOCKWISE_90:
                return new BlockPos(k, j, -i);
            case CLOCKWISE_90:
                return new BlockPos(-k, j, i);
            case CLOCKWISE_180:
                return new BlockPos(-i, j, -k);
            default:
                return flag ? new BlockPos(i, j, k) : pos;
        }
    }

    public BlockPos getZeroPositionWithTransform(BlockPos p_189961_1_, Mirror p_189961_2_, Rotation p_189961_3_) {
        return getZeroPositionWithTransform(p_189961_1_, p_189961_2_, p_189961_3_, getSize().getX(), getSize().getZ());
    }

    public static BlockPos getZeroPositionWithTransform(BlockPos p_191157_0_, Mirror p_191157_1_, Rotation p_191157_2_, int p_191157_3_, int p_191157_4_) {
        --p_191157_3_;
        --p_191157_4_;
        int i = p_191157_1_ == Mirror.FRONT_BACK ? p_191157_3_ : 0;
        int j = p_191157_1_ == Mirror.LEFT_RIGHT ? p_191157_4_ : 0;
        BlockPos blockpos = p_191157_0_;
        switch (p_191157_2_) {
            case COUNTERCLOCKWISE_90:
                blockpos = p_191157_0_.add(j, 0, p_191157_3_ - i);
                break;
            case CLOCKWISE_90:
                blockpos = p_191157_0_.add(p_191157_4_ - j, 0, i);
                break;
            case CLOCKWISE_180:
                blockpos = p_191157_0_.add(p_191157_3_ - i, 0, p_191157_4_ - j);
                break;
            case NONE:
                blockpos = p_191157_0_.add(i, 0, j);
                break;
            default:
        }
        return blockpos;
    }

    // ------------------------------------------------------------------ NBT

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        List<IBlockState> palette = new ArrayList<>();
        NBTTagList blockList = new NBTTagList();
        for (BlockInfo info : blocks) {
            int idx = palette.indexOf(info.blockState);
            if (idx < 0) {
                idx = palette.size();
                palette.add(info.blockState);
            }
            NBTTagCompound b = new NBTTagCompound();
            b.setTag("pos", intList(info.pos.getX(), info.pos.getY(), info.pos.getZ()));
            b.setInteger("state", idx);
            if (info.tileentityData != null) b.setTag("nbt", info.tileentityData);
            blockList.appendTag(b);
        }
        NBTTagList paletteList = new NBTTagList();
        for (IBlockState state : palette) {
            NBTTagCompound s = new NBTTagCompound();
            var name = state == null || state.getBlock() == null ? null : state.getBlock().getRegistryName();
            s.setString("Name", name == null ? "minecraft:air" : name.toString());
            if (state != null && !state.getProperties().isEmpty()) {
                NBTTagCompound props = new NBTTagCompound();
                for (var e : state.getProperties().entrySet()) props.setString(e.getKey().getName(), nameOf(e.getKey(), e.getValue()));
                s.setTag("Properties", props);
            }
            paletteList.appendTag(s);
        }
        nbt.setTag("palette", paletteList);
        nbt.setTag("blocks", blockList);
        NBTTagList entityList = new NBTTagList();
        for (NBTTagCompound e : entities) entityList.appendTag(e);
        nbt.setTag("entities", entityList);
        nbt.setTag("size", intList(size.getX(), size.getY(), size.getZ()));
        nbt.setString("author", author);
        nbt.setInteger("DataVersion", 1343);
        return nbt;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String nameOf(IProperty property, Comparable value) {
        return property.getName(value);
    }

    private static NBTTagList intList(int... values) {
        NBTTagList list = new NBTTagList();
        for (int v : values) list.appendTag(new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagInt(v));
        return list;
    }

    public void read(NBTTagCompound compound) {
        blocks.clear();
        entities.clear();
        NBTTagList sizeList = compound.getTagList("size", 3);
        size = sizeList.tagCount() >= 3 ? new BlockPos(sizeList.mcc$modern().getInt(0), sizeList.mcc$modern().getInt(1), sizeList.mcc$modern().getInt(2)) : BlockPos.ORIGIN;
        author = compound.getString("author");
        NBTTagList palette = compound.getTagList("palette", 10);
        List<IBlockState> legacyStates = new ArrayList<>();
        for (int i = 0; i < palette.tagCount(); i++) legacyStates.add(legacyState(palette.getCompoundTagAt(i)));
        List<net.minecraft.world.level.block.state.BlockState> modernStates = modernPalette(compound, palette.tagCount());
        NBTTagList blockList = compound.getTagList("blocks", 10);
        for (int i = 0; i < blockList.tagCount(); i++) {
            NBTTagCompound b = blockList.getCompoundTagAt(i);
            NBTTagList p = b.getTagList("pos", 3);
            BlockPos pos = new BlockPos(p.mcc$modern().getInt(0), p.mcc$modern().getInt(1), p.mcc$modern().getInt(2));
            int idx = b.getInteger("state");
            IBlockState state = idx >= 0 && idx < legacyStates.size() ? legacyStates.get(idx) : null;
            NBTTagCompound nbt = b.hasKey("nbt") ? b.getCompoundTag("nbt") : null;
            BlockInfo info = new BlockInfo(pos, state, nbt);
            String name = idx >= 0 && idx < palette.tagCount() ? palette.getCompoundTagAt(idx).getString("Name") : "";
            if (modernStates != null && idx >= 0 && idx < modernStates.size() && (name.startsWith("minecraft:") || name.indexOf(':') < 0)) {
                info.mcc$modern = modernStates.get(idx);
            }
            if (state == null && info.mcc$modern == null) continue;
            if (state == null) info.blockState = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(info.mcc$modern);
            if (info.blockState == null && info.mcc$modern == null) continue;
            blocks.add(info);
        }
        NBTTagList entityList = compound.getTagList("entities", 10);
        for (int i = 0; i < entityList.tagCount(); i++) entities.add(entityList.getCompoundTagAt(i));
    }

    /** A palette entry as a legacy state: the block by name, with the properties its state container knows. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IBlockState legacyState(NBTTagCompound entry) {
        String name = entry.getString("Name");
        if (name.isEmpty()) return null;
        Block block = Block.getBlockFromName(name);
        if (block == null) return null;
        IBlockState state = block.getDefaultState();
        if (state == null || !entry.hasKey("Properties")) return state;
        NBTTagCompound props = entry.getCompoundTag("Properties");
        var container = block.getBlockState();
        for (String key : props.getKeySet()) {
            IProperty property = container == null ? null : container.getProperty(key);
            if (property == null) continue;
            com.google.common.base.Optional value = property.parseValue(props.getString(key));
            if (value.isPresent()) {
                try {
                    state = state.withProperty(property, (Comparable) value.get());
                } catch (RuntimeException e) {
                    // keep the state without it
                }
            }
        }
        return state;
    }

    /** The palette upgraded by the modern data fixer (1.12.2 is data version 1343), or null when that fails. */
    private static List<net.minecraft.world.level.block.state.BlockState> modernPalette(NBTTagCompound compound, int count) {
        try {
            net.minecraft.nbt.CompoundTag tag = compound.mcc$modern().copy();
            if (!tag.contains("DataVersion")) tag.putInt("DataVersion", 1343);
            int version = tag.getInt("DataVersion");
            var fixed = net.minecraft.util.datafix.DataFixTypes.STRUCTURE.updateToCurrentVersion(net.minecraft.util.datafix.DataFixers.getDataFixer(), tag, version);
            var list = fixed.getList("palette", 10);
            if (list.size() != count) return null;
            List<net.minecraft.world.level.block.state.BlockState> out = new ArrayList<>();
            var blocks = net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup();
            for (int i = 0; i < list.size(); i++) out.add(net.minecraft.nbt.NbtUtils.readBlockState(blocks, list.getCompound(i)));
            return out;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** One block of a template. */
    public static class BlockInfo {
        public final BlockPos pos;
        public IBlockState blockState;
        public final NBTTagCompound tileentityData;
        /** The modern state of a vanilla palette entry (keeps the properties legacy vanilla wrappers do not model). */
        public net.minecraft.world.level.block.state.BlockState mcc$modern;

        public BlockInfo(BlockPos posIn, IBlockState stateIn, NBTTagCompound compoundIn) {
            this.pos = posIn;
            this.blockState = stateIn;
            this.tileentityData = compoundIn;
        }
    }

    /** Vanilla's default processor: integrity (random removal) and the replaced block. */
    public static class BlockRotationProcessor implements ITemplateProcessor {
        private final float chance;
        private final Random random;

        public BlockRotationProcessor(BlockPos pos, PlacementSettings settings) {
            this.chance = settings.getIntegrity();
            this.random = settings.getRandom(pos);
        }

        @Override
        public BlockInfo processBlock(World worldIn, BlockPos pos, BlockInfo blockInfoIn) {
            return chance < 1.0F && random.nextFloat() > chance ? null : blockInfoIn;
        }
    }

    /** The name of a template file inside a mod jar ({@code assets/<ns>/structures/<path>.nbt}). */
    static String resourcePath(ResourceLocation id) {
        return "/assets/" + id.getNamespace() + "/structures/" + id.getPath() + ".nbt";
    }

    /** An entity saved in a template: exact position, block position and its NBT. */
    public static class EntityInfo {
        public final legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d pos;
        public final BlockPos blockPos;
        public final NBTTagCompound entityData;

        public EntityInfo(legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d vecIn, BlockPos posIn, NBTTagCompound compoundIn) {
            this.pos = vecIn;
            this.blockPos = posIn;
            this.entityData = compoundIn;
        }
    }
}
