package legacyconnector.vb1_7_3.net.minecraft.entity.data;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Beta 1.7.3 {@code DataTracker} (the entity's synced byte/short/int/string slots, ids 0 to 31). Held per entity; mods store state flags in it
 * ({@code dataTracker.startTracking(16, (byte) 0)}) and read them back. Values are kept locally; the host's own tracking sends what clients need.
 */
public class DataTracker {
    private final Map<Integer, Object> values = new TreeMap<>();
    private final Map<Integer,Class<?>> declaredTypes = new TreeMap<>();
    private final Map<Integer, Boolean> dirtyIds = new TreeMap<>();
    private boolean dirty;

    public void startTracking(int id, Object value) {
        if (id > 31 || id < 0) throw new IllegalArgumentException("Data value id is too big with " + id + "! (Max is 31)");
        if (values.containsKey(id)) throw new IllegalArgumentException("Duplicate id value for " + id + "!");
        if(value==null||type(value.getClass())<0)throw new IllegalArgumentException("Unsupported Beta tracker type");
        declaredTypes.put(id,value.getClass());
        values.put(id, value);
    }

    public byte getByte(int id) { Object v = values.get(id); return v instanceof Number n ? n.byteValue() : 0; }
    public short getShort(int id) { Object v = values.get(id); return v instanceof Number n ? n.shortValue() : 0; }
    public int getInt(int id) { Object v = values.get(id); return v instanceof Number n ? n.intValue() : 0; }
    public float getFloat(int id) { Object v = values.get(id); return v instanceof Number n ? n.floatValue() : 0; }
    public String getString(int id) { Object v = values.get(id); return v == null ? "" : v.toString(); }

    public void set(int id, Object value) {
        Object old = values.get(id);
        if (old == null || !old.equals(value)) {
            values.put(id, value);
            dirtyIds.put(id, Boolean.TRUE);
            dirty = true;
        }
    }

    public boolean isDirty() { return dirty; }

    public ArrayList<Object> getDirtyEntries() {
        ArrayList<Object> out = new ArrayList<>();
        if (dirty) for (int id : dirtyIds.keySet()) out.add(values.get(id));
        dirtyIds.clear();
        dirty = false;
        return out;
    }
    private static final int MAX_STRING=64,MAX_BYTES=16_384;
    private static int type(Class<?> type){
        if(type==Byte.class)return 0;if(type==Short.class)return 1;if(type==Integer.class)return 2;if(type==Float.class)return 3;
        if(type==String.class)return 4;if(type==legacyconnector.vb1_7_3.net.minecraft.item.ItemStack.class)return 5;
        return type==legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3i.class?6:-1;
    }
    private static void bounded(net.minecraft.nbt.CompoundTag tag){
        try(var output=new java.io.DataOutputStream(new java.io.OutputStream(){
            int size;
            @Override public void write(int value)throws java.io.IOException {advance(1);}
            @Override public void write(byte[] bytes,int offset,int length)throws java.io.IOException {advance(length);}
            private void advance(int count)throws java.io.IOException {if(count>MAX_BYTES-size)throw new java.io.IOException("Beta tracker snapshot exceeds bound");size+=count;}
        })){net.minecraft.nbt.NbtIo.write(tag,output);}
        catch(java.io.IOException error){throw new IllegalArgumentException("Cannot encode bounded Beta tracker snapshot",error);}
    }
    /** Original metadata only; native SynchedEntityData is never read or replaced. */
    public net.minecraft.nbt.CompoundTag mcc$snapshot(){
        if(!values.keySet().equals(declaredTypes.keySet())||values.size()>32)throw new IllegalArgumentException("Undeclared Beta tracker state");
        var result=new net.minecraft.nbt.CompoundTag();var entries=new net.minecraft.nbt.ListTag();
        for(var declared:declaredTypes.entrySet()){
            Object value=values.get(declared.getKey());if(value==null||value.getClass()!=declared.getValue())throw new IllegalArgumentException("Beta tracker type changed");
            int type=type(declared.getValue());var entry=new net.minecraft.nbt.CompoundTag();entry.putInt("id",declared.getKey());entry.putByte("type",(byte)type);
            switch(type){
                case 0->entry.putByte("value",(Byte)value);case 1->entry.putShort("value",(Short)value);case 2->entry.putInt("value",(Integer)value);
                case 3->{float number=(Float)value;if(!Float.isFinite(number))throw new IllegalArgumentException("Nonfinite Beta tracker value");entry.putFloat("value",number);}
                case 4->{String text=(String)value;if(text.length()>MAX_STRING)throw new IllegalArgumentException("Beta tracker string exceeds bound");entry.putString("value",text);}
                case 5->{var stack=(legacyconnector.vb1_7_3.net.minecraft.item.ItemStack)value;var item=new net.minecraft.nbt.CompoundTag();item.putInt("id",stack.itemId);item.putInt("count",stack.count);item.putInt("damage",stack.getDamage());validateStack(item);entry.put("value",item);}
                case 6->{var position=(legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3i)value;entry.putIntArray("value",new int[]{position.x,position.y,position.z});}
                default->throw new IllegalArgumentException("Unsupported Beta tracker type");
            }
            entries.add(entry);
        }
        result.put("entries",entries);bounded(result);return result;
    }
    private static void validateStack(net.minecraft.nbt.CompoundTag item){
        if(!item.getAllKeys().equals(java.util.Set.of("id","count","damage"))||!item.contains("id",net.minecraft.nbt.Tag.TAG_INT)||!item.contains("count",net.minecraft.nbt.Tag.TAG_INT)||!item.contains("damage",net.minecraft.nbt.Tag.TAG_INT)
                ||item.getInt("id")<0||item.getInt("id")>=32000||item.getInt("count")<0||item.getInt("count")>127
                ||item.getInt("damage")<Short.MIN_VALUE||item.getInt("damage")>Short.MAX_VALUE)throw new IllegalArgumentException("Invalid Beta tracked stack");
    }
    /** Full validation precedes any mutation; unchanged server values preserve client render choices. */
    public Runnable mcc$prepare(net.minecraft.nbt.CompoundTag previous,net.minecraft.nbt.CompoundTag incoming){
        bounded(incoming);
        if(!incoming.getAllKeys().equals(java.util.Set.of("entries"))||!incoming.contains("entries",net.minecraft.nbt.Tag.TAG_LIST))throw new IllegalArgumentException("Malformed Beta tracker snapshot");
        var raw=(net.minecraft.nbt.ListTag)incoming.get("entries");if(raw.size()>32||!raw.isEmpty()&&raw.getElementType()!=net.minecraft.nbt.Tag.TAG_COMPOUND)throw new IllegalArgumentException("Malformed Beta tracker entries");
        var decoded=new TreeMap<Integer,Object>();var changes=new TreeMap<Integer,Object>();
        var old=new TreeMap<Integer,net.minecraft.nbt.CompoundTag>();
        if(previous!=null)for(var value:previous.getList("entries",net.minecraft.nbt.Tag.TAG_COMPOUND)){var entry=(net.minecraft.nbt.CompoundTag)value;old.put(entry.getInt("id"),entry);}
        for(var value:raw){
            var entry=(net.minecraft.nbt.CompoundTag)value;
            if(!entry.getAllKeys().equals(java.util.Set.of("id","type","value"))||!entry.contains("id",net.minecraft.nbt.Tag.TAG_INT)||!entry.contains("type",net.minecraft.nbt.Tag.TAG_BYTE))throw new IllegalArgumentException("Malformed Beta tracker entry");
            int id=entry.getInt("id");Class<?> expected=declaredTypes.get(id);int kind=entry.getByte("type");
            if(expected==null||kind!=type(expected)||decoded.containsKey(id))throw new IllegalArgumentException("Undeclared or changed Beta tracker schema");
            Object next=switch(kind){
                case 0->{require(entry,net.minecraft.nbt.Tag.TAG_BYTE);yield entry.getByte("value");}
                case 1->{require(entry,net.minecraft.nbt.Tag.TAG_SHORT);yield entry.getShort("value");}
                case 2->{require(entry,net.minecraft.nbt.Tag.TAG_INT);yield entry.getInt("value");}
                case 3->{require(entry,net.minecraft.nbt.Tag.TAG_FLOAT);float number=entry.getFloat("value");if(!Float.isFinite(number))throw new IllegalArgumentException("Nonfinite tracker value");yield number;}
                case 4->{require(entry,net.minecraft.nbt.Tag.TAG_STRING);String text=entry.getString("value");if(text.length()>MAX_STRING)throw new IllegalArgumentException("Tracker string exceeds bound");yield text;}
                case 5->{require(entry,net.minecraft.nbt.Tag.TAG_COMPOUND);var item=entry.getCompound("value");validateStack(item);yield new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(item.getInt("id"),item.getInt("count"),item.getInt("damage"));}
                case 6->{require(entry,net.minecraft.nbt.Tag.TAG_INT_ARRAY);int[] position=entry.getIntArray("value");if(position.length!=3)throw new IllegalArgumentException("Malformed tracked position");yield new legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3i(position[0],position[1],position[2]);}
                default->throw new IllegalArgumentException("Unsupported Beta tracker type");
            };
            decoded.put(id,next);if(!entry.equals(old.get(id)))changes.put(id,next);
        }
        if(!decoded.keySet().equals(declaredTypes.keySet()))throw new IllegalArgumentException("Missing Beta tracker declaration");
        return ()->values.putAll(changes);
    }
    private static void require(net.minecraft.nbt.CompoundTag entry,int type){if(!entry.contains("value",type))throw new IllegalArgumentException("Beta tracker value type mismatch");}

}
