package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import org.objectweb.asm.*;

/** Actor-local scalar state written by original hook implementation methods; helper-class globals remain shared. */
public final class HookActorStateB173 {
    private static final Map<Field,Object> BOOTSTRAP=new LinkedHashMap<>();
    private static final Map<UUID,Map<Field,Object>> ACTORS=new HashMap<>();
    private record Active(UUID actor){}
    private static final ThreadLocal<Active> ACTIVE=new ThreadLocal<>();
    private record MethodKey(String owner,String name,String descriptor){}
    private record Call(int opcode,String owner,String name,String descriptor){}
    private record Write(String owner,String name,String descriptor){}
    private record Body(List<Call> calls,List<Write> writes){}
    private static final ClassValue<List<Field>> FIELDS=new ClassValue<>(){
        @Override protected List<Field> computeValue(Class<?> type){return discover(type);}
    };
    private HookActorStateB173(){}
    private static List<Field> discover(Class<?> type){
        var hierarchy=new LinkedHashMap<String,Class<?>>();
        for(Class<?> c=type;c!=null&&c!=BaseMod.class&&BaseMod.class.isAssignableFrom(c);c=c.getSuperclass())
            if(!c.getName().startsWith("legacyconnector.")&&!c.getName().startsWith("legacyconnector_synth."))hierarchy.put(Type.getInternalName(c),c);
        var bodies=new HashMap<MethodKey,Body>();
        try{
            for(var entry:hierarchy.entrySet())try(var input=entry.getValue().getResourceAsStream("/"+entry.getKey()+".class")){
                if(input==null)throw new IllegalStateException("Missing original hook implementation bytes: "+entry.getKey());
                String owner=entry.getKey();
                new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){
                    @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                        var calls=new ArrayList<Call>();var writes=new ArrayList<Write>();bodies.put(new MethodKey(owner,name,descriptor),new Body(calls,writes));
                        return new MethodVisitor(Opcodes.ASM9){
                            @Override public void visitMethodInsn(int opcode,String target,String called,String desc,boolean itf){if(hierarchy.containsKey(target))calls.add(new Call(opcode,target,called,desc));}
                            @Override public void visitFieldInsn(int opcode,String target,String field,String desc){if(opcode==Opcodes.PUTSTATIC&&hierarchy.containsKey(target))writes.add(new Write(target,field,desc));}
                        };
                    }
                },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
            }
            var pending=new ArrayDeque<MethodKey>();String root=Type.getInternalName(type);
            for(String name:List.of("OnTickInGame","OnTickInGUI")){
                String descriptor=name.equals("OnTickInGame")?"(Llegacyconnector/vb1_7_3/net/minecraft/client/Minecraft;)Z"
                        :"(Llegacyconnector/vb1_7_3/net/minecraft/client/Minecraft;Llegacyconnector/vb1_7_3/net/minecraft/client/gui/screen/Screen;)Z";
                MethodKey method=resolve(root,name,descriptor,hierarchy,bodies);if(method!=null)pending.add(method);
            }
            var visited=new HashSet<MethodKey>();var fields=new LinkedHashSet<Field>();
            while(!pending.isEmpty()){
                var method=pending.removeFirst();if(!visited.add(method))continue;var body=bodies.get(method);if(body==null)continue;
                for(var call:body.calls){
                    String start=call.opcode==Opcodes.INVOKEVIRTUAL||call.opcode==Opcodes.INVOKEINTERFACE?root:call.owner;
                    var target=resolve(start,call.name,call.descriptor,hierarchy,bodies);if(target!=null)pending.add(target);
                }
                for(var write:body.writes){
                    for(Class<?> c=hierarchy.get(write.owner);c!=null&&hierarchy.containsValue(c);c=c.getSuperclass())try{
                        Field field=c.getDeclaredField(write.name);
                        if(!Type.getDescriptor(field.getType()).equals(write.descriptor))break;
                        int access=field.getModifiers();Class<?> kind=field.getType();
                        if(Modifier.isStatic(access)&&!Modifier.isFinal(access)&&!field.isSynthetic()&&(kind.isPrimitive()||kind==String.class)){
                            if(!field.trySetAccessible())throw new IllegalStateException("Inaccessible original hook scalar "+field);fields.add(field);
                        }
                        break;
                    }catch(NoSuchFieldException ignored){}
                }
            }
            return List.copyOf(fields);
        }catch(java.io.IOException error){throw new IllegalStateException("Cannot inspect original hook scalar state",error);}
    }
    private static MethodKey resolve(String owner,String name,String descriptor,Map<String,Class<?>> hierarchy,Map<MethodKey,Body> bodies){
        for(Class<?> c=hierarchy.get(owner);c!=null&&hierarchy.containsValue(c);c=c.getSuperclass()){
            var key=new MethodKey(Type.getInternalName(c),name,descriptor);if(bodies.containsKey(key))return key;
        }
        return null;
    }
    public static final class Scope implements AutoCloseable {
        private final Active previous,current;private final Map<Field,Object> saved;private boolean closed;
        private Scope(Active previous,Active current,Map<Field,Object> saved){this.previous=previous;this.current=current;this.saved=saved;}
        @Override public void close(){
            if(closed)return;closed=true;if(saved==null)return;
            try{
                if(current.actor!=null){var values=ACTORS.computeIfAbsent(current.actor,key->new HashMap<>());for(var field:saved.keySet())values.put(field,read(field));}
            }finally{
                try{for(var entry:saved.entrySet())write(entry.getKey(),entry.getValue());}
                finally{if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);}
            }
        }
    }
    private static Object read(Field field){try{return field.get(null);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private static void write(Field field,Object value){try{field.set(null,value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    static Scope open(UUID actor,Collection<Class<?>> implementations){
        Active previous=ACTIVE.get();if(previous!=null&&Objects.equals(previous.actor,actor))return new Scope(null,null,null);
        var fields=new LinkedHashSet<Field>();for(var type:implementations)fields.addAll(FIELDS.get(type));
        var saved=new LinkedHashMap<Field,Object>();Map<Field,Object> values=actor==null?Map.of():ACTORS.getOrDefault(actor,Map.of());
        for(var field:fields){Object current=read(field);saved.put(field,current);if(!BOOTSTRAP.containsKey(field))BOOTSTRAP.put(field,current);write(field,values.containsKey(field)?values.get(field):BOOTSTRAP.get(field));}
        Active current=new Active(actor);ACTIVE.set(current);return new Scope(previous,current,saved);
    }
    public static Scope forContext(legacyconnector.vb1_7_3.net.minecraft.world.World world,legacyconnector.vb1_7_3.net.minecraft.entity.Entity focus){
        if(world==null||!(world.mcc$level() instanceof net.minecraft.server.level.ServerLevel level)||!level.getServer().isDedicatedServer()||!level.getServer().isSameThread())return new Scope(null,null,null);
        UUID actor=focus!=null&&focus.mcc$modern instanceof net.minecraft.server.level.ServerPlayer player?player.getUUID():null;
        return open(actor,ContentB173.loadedMods().stream().map(Object::getClass).toList());
    }
    public static Scope forPlayer(net.minecraft.server.level.ServerPlayer player){
        if(!player.server.isDedicatedServer()||!player.server.isSameThread())return new Scope(null,null,null);
        return open(player.getUUID(),ContentB173.loadedMods().stream().map(Object::getClass).toList());
    }
    static List<Field> fields(Class<?> implementation){return FIELDS.get(implementation);}
    public static void remove(UUID actor){ACTORS.remove(actor);}
    public static void clear(){ACTORS.clear();BOOTSTRAP.clear();ACTIVE.remove();}
}
