package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.ref.WeakReference;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerInventory;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.*;
import org.objectweb.asm.Type;

/** Successful original GUI constructor arguments, retained weakly and serialized only through the closed menu context. */
public final class MenuConstructorStateB173 {
    private record Snapshot(String descriptor,List<Object> arguments){}
    private static final int MAX_SCREENS=128,MAX_ARGUMENTS=8,MAX_STRING=2048;
    private static final Map<HandledScreen,Snapshot> CAPTURED=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private MenuConstructorStateB173(){}
    /** Transformer entry point. Only the most-derived successful constructor can describe the GUI. */
    public static synchronized void record(Object screen,Class<?> owner,String descriptor,Object[] arguments){
        if(!(screen instanceof HandledScreen gui)||screen.getClass()!=owner)return;
        CAPTURED.remove(gui);
        if(arguments==null||arguments.length>MAX_ARGUMENTS||descriptor==null||descriptor.length()>2048)return;
        var args=new ArrayList<Object>();
        for(Object value:arguments){
            if(value instanceof Inventory)args.add(new WeakReference<>(value));
            else if(value instanceof String text&&text.length()<=MAX_STRING)args.add(text);
            else if(value instanceof Boolean||value instanceof Byte||value instanceof Short||value instanceof Character||value instanceof Integer||value instanceof Long
                    ||value instanceof Float f&&Float.isFinite(f)||value instanceof Double d&&Double.isFinite(d))args.add(value);
            else return;
        }
        while(CAPTURED.size()>=MAX_SCREENS){var iterator=CAPTURED.keySet().iterator();if(!iterator.hasNext())break;CAPTURED.remove(iterator.next());}
        CAPTURED.put(gui,new Snapshot(descriptor,List.copyOf(args)));
    }
    static synchronized CompoundTag capture(HandledScreen gui){
        var snapshot=CAPTURED.get(gui);if(snapshot==null)return null;
        var result=new CompoundTag();result.putString("descriptor",snapshot.descriptor());var args=new ListTag();
        try{for(Object stored:snapshot.arguments()){
            Object value=stored instanceof WeakReference<?> reference?reference.get():stored;if(value==null)return null;
            var entry=new CompoundTag();
            if(value instanceof PlayerInventory)entry.putString("kind","player");
            else if(value instanceof BlockEntity tile&&value instanceof Inventory){
                String id=BlockEntity.mcc$idOf(tile.getClass());if(id==null)return null;
                entry.putString("kind","tile");entry.putString("type",id);entry.putInt("x",tile.x);entry.putInt("y",tile.y);entry.putInt("z",tile.z);
            }else if(value instanceof String text){entry.putString("kind","java.lang.String");entry.putString("value",text);}
            else put(entry,value);
            args.add(entry);
        }}catch(IllegalArgumentException unsupported){return null;}
        result.put("arguments",args);return result;
    }
    private static void put(CompoundTag entry,Object value){
        if(value instanceof Boolean v){entry.putString("kind","boolean");entry.putBoolean("value",v);}else if(value instanceof Byte v){entry.putString("kind","byte");entry.putByte("value",v);}
        else if(value instanceof Short v){entry.putString("kind","short");entry.putShort("value",v);}else if(value instanceof Character v){entry.putString("kind","char");entry.putInt("value",v);}
        else if(value instanceof Integer v){entry.putString("kind","int");entry.putInt("value",v);}else if(value instanceof Long v){entry.putString("kind","long");entry.putLong("value",v);}
        else if(value instanceof Float v){entry.putString("kind","float");entry.putFloat("value",v);}else if(value instanceof Double v){entry.putString("kind","double");entry.putDouble("value",v);}else throw new IllegalArgumentException();
    }
    private static Object primitive(Class<?> type,CompoundTag entry){
        int kind=type==boolean.class||type==byte.class?Tag.TAG_BYTE:type==short.class?Tag.TAG_SHORT:type==int.class||type==char.class?Tag.TAG_INT:type==long.class?Tag.TAG_LONG:type==float.class?Tag.TAG_FLOAT:Tag.TAG_DOUBLE;
        if(!entry.contains("value",kind))return null;
        if(type==boolean.class)return entry.getBoolean("value");if(type==byte.class)return entry.getByte("value");if(type==short.class)return entry.getShort("value");if(type==int.class)return entry.getInt("value");if(type==long.class)return entry.getLong("value");
        if(type==char.class){int value=entry.getInt("value");return value>=0&&value<=65535?(char)value:null;}
        if(type==float.class){float value=entry.getFloat("value");return Float.isFinite(value)?value:null;}
        double value=entry.getDouble("value");return Double.isFinite(value)?value:null;
    }
    record Prepared(java.lang.reflect.Constructor<?> constructor,Object[] arguments){
        HandledScreen create()throws ReflectiveOperationException{return (HandledScreen)constructor.newInstance(arguments);}
    }
    static Prepared prepare(Class<? extends HandledScreen> type,CompoundTag state,PlayerInventory player,Map<String,BlockEntity> tiles){
        if(!state.contains("arguments",Tag.TAG_LIST)||state.getString("descriptor").length()>2048)return null;
        var raw=(ListTag)state.get("arguments");if(!raw.isEmpty()&&raw.getElementType()!=Tag.TAG_COMPOUND)return null;
        var entries=state.getList("arguments",Tag.TAG_COMPOUND);if(entries.size()>MAX_ARGUMENTS)return null;
        for(var constructor:type.getConstructors()){
            if(!Type.getConstructorDescriptor(constructor).equals(state.getString("descriptor")))continue;
            var parameters=constructor.getParameterTypes();if(parameters.length!=entries.size())return null;
            Object[] args=new Object[entries.size()];
            for(int index=0;index<args.length;index++){
                Class<?> parameter=parameters[index];var entry=entries.getCompound(index);String kind=entry.getString("kind");Object value;
                if(Inventory.class.isAssignableFrom(parameter)&&kind.equals("player"))value=player;
                else if(Inventory.class.isAssignableFrom(parameter)&&kind.equals("tile"))value=tiles.get(entry.getString("type")+":"+entry.getInt("x")+":"+entry.getInt("y")+":"+entry.getInt("z"));
                else if(parameter==String.class&&kind.equals("java.lang.String")&&entry.contains("value",Tag.TAG_STRING)&&entry.getString("value").length()<=MAX_STRING)value=entry.getString("value");
                else if(parameter.isPrimitive()&&kind.equals(parameter.getName()))value=primitive(parameter,entry);
                else return null;
                if(value==null||!parameter.isPrimitive()&&!parameter.isInstance(value))return null;args[index]=value;
            }
            return new Prepared(constructor,args);
        }
        return null;
    }
}
