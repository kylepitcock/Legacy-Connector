package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.*;

/** Server-owned original scalar fields, sent separately from native entity tracking and save callbacks. */
public final class AnimalRenderStateB173 {
    private record Slot(String key,Field field) {}
    private record Schema(List<Slot> slots,String failure) {}
    private static final ClassValue<Schema> SCHEMAS=new ClassValue<>() {
        @Override protected Schema computeValue(Class<?> type) {
            var slots=new ArrayList<Slot>();
            try {
                for(Class<?> c=type;c!=null&&c!=Object.class&&!api(c);c=c.getSuperclass()) {
                    for(Field field:c.getDeclaredFields()) {
                        int modifiers=field.getModifiers();
                        if(Modifier.isStatic(modifiers)||Modifier.isFinal(modifiers)||field.isSynthetic())continue;
                        Class<?> kind=field.getType();
                        if(!kind.isPrimitive()&&kind!=String.class)continue;
                        if(!field.trySetAccessible())return new Schema(List.of(),"inaccessible render field "+c.getName()+"."+field.getName());
                        slots.add(new Slot(c.getName()+"#"+field.getName(),field));
                    }
                }
                slots.sort(java.util.Comparator.comparing(Slot::key));
                return new Schema(List.copyOf(slots),null);
            }catch(RuntimeException|LinkageError error){return new Schema(List.of(),"cannot inspect original render fields of "+type.getName()+": "+error);}
        }
    };
    private AnimalRenderStateB173() {}
    private static boolean api(Class<?> type) {
        String name=type.getName();return name.startsWith("legacyconnector.")||name.startsWith("legacyconnector_synth.");
    }
    /** Null means the complete original scalar schema can be inspected. Reference-valued fields remain local. */
    public static String unsupportedReason(Class<?> type){return SCHEMAS.get(type).failure;}
    private static List<Slot> slots(Object original) {
        Schema schema=SCHEMAS.get(original.getClass());
        if(schema.failure!=null)throw new IllegalStateException(schema.failure);
        return schema.slots;
    }
    public static CompoundTag capture(Object original) {
        var result=new CompoundTag();
        for(Slot slot:slots(original))try{result.put(slot.key,encode(slot.field.getType(),slot.field.get(original)));}
        catch(IllegalAccessException error){throw new IllegalStateException("Cannot capture original render field "+slot.key,error);}
        return result;
    }
    /**
     * Apply only values changed by the server since the previous received snapshot. An initial null snapshot applies all fields.
     * Compare against the received packet, never the mutable client view: render callbacks may deliberately change local fields.
     */
    public static void apply(Object original,CompoundTag previousReceived,CompoundTag incoming) {
        var changes=new ArrayList<java.util.Map.Entry<Slot,Object>>();
        for(Slot slot:slots(original)) {
            Tag next=incoming.get(slot.key);
            if(next==null)throw new IllegalArgumentException("Missing original render field "+slot.key);
            Object value=decode(slot.field.getType(),next,slot.key);
            if(previousReceived==null||!next.equals(previousReceived.get(slot.key)))
                changes.add(new java.util.AbstractMap.SimpleImmutableEntry<>(slot,value));
        }
        for(var change:changes)try{change.getKey().field.set(original,change.getValue());}
        catch(IllegalAccessException error){throw new IllegalStateException("Cannot apply original render field "+change.getKey().key,error);}
    }
    private static Tag encode(Class<?> type,Object value) {
        if(type==String.class)return value==null?new CompoundTag():StringTag.valueOf((String)value);
        if(type==boolean.class)return ByteTag.valueOf((Boolean)value);
        if(type==byte.class)return ByteTag.valueOf((Byte)value);
        if(type==short.class)return ShortTag.valueOf((Short)value);
        if(type==char.class)return IntTag.valueOf((Character)value);
        if(type==int.class)return IntTag.valueOf((Integer)value);
        if(type==long.class)return LongTag.valueOf((Long)value);
        if(type==float.class)return FloatTag.valueOf((Float)value);
        if(type==double.class)return DoubleTag.valueOf((Double)value);
        throw new IllegalArgumentException("Unsupported render field type "+type);
    }
    private static Object decode(Class<?> type,Tag value,String key) {
        if(type==String.class){if(value instanceof StringTag string)return string.getAsString();if(value instanceof CompoundTag compound&&compound.isEmpty())return null;}
        if(type==boolean.class&&value instanceof ByteTag number)return number.getAsByte()!=0;
        if(type==byte.class&&value instanceof ByteTag number)return number.getAsByte();
        if(type==short.class&&value instanceof ShortTag number)return number.getAsShort();
        if(type==char.class&&value instanceof IntTag number&&number.getAsInt()>=0&&number.getAsInt()<=Character.MAX_VALUE)return (char)number.getAsInt();
        if(type==int.class&&value instanceof IntTag number)return number.getAsInt();
        if(type==long.class&&value instanceof LongTag number)return number.getAsLong();
        if(type==float.class&&value instanceof FloatTag number)return number.getAsFloat();
        if(type==double.class&&value instanceof DoubleTag number)return number.getAsDouble();
        throw new IllegalArgumentException("Invalid original render field "+key+" for "+type.getTypeName());
    }
}
