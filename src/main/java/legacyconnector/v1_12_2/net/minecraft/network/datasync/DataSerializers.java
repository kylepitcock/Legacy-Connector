package legacyconnector.v1_12_2.net.minecraft.network.datasync;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** The serializers of synchronized entity data, numbered in registration order (vanilla's first, then mods'). */
public class DataSerializers {
    private static final List<DataSerializer<?>> REGISTRY = new ArrayList<>();
    private static final Map<DataSerializer<?>, Integer> IDS = new IdentityHashMap<>();

    /** A serializer from its write and read functions; values are immutable unless {@code copy} says otherwise. */
    private interface Writer<T> {
        void write(PacketBuffer buf, T value);
    }

    private interface Reader<T> {
        T read(PacketBuffer buf) throws IOException;
    }

    private static <T> DataSerializer<T> of(Writer<T> writer, Reader<T> reader, java.util.function.UnaryOperator<T> copy) {
        return new DataSerializer<T>() {
            @Override
            public void write(PacketBuffer buf, T value) {
                writer.write(buf, value);
            }

            @Override
            public T read(PacketBuffer buf) throws IOException {
                return reader.read(buf);
            }

            @Override
            public DataParameter<T> createKey(int id) {
                return new DataParameter<>(id, this);
            }

            @Override
            public T copyValue(T value) {
                return copy.apply(value);
            }
        };
    }

    public static final DataSerializer<Byte> BYTE = of((b, v) -> b.writeByte(v), PacketBuffer::readByte, v -> v);
    public static final DataSerializer<Integer> VARINT = of(PacketBuffer::writeVarInt, PacketBuffer::readVarInt, v -> v);
    public static final DataSerializer<Float> FLOAT = of((b, v) -> b.writeFloat(v), PacketBuffer::readFloat, v -> v);
    public static final DataSerializer<String> STRING = of(PacketBuffer::writeString, b -> b.readString(32767), v -> v);
    public static final DataSerializer<ITextComponent> TEXT_COMPONENT = of((b, v) -> b.writeString(v.getUnformattedText()),
            b -> new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(b.readString(32767)), ITextComponent::createCopy);
    public static final DataSerializer<ItemStack> ITEM_STACK = of(PacketBuffer::writeItemStack, PacketBuffer::readItemStack, ItemStack::copy);
    public static final DataSerializer<Optional<IBlockState>> OPTIONAL_BLOCK_STATE = of(
            (b, v) -> b.writeVarInt(v.isPresent() ? Block.getStateId(v.get()) : 0),
            b -> {
                int id = b.readVarInt();
                return id == 0 ? Optional.absent() : Optional.of(Block.getStateById(id));
            }, v -> v);
    public static final DataSerializer<Boolean> BOOLEAN = of((b, v) -> b.writeBoolean(v), PacketBuffer::readBoolean, v -> v);
    /** Armor stand rotations (a synthesized type here): written as three floats by the value's own fields when present. */
    public static final DataSerializer<Object> ROTATIONS = of((b, v) -> {
    }, b -> null, v -> v);
    public static final DataSerializer<BlockPos> BLOCK_POS = of(PacketBuffer::writeBlockPos, PacketBuffer::readBlockPos, v -> v);
    public static final DataSerializer<Optional<BlockPos>> OPTIONAL_BLOCK_POS = of((b, v) -> {
        b.writeBoolean(v.isPresent());
        if (v.isPresent()) b.writeBlockPos(v.get());
    }, b -> b.readBoolean() ? Optional.of(b.readBlockPos()) : Optional.absent(), v -> v);
    public static final DataSerializer<EnumFacing> FACING = of(PacketBuffer::writeEnumValue, b -> b.readEnumValue(EnumFacing.class), v -> v);
    public static final DataSerializer<Optional<UUID>> OPTIONAL_UNIQUE_ID = of((b, v) -> {
        b.writeBoolean(v.isPresent());
        if (v.isPresent()) b.writeUniqueId(v.get());
    }, b -> b.readBoolean() ? Optional.of(b.readUniqueId()) : Optional.absent(), v -> v);
    public static final DataSerializer<NBTTagCompound> COMPOUND_TAG = of(PacketBuffer::writeCompoundTag, PacketBuffer::readCompoundTag, NBTTagCompound::copy);

    static {
        for (DataSerializer<?> s : new DataSerializer<?>[] {BYTE, VARINT, FLOAT, STRING, TEXT_COMPONENT, ITEM_STACK, BOOLEAN, ROTATIONS, BLOCK_POS,
                OPTIONAL_BLOCK_POS, FACING, OPTIONAL_UNIQUE_ID, OPTIONAL_BLOCK_STATE, COMPOUND_TAG}) {
            registerSerializer(s);
        }
    }

    public static synchronized void registerSerializer(DataSerializer<?> serializer) {
        if (serializer == null || IDS.containsKey(serializer)) return;
        IDS.put(serializer, REGISTRY.size());
        REGISTRY.add(serializer);
    }

    public static synchronized DataSerializer<?> getSerializer(int id) {
        return id >= 0 && id < REGISTRY.size() ? REGISTRY.get(id) : null;
    }

    public static synchronized int getSerializerId(DataSerializer<?> serializer) {
        Integer id = IDS.get(serializer);
        return id == null ? -1 : id;
    }
}
