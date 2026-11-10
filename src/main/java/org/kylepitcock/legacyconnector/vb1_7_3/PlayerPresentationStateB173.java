package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.src.PlayerBase;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

/** A closed scalar schema from registered original bases; no wire-supplied class is ever loaded. */
public final class PlayerPresentationStateB173 {
    public static final int MAX_BASES=32,MAX_FIELDS=256,MAX_STRING=2048;
    private PlayerPresentationStateB173(){}
    private static final ClassValue<List<Field>> SCHEMA=new ClassValue<>(){
        @Override protected List<Field> computeValue(Class<?> type){return declaredFields(type);}
    };
    public record Identity(UUID subject,int entity,ResourceLocation dimension,long revision){}
    public static boolean matches(Identity wire,UUID self,UUID actual,int entity,ResourceLocation dimension,long revision){
        return wire.revision()>revision&&wire.entity()==entity&&wire.subject().equals(actual)
                &&!wire.subject().equals(self)&&wire.dimension().equals(dimension);
    }
    public static boolean matchesOwner(Identity wire,UUID self,int entity,ResourceLocation dimension,long revision){
        return wire.revision()>revision&&wire.subject().equals(self)&&wire.entity()==entity&&wire.dimension().equals(dimension);
    }
    /** A replacement native player starts a fresh revision lifetime, including death within one level. */
    public static long ownerRevision(Object previous,Object current,long revision){return previous==current?revision:0;}
    public static boolean applyOwnerScalars(PlayerViewB173 view,CompoundTag state){
        if(!(view.mcc$modern instanceof net.minecraft.world.entity.player.Player player)||!player.level().isClientSide)return false;
        return applyScalars(PlayerApiB173.existingBases(view),state);
    }
    private static List<Field> fields(PlayerBase base){
        if(!PlayerApiB173.isRegistered(base.getClass()))return List.of();
        return SCHEMA.get(base.getClass());
    }
    private static List<Field> declaredFields(Class<?> implementation){
        var result=new ArrayList<Field>();
        for(var field:implementation.getDeclaredFields()){
            int modifiers=field.getModifiers();Class<?> type=field.getType();
            if(field.isSynthetic()||Modifier.isStatic(modifiers)||Modifier.isFinal(modifiers)
                    ||!(type.isPrimitive()||type==String.class))continue;
            if(!field.trySetAccessible())throw new IllegalStateException("Inaccessible original presentation field");
            result.add(field);
        }
        result.sort(Comparator.comparing(Field::getName));return List.copyOf(result);
    }
    static CompoundTag captureScalars(List<PlayerBase> bases){
        if(bases.size()>MAX_BASES)throw new IllegalStateException("Too many original presentation bases");
        var list=new ListTag();
        try{for(var base:bases)for(var field:fields(base)){
            if(list.size()>=MAX_FIELDS)throw new IllegalStateException("Too many original presentation fields");
            var entry=new CompoundTag();entry.putString("base",base.getClass().getName());entry.putString("field",field.getName());
            Class<?> type=field.getType();entry.putString("type",type.getName());Object value=field.get(base);
            if(type==String.class){if(value==null)entry.putBoolean("null",true);else {String text=(String)value;if(text.length()>MAX_STRING)throw new IllegalStateException("Original presentation string too long");entry.putString("value",text);}}
            else if(type==boolean.class)entry.putBoolean("value",(boolean)value);
            else if(type==byte.class)entry.putByte("value",(byte)value);
            else if(type==short.class)entry.putShort("value",(short)value);
            else if(type==char.class)entry.putInt("value",(char)value);
            else if(type==int.class)entry.putInt("value",(int)value);
            else if(type==long.class)entry.putLong("value",(long)value);
            else if(type==float.class){if(!Float.isFinite((float)value))throw new IllegalStateException("Nonfinite presentation field");entry.putFloat("value",(float)value);}
            else if(type==double.class){if(!Double.isFinite((double)value))throw new IllegalStateException("Nonfinite presentation field");entry.putDouble("value",(double)value);}
            list.add(entry);
        }}catch(IllegalAccessException error){throw new IllegalStateException(error);}
        var result=new CompoundTag();result.put("fields",list);return result;
    }
    private record Update(PlayerBase base,Field field,Object value){}
    /** Validate the full scalar payload before modifying any original instance. */
    static boolean applyScalars(List<PlayerBase> bases,CompoundTag tag){
        Runnable prepared=prepareScalars(bases,tag);if(prepared==null)return false;prepared.run();return true;
    }
    private static Runnable prepareScalars(List<PlayerBase> bases,CompoundTag tag){
        if(bases.size()>MAX_BASES||!tag.contains("fields",Tag.TAG_LIST))return null;
        if(!((ListTag)tag.get("fields")).isEmpty()&&((ListTag)tag.get("fields")).getElementType()!=Tag.TAG_COMPOUND)return null;
        var entries=tag.getList("fields",Tag.TAG_COMPOUND);if(entries.size()>MAX_FIELDS)return null;
        var schema=new HashMap<String,Update>();
        for(var base:bases)for(var field:fields(base))schema.put(base.getClass().getName()+"#"+field.getName(),new Update(base,field,null));
        var updates=new ArrayList<Update>();var seen=new HashSet<String>();
        for(var raw:entries){
            var entry=(CompoundTag)raw;String key=entry.getString("base")+"#"+entry.getString("field");var binding=schema.get(key);
            if(binding==null||!seen.add(key))return null;
            Class<?> type=binding.field().getType();if(!type.getName().equals(entry.getString("type")))return null;Object value;
            if(type==String.class){if(entry.getBoolean("null"))value=null;else {if(!entry.contains("value",Tag.TAG_STRING)||entry.getString("value").length()>MAX_STRING)return null;value=entry.getString("value");}}
            else {
                int kind=type==boolean.class||type==byte.class?Tag.TAG_BYTE:type==short.class?Tag.TAG_SHORT:type==char.class||type==int.class?Tag.TAG_INT:type==long.class?Tag.TAG_LONG:type==float.class?Tag.TAG_FLOAT:Tag.TAG_DOUBLE;
                if(!entry.contains("value",kind))return null;
                if(type==boolean.class)value=entry.getBoolean("value");else if(type==byte.class)value=entry.getByte("value");
                else if(type==short.class)value=entry.getShort("value");else if(type==int.class)value=entry.getInt("value");
                else if(type==char.class){int n=entry.getInt("value");if(n<0||n>65535)return null;value=(char)n;}
                else if(type==long.class)value=entry.getLong("value");else if(type==float.class){float n=entry.getFloat("value");if(!Float.isFinite(n))return null;value=n;}
                else {double n=entry.getDouble("value");if(!Double.isFinite(n))return null;value=n;}
            }
            updates.add(new Update(binding.base(),binding.field(),value));
        }
        return ()->{
            try{for(var update:updates)update.field().set(update.base(),update.value());}
            catch(IllegalAccessException error){throw new IllegalStateException(error);}
        };
    }
    public static CompoundTag capture(PlayerViewB173 view){
        var result=captureScalars(PlayerApiB173.existingBases(view));result.put("inventories",PlayerInventoryStateB173.capture(view));return result;
    }
    public static boolean apply(PlayerViewB173 view,CompoundTag state){
        if(!(view.mcc$modern instanceof net.minecraft.world.entity.player.Player player)||!player.level().isClientSide)return false;
        var bases=PlayerApiB173.existingBases(view);
        Runnable prepared=prepare(bases,PlayerInventoryStateB173.bindings(bases,view.inventory),state,
                tag->net.minecraft.world.item.ItemStack.parseOptional(player.registryAccess(),tag));
        if(prepared==null)return false;prepared.run();return true;
    }
    /** Scalar and inventory payloads form one validation boundary and one accepted revision. */
    static Runnable prepare(List<PlayerBase> bases,List<PlayerInventoryStateB173.Binding> bindings,CompoundTag state,
            java.util.function.Function<CompoundTag,net.minecraft.world.item.ItemStack> decode){
        if(!state.contains("inventories",Tag.TAG_COMPOUND))return null;
        Runnable scalars=prepareScalars(bases,state);
        if(scalars==null)return null;
        Runnable inventories=PlayerInventoryStateB173.prepare(bindings,state.getCompound("inventories"),decode);
        if(inventories==null)return null;
        return ()->{scalars.run();inventories.run();};
    }
    public static <K,V>void releaseMirrors(Map<K,V> mirrors,java.util.function.Consumer<V> release){
        var values=List.copyOf(mirrors.values());mirrors.clear();values.forEach(release);
    }
    public static <K,V>void trimMirrors(Map<K,V> mirrors,int limit,java.util.function.Consumer<V> release){
        while(mirrors.size()>limit){var key=mirrors.keySet().iterator().next();release.accept(mirrors.remove(key));}
    }
}
