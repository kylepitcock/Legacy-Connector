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

    // ---- 1.12.2 game profiles and comparisons

    /** A game profile from its tag (Name, Id and Properties), or null when it has neither name nor id. */
    public static com.mojang.authlib.GameProfile readGameProfileFromNBT(NBTTagCompound compound) {
        String name = compound.hasKey("Name", 8) ? compound.getString("Name") : null;
        String id = compound.hasKey("Id", 8) ? compound.getString("Id") : null;
        try {
            UUID uuid;
            try {
                uuid = UUID.fromString(id);
            } catch (Throwable t) {
                uuid = null;
            }
            if (uuid == null && name == null) return null;
            // modern profiles need both: an offline id as the server derives it, an empty name
            com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(
                    uuid != null ? uuid : net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(name), name == null ? "" : name);
            if (compound.hasKey("Properties", 10)) {
                NBTTagCompound properties = compound.getCompoundTag("Properties");
                for (String key : properties.getKeySet()) {
                    NBTTagList list = properties.getTagList(key, 10);
                    for (int i = 0; i < list.tagCount(); i++) {
                        NBTTagCompound p = list.getCompoundTagAt(i);
                        String value = p.getString("Value");
                        if (p.hasKey("Signature", 8)) {
                            profile.getProperties().put(key, new com.mojang.authlib.properties.Property(key, value, p.getString("Signature")));
                        } else {
                            profile.getProperties().put(key, new com.mojang.authlib.properties.Property(key, value));
                        }
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
            NBTTagCompound properties = new NBTTagCompound();
            for (String key : profile.getProperties().keySet()) {
                NBTTagList list = new NBTTagList();
                for (com.mojang.authlib.properties.Property property : profile.getProperties().get(key)) {
                    NBTTagCompound p = new NBTTagCompound();
                    p.setString("Value", property.value());
                    if (property.hasSignature()) p.setString("Signature", property.signature());
                    list.appendTag(p);
                }
                properties.setTag(key, list);
            }
            tagCompound.setTag("Properties", properties);
        }
        return tagCompound;
    }

    /**
     * Whether {@code compare} holds everything {@code reference} has (extra keys allowed); with {@code compareTagList} a list matches
     * when each of the reference's elements matches some element of the other list.
     */
    public static boolean areNBTEquals(NBTBase reference, NBTBase compare, boolean compareTagList) {
        if (reference == compare) return true;
        if (reference == null) return true;
        if (compare == null) return false;
        if (!reference.getClass().equals(compare.getClass())) return false;
        if (reference instanceof NBTTagCompound ref) {
            NBTTagCompound cmp = (NBTTagCompound) compare;
            for (String key : ref.getKeySet()) {
                if (!areNBTEquals(ref.getTag(key), cmp.getTag(key), compareTagList)) return false;
            }
            return true;
        }
        if (reference instanceof NBTTagList ref && compareTagList) {
            NBTTagList cmp = (NBTTagList) compare;
            if (ref.tagCount() == 0) return cmp.tagCount() == 0;
            for (int i = 0; i < ref.tagCount(); i++) {
                NBTBase a = ref.get(i);
                boolean found = false;
                for (int j = 0; j < cmp.tagCount(); j++) {
                    if (areNBTEquals(a, cmp.get(j), compareTagList)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
        return reference.equals(compare);
    }
}
