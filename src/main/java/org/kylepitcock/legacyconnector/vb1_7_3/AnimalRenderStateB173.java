package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.*;

/** Server-owned original scalars and typed entity references, separate from native canonical tracking and saves. */
public final class AnimalRenderStateB173 {
    private record Slot(String key,Field field) {}
    private record Schema(List<Slot> slots,String failure) {}
    private static final ClassValue<Schema> SCHEMAS=new ClassValue<>() {
        @Override protected Schema computeValue(Class<?> type) {
            var slots=new ArrayList<Slot>();
            try {
                for(Class<?> c=type;c!=null&&c!=Object.class&&!nativeBoundary(c);c=c.getSuperclass()) {
                    for(Field field:c.getDeclaredFields()) {
                        int modifiers=field.getModifiers();
                        if(Modifier.isStatic(modifiers)||Modifier.isFinal(modifiers)||field.isSynthetic())continue;
                        Class<?> kind=field.getType();
                        if(!kind.isPrimitive()&&kind!=String.class&&!(entityReference(kind)&&!api(c))&&!(kind.isEnum()&&!api(kind)))continue;
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
    private static boolean nativeBoundary(Class<?> type) {
        // Native tracking owns LivingEntity/Entity coordinates and health. Vanilla family
        // subclasses still own renderer state, such as chicken wings and pig saddles.
        return api(type)&&(!legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity.class.isAssignableFrom(type)
                ||type==legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity.class);
    }
    /** Null means the scalar/entity schema can be inspected. Other object-valued fields remain local. */
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
        if(original instanceof legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity&&entity.dataTracker!=null)
            result.put("$tracker",entity.dataTracker.mcc$snapshot());
        return result;
    }
    private static List<Slot> generationSlots(Object original){
        return slots(original).stream().filter(slot->!api(slot.field.getDeclaringClass())
            &&(slot.field.getType().isPrimitive()||slot.field.getType()==String.class)).toList();
    }
    /** Only first-materialization state: never captures SDK coordinates/health or transient numeric entity IDs. */
    public static CompoundTag captureGenerationScalars(Object original){
        var result=new CompoundTag();
        for(var slot:generationSlots(original))try{result.put(slot.key,encode(slot.field.getType(),slot.field.get(original)));}
        catch(IllegalAccessException error){throw new IllegalStateException("Cannot capture original generation field "+slot.key,error);}
        return result;
    }
    /** Validate all fields before returning an apply action; callers can validate a complete passenger tree atomically. */
    public static Runnable prepareGenerationScalars(Object original,CompoundTag incoming){
        var slots=generationSlots(original);var expected=slots.stream().map(Slot::key).collect(java.util.stream.Collectors.toSet());
        if(!expected.equals(incoming.getAllKeys()))throw new IllegalArgumentException("Original generation scalar schema mismatch for "+original.getClass().getName());
        var changes=new ArrayList<java.util.Map.Entry<Slot,Object>>();
        for(var slot:slots)changes.add(new java.util.AbstractMap.SimpleImmutableEntry<>(slot,decode(slot.field.getType(),incoming.get(slot.key),slot.key)));
        return ()->{for(var change:changes)try{change.getKey().field.set(original,change.getValue());}
            catch(IllegalAccessException error){throw new IllegalStateException("Cannot restore original generation field "+change.getKey().key,error);}};
    }
    /**
     * Apply only values changed by the server since the previous received snapshot. An initial null snapshot applies all fields.
     * Compare against the received packet, never the mutable client view: render callbacks may deliberately change local fields.
     */
    public static void apply(Object original,CompoundTag previousReceived,CompoundTag incoming) {
        apply(original,previousReceived,incoming,(id,uuid)->resolve(original,id,uuid));
    }
    private record EntityReference(int id,java.util.UUID uuid){}
    // Original classes can override equals/hashCode; packet ownership follows object identity.
    private static final java.util.Map<Object,java.util.Map<Slot,EntityReference>> PENDING=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private static boolean entityReference(Class<?> type){return legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class.isAssignableFrom(type);}
    private static legacyconnector.vb1_7_3.net.minecraft.entity.Entity resolve(Object original,int id,java.util.UUID uuid){
        if(!(original instanceof legacyconnector.vb1_7_3.net.minecraft.entity.Entity owner)||owner.world==null||owner.world.mcc$level()==null)return null;
        var target=owner.world.mcc$level().getEntity(id);
        return target!=null&&target.getUUID().equals(uuid)?EntityViewsB173.wrapReadOnly(target):null;
    }
    static void apply(Object original,CompoundTag previousReceived,CompoundTag incoming,java.util.function.BiFunction<Integer,java.util.UUID,legacyconnector.vb1_7_3.net.minecraft.entity.Entity> resolver) {
        var changes=new ArrayList<java.util.Map.Entry<Slot,Object>>();
        for(Slot slot:slots(original)) {
            Tag next=incoming.get(slot.key);
            if(next==null)throw new IllegalArgumentException("Missing original render field "+slot.key);
            Object value=decode(slot.field.getType(),next,slot.key);
            if(previousReceived==null||!next.equals(previousReceived.get(slot.key)))
                changes.add(new java.util.AbstractMap.SimpleImmutableEntry<>(slot,value));
        }
        Runnable tracker=()->{};
        if(original instanceof legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity&&entity.dataTracker!=null){
            if(!incoming.contains("$tracker",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Missing original tracker snapshot");
            tracker=entity.dataTracker.mcc$prepare(previousReceived==null?null:previousReceived.getCompound("$tracker"),incoming.getCompound("$tracker"));
        }
        for(var change:changes)try{
            var slot=change.getKey();var value=change.getValue();
            if(entityReference(slot.field.getType())){
                synchronized(PENDING){
                    var pending=PENDING.computeIfAbsent(original,key->new java.util.HashMap<>());
                    if(value instanceof EntityReference reference)pending.put(slot,reference);else pending.remove(slot);
                    if(pending.isEmpty())PENDING.remove(original);
                }
                slot.field.set(original,null);
            }else slot.field.set(original,value);
        }
        catch(IllegalAccessException error){throw new IllegalStateException("Cannot apply original render field "+change.getKey().key,error);}
        tracker.run();
        retryReferences(original,resolver);
    }
    /** A referent's spawn packet can arrive after this snapshot, without another metadata update. */
    public static void retryReferences(Object original){retryReferences(original,(id,uuid)->resolve(original,id,uuid));}
    static void retryReferences(Object original,java.util.function.BiFunction<Integer,java.util.UUID,legacyconnector.vb1_7_3.net.minecraft.entity.Entity> resolver){
        synchronized(PENDING){
            var pending=PENDING.get(original);if(pending==null)return;
            var iterator=pending.entrySet().iterator();
            while(iterator.hasNext()){
                var entry=iterator.next();var reference=entry.getValue();var value=resolver.apply(reference.id,reference.uuid);
                // Validate the native identity again even for an injected resolver: numeric ids may be reused.
                if(value==null||value.mcc$modern==null||value.mcc$modern.getId()!=reference.id||!value.mcc$modern.getUUID().equals(reference.uuid)||!entry.getKey().field.getType().isInstance(value))continue;
                try{entry.getKey().field.set(original,value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}iterator.remove();
            }
            if(pending.isEmpty())PENDING.remove(original);
        }
    }
    private static Tag encode(Class<?> type,Object value) {
        if(entityReference(type)){
            var tag=new CompoundTag();
            if(value instanceof legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity&&entity.mcc$modern!=null){tag.putInt("entityId",entity.mcc$modern.getId());tag.putUUID("entityUUID",entity.mcc$modern.getUUID());}
            return tag;
        }
        if(type.isEnum())return value==null?new CompoundTag():StringTag.valueOf(((Enum<?>)value).name());
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
        if(entityReference(type)&&value instanceof CompoundTag compound){
            if(compound.isEmpty())return null;
            if(compound.contains("entityId",Tag.TAG_INT)&&compound.hasUUID("entityUUID"))return new EntityReference(compound.getInt("entityId"),compound.getUUID("entityUUID"));
        }
        if(type.isEnum()){if(value instanceof StringTag string){for(Object constant:type.getEnumConstants())if(((Enum<?>)constant).name().equals(string.getAsString()))return constant;}else if(value instanceof CompoundTag compound&&compound.isEmpty())return null;}
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
