package legacyconnector.v1_12_2.net.minecraft.nbt;

import java.util.UUID;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** NBT forms of block states, UUIDs and positions, in the 1.12.2 layout. */
public final class NBTUtil {
    private NBTUtil() {}

    public static NBTTagCompound createUUIDTag(UUID uuid) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("M", uuid.getMostSignificantBits());
        tag.setLong("L", uuid.getLeastSignificantBits());
        return tag;
    }

    public static UUID getUUIDFromTag(NBTTagCompound tag) {
        return new UUID(tag.getLong("M"), tag.getLong("L"));
    }

    public static com.mojang.authlib.GameProfile readGameProfileFromNBT(NBTTagCompound compound) {
        String name = compound.hasKey("Name", 8) ? compound.getString("Name") : null;
        UUID uuid = null;
        if (compound.hasKey("Id", 8)) {
            try {
                uuid = UUID.fromString(compound.getString("Id"));
            } catch (Throwable t) {
                // a malformed id reads as none
            }
        }
        if (name == null && uuid == null) return null;
        try {
            com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(
                    uuid == null ? net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(name) : uuid, name == null ? "" : name);
            if (compound.hasKey("Properties", 10)) {
                NBTTagCompound props = compound.getCompoundTag("Properties");
                for (String key : props.getKeySet()) {
                    NBTTagList list = props.getTagList(key, 10);
                    for (int i = 0; i < list.tagCount(); i++) {
                        NBTTagCompound p = list.getCompoundTagAt(i);
                        String value = p.getString("Value");
                        profile.getProperties().put(key, p.hasKey("Signature", 8)
                                ? new com.mojang.authlib.properties.Property(key, value, p.getString("Signature"))
                                : new com.mojang.authlib.properties.Property(key, value));
                    }
                }
            }
            return profile;
        } catch (Throwable t) {
            return null;
        }
    }

    public static NBTTagCompound writeGameProfile(NBTTagCompound tagCompound, com.mojang.authlib.GameProfile profile) {
        if (profile.getName() != null && !profile.getName().isEmpty()) tagCompound.setString("Name", profile.getName());
        if (profile.getId() != null) tagCompound.setString("Id", profile.getId().toString());
        if (!profile.getProperties().isEmpty()) {
            NBTTagCompound props = new NBTTagCompound();
            for (String key : profile.getProperties().keySet()) {
                NBTTagList list = new NBTTagList();
                for (com.mojang.authlib.properties.Property property : profile.getProperties().get(key)) {
                    NBTTagCompound p = new NBTTagCompound();
                    p.setString("Value", property.value());
                    if (property.signature() != null) p.setString("Signature", property.signature());
                    list.appendTag(p);
                }
                props.setTag(key, list);
            }
            tagCompound.setTag("Properties", props);
        }
        return tagCompound;
    }

    /**
     * Whether {@code compare} holds everything {@code reference} holds (extra keys allowed); with {@code compareTagList} a
     * reference list matches when each of its entries matches some entry of the other list.
     */
    public static boolean areNBTEquals(NBTBase reference, NBTBase compare, boolean compareTagList) {
        return net.minecraft.nbt.NbtUtils.compareNbt(reference == null ? null : reference.mcc$modern(), compare == null ? null : compare.mcc$modern(), compareTagList);
    }

    public static NBTTagCompound createPosTag(BlockPos pos) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("X", pos.getX());
        tag.setInteger("Y", pos.getY());
        tag.setInteger("Z", pos.getZ());
        return tag;
    }

    public static BlockPos getPosFromTag(NBTTagCompound tag) {
        return new BlockPos(tag.getInteger("X"), tag.getInteger("Y"), tag.getInteger("Z"));
    }

    public static IBlockState readBlockState(NBTTagCompound tag) {
        if (!tag.hasKey("Name", 8)) return org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
        Block block = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.BLOCKS.getValue(new ResourceLocation(tag.getString("Name")));
        if (block == null) return org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122.airState();
        IBlockState state = block.getDefaultState();
        if (tag.hasKey("Properties", 10)) {
            NBTTagCompound props = tag.getCompoundTag("Properties");
            for (IProperty<?> property : state.getPropertyKeys()) {
                if (props.hasKey(property.getName())) state = withValue(state, property, props.getString(property.getName()));
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> IBlockState withValue(IBlockState state, IProperty<T> property, String value) {
        com.google.common.base.Optional<T> parsed = property.parseValue(value);
        return parsed.isPresent() ? state.withProperty(property, parsed.get()) : state;
    }

    public static NBTTagCompound writeBlockState(NBTTagCompound tag, IBlockState state) {
        tag.setString("Name", String.valueOf(state.getBlock().getRegistryName()));
        if (!state.getPropertyKeys().isEmpty()) {
            NBTTagCompound props = new NBTTagCompound();
            for (IProperty<?> property : state.getPropertyKeys()) props.setString(property.getName(), valueName(state, property));
            tag.setTag("Properties", props);
        }
        return tag;
    }

    private static <T extends Comparable<T>> String valueName(IBlockState state, IProperty<T> property) {
        return property.getName(state.getValue(property));
    }
}
