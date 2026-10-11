package mccompat.v1_7_10.net.minecraft.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.util.ChunkCoordinates;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.mccompat.v1_7_10.entity.LegacyHost;

/**
 * 1.7.10 DataWatcher: typed values by id that the server syncs to clients. Values a mod adds live here; for a legacy
 * entity backed by a host, every change is pushed to the host's single synced compound, which clients read back.
 * The vanilla slots (0 flags, 1 air, 6 health) read and write the modern entity's real state.
 */
public class DataWatcher {
    private final Entity owner;
    private final Map<Integer, WatchableObject> watched = new TreeMap<>();
    private boolean isBlank = true;
    private boolean objectChanged;

    public DataWatcher(Entity owner) {
        this.owner = owner;
    }

    public void addObject(int id, Object value) {
        int type = typeOf(value);
        if (type < 0) throw new IllegalArgumentException("Unknown data type: " + (value == null ? "null" : value.getClass()));
        if (id > 31) throw new IllegalArgumentException("Data value id is too big with " + id + "! (Max is 31)");
        if (watched.containsKey(id)) throw new IllegalArgumentException("Duplicate id value for " + id + "!");
        watched.put(id, new WatchableObject(type, id, value));
        isBlank = false;
    }

    public void addObjectByDataType(int id, int type) {
        WatchableObject o = new WatchableObject(type, id, null);
        watched.put(id, o);
        isBlank = false;
    }

    public byte getWatchableObjectByte(int id) {
        if (id == 0) {
            var m = modern();
            if (m != null) return mcc$flags(m);
        }
        Object v = value(id);
        return v instanceof Number n ? n.byteValue() : 0;
    }

    public short getWatchableObjectShort(int id) {
        if (id == 1) {
            var m = modern();
            if (m != null) return (short) m.getAirSupply();
        }
        Object v = value(id);
        return v instanceof Number n ? n.shortValue() : 0;
    }

    public int getWatchableObjectInt(int id) {
        Object v = value(id);
        return v instanceof Number n ? n.intValue() : 0;
    }

    public float getWatchableObjectFloat(int id) {
        if (id == 6 && modern() instanceof LivingEntity l) return l.getHealth();
        Object v = value(id);
        return v instanceof Number n ? n.floatValue() : 0.0F;
    }

    public String getWatchableObjectString(int id) {
        Object v = value(id);
        return v instanceof String s ? s : "";
    }

    public ItemStack getWatchableObjectItemStack(int id) {
        Object v = value(id);
        return v instanceof ItemStack s ? s : null;
    }

    public ChunkCoordinates mcc$getCoordinates(int id) {
        Object v = value(id);
        return v instanceof ChunkCoordinates c ? c : null;
    }

    public void updateObject(int id, Object value) {
        var m = modern();
        if (m != null) {
            if (id == 0 && value instanceof Number n) {
                mcc$setFlags(m, n.byteValue());
                return;
            }
            if (id == 1 && value instanceof Number n) {
                m.setAirSupply(n.intValue());
                return;
            }
            if (id == 6 && value instanceof Number n && m instanceof LivingEntity l) {
                l.setHealth(n.floatValue());
                return;
            }
        }
        WatchableObject o = watched.get(id);
        if (o == null) {
            return; // never added: vanilla would throw; ignoring keeps the id free for a later addObject
        } else if (java.util.Objects.equals(o.object, value)) {
            return;
        } else {
            o.object = value;
        }
        o.watched = true;
        objectChanged = true;
        mcc$push();
    }

    public void setObjectWatched(int id) {
        WatchableObject o = watched.get(id);
        if (o != null) {
            o.watched = true;
            objectChanged = true;
            mcc$push();
        }
    }

    public boolean hasChanges() {
        return objectChanged;
    }

    public List<WatchableObject> getChanged() {
        List<WatchableObject> out = new ArrayList<>();
        for (WatchableObject o : watched.values()) {
            if (o.watched) {
                o.watched = false;
                out.add(o);
            }
        }
        objectChanged = false;
        return out.isEmpty() ? null : out;
    }

    public List<WatchableObject> getAllWatched() {
        return new ArrayList<>(watched.values());
    }

    public void updateWatchedObjectsFromList(List<WatchableObject> list) {
        if (list == null) return;
        for (WatchableObject o : list) {
            WatchableObject mine = watched.get(o.dataValueId);
            if (mine != null) mine.object = o.object;
        }
    }

    public boolean getIsBlank() {
        return isBlank;
    }

    public void func_111144_e() {
        objectChanged = false;
    }

    // ------------------------------------------------------------------ sync with the host

    private Object value(int id) {
        WatchableObject o = watched.get(id);
        return o == null ? null : o.object;
    }

    private net.minecraft.world.entity.Entity modern() {
        return owner == null ? null : owner.mcc$entity;
    }

    private void mcc$push() {
        if (owner != null && owner.mcc$entity instanceof LegacyHost host && host.mcc$legacy() == owner && !owner.mcc$entity.level().isClientSide()) {
            host.mcc$syncWatcher(mcc$toTag());
        }
    }

    /** Mod-owned values (vanilla slots are synced by the modern entity itself). */
    public CompoundTag mcc$toTag() {
        CompoundTag tag = new CompoundTag();
        for (WatchableObject o : watched.values()) {
            if (o.dataValueId == 0 || o.dataValueId == 1 || o.dataValueId == 6 || o.object == null) continue;
            String key = Integer.toString(o.dataValueId);
            switch (o.objectType) {
                case 0 -> tag.putByte(key, ((Number) o.object).byteValue());
                case 1 -> tag.putShort(key, ((Number) o.object).shortValue());
                case 2 -> tag.putInt(key, ((Number) o.object).intValue());
                case 3 -> tag.putFloat(key, ((Number) o.object).floatValue());
                case 4 -> tag.putString(key, (String) o.object);
                case 5 -> {
                    NBTTagCompound stack = new NBTTagCompound();
                    ((ItemStack) o.object).writeToNBT(stack);
                    tag.put(key, stack.mcc$modern());
                }
                case 6 -> {
                    ChunkCoordinates c = (ChunkCoordinates) o.object;
                    tag.putIntArray(key, new int[] {c.posX, c.posY, c.posZ});
                }
                default -> {
                }
            }
        }
        return tag;
    }

    /** Client: apply values received from the server. */
    public void mcc$fromTag(CompoundTag tag) {
        for (String key : tag.getAllKeys()) {
            int id;
            try {
                id = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            Tag t = tag.get(key);
            Object value = switch (t.getId()) {
                case Tag.TAG_BYTE -> tag.getByte(key);
                case Tag.TAG_SHORT -> tag.getShort(key);
                case Tag.TAG_INT -> tag.getInt(key);
                case Tag.TAG_FLOAT -> tag.getFloat(key);
                case Tag.TAG_STRING -> tag.getString(key);
                case Tag.TAG_COMPOUND -> ItemStack.loadItemStackFromNBT(new NBTTagCompound(tag.getCompound(key)));
                case Tag.TAG_INT_ARRAY -> {
                    int[] a = tag.getIntArray(key);
                    yield a.length == 3 ? new ChunkCoordinates(a[0], a[1], a[2]) : null;
                }
                default -> null;
            };
            if (value == null) continue;
            WatchableObject o = watched.get(id);
            if (o == null) watched.put(id, new WatchableObject(typeOf(value), id, value));
            else o.object = value;
        }
    }

    private static byte mcc$flags(net.minecraft.world.entity.Entity m) {
        int f = 0;
        if (m.isOnFire()) f |= 1;
        if (m.isShiftKeyDown()) f |= 2;
        if (m.isSprinting()) f |= 8;
        if (m instanceof LivingEntity l && l.isUsingItem()) f |= 16;
        if (m.isInvisible()) f |= 32;
        return (byte) f;
    }

    private static void mcc$setFlags(net.minecraft.world.entity.Entity m, byte f) {
        m.setShiftKeyDown((f & 2) != 0);
        m.setSprinting((f & 8) != 0);
        m.setInvisible((f & 32) != 0);
    }

    static int typeOf(Object value) {
        if (value instanceof Byte) return 0;
        if (value instanceof Short) return 1;
        if (value instanceof Integer) return 2;
        if (value instanceof Float) return 3;
        if (value instanceof String) return 4;
        if (value instanceof ItemStack) return 5;
        if (value instanceof ChunkCoordinates) return 6;
        return -1;
    }

    public static class WatchableObject {
        private final int objectType;
        private final int dataValueId;
        private Object object;
        private boolean watched;

        public WatchableObject(int type, int id, Object object) {
            this.objectType = type;
            this.dataValueId = id;
            this.object = object;
            this.watched = true;
        }

        public int getDataValueId() {
            return dataValueId;
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public Object getObject() {
            return object;
        }

        public int getObjectType() {
            return objectType;
        }

        public boolean isWatched() {
            return watched;
        }

        public void setWatched(boolean watched) {
            this.watched = watched;
        }
    }
}
