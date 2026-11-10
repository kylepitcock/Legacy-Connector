package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import org.objectweb.asm.*;

/** Actor-local scalar writes reachable from executable original hooks, including helpers owned by the same artifact. */
public final class HookActorStateB173 {
    private static final Map<Field,Object> BOOTSTRAP=new LinkedHashMap<>();
    private static final Map<UUID,Map<Field,Object>> ACTORS=new HashMap<>();
    private record Active(UUID actor){}
    private static final ThreadLocal<Active> ACTIVE=new ThreadLocal<>();
    private record MethodKey(String owner,String name,String descriptor){}
    private record Call(int opcode,String owner,String name,String descriptor){}
    private record Write(String owner,String name,String descriptor){}
    private record Body(List<Call> calls,List<Write> writes,List<Write> accesses){}
    private static final ClassValue<List<Field>> FIELDS=new ClassValue<>(){
        @Override protected List<Field> computeValue(Class<?> type){return discover(type);}
    };
    private HookActorStateB173(){}
    private static List<Field> discover(Class<?> type){return new Discovery(type,512,16384,131072).fields();}
    static List<Field> fields(Class<?> type,int classes,int methods,int edges){return new Discovery(type,classes,methods,edges).fields();}
    private static final class Discovery {
        private final Class<?> implementation;
        private final int maxClasses,maxMethods,maxEdges;
        private final Map<String,Class<?>> classes=new LinkedHashMap<>();
        private final Set<String> foreign=new HashSet<>();
        private final Map<MethodKey,Body> bodies=new HashMap<>();
        private int edges;
        Discovery(Class<?> implementation,int maxClasses,int maxMethods,int maxEdges){this.implementation=implementation;this.maxClasses=maxClasses;this.maxMethods=maxMethods;this.maxEdges=maxEdges;}
        private static boolean api(String name){return name.startsWith("legacyconnector/")||name.startsWith("legacyconnector_synth/")||name.startsWith("java/")||name.startsWith("javax/")||name.startsWith("jdk/")||name.startsWith("sun/")||name.startsWith("net/minecraft/")||name.startsWith("net/neoforged/");}
        private boolean sameArtifact(Class<?> candidate){
            if(candidate.getClassLoader()!=implementation.getClassLoader())return false;
            var source=implementation.getProtectionDomain().getCodeSource();var other=candidate.getProtectionDomain().getCodeSource();
            return source!=null&&other!=null?source.equals(other):implementation.getModule().isNamed()&&implementation.getModule()==candidate.getModule();
        }
        private Class<?> load(String owner){
            if(classes.containsKey(owner))return classes.get(owner);if(api(owner)||foreign.contains(owner))return null;
            Class<?> type;
            try{type=Class.forName(owner.replace('/','.'),false,implementation.getClassLoader());}
            catch(ClassNotFoundException missing){foreign.add(owner);return null;}
            if(!sameArtifact(type)){foreign.add(owner);return null;}
            if(classes.size()>=maxClasses)throw new IllegalStateException("Original hook helper class graph exceeds bounded discovery");
            classes.put(owner,type);
            try(var input=type.getResourceAsStream("/"+owner+".class")){
                if(input==null)throw new IllegalStateException("Missing original hook/helper bytes: "+owner);
                new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9){
                    @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                        if(name.startsWith("<"))return null; // Constructors and static initialization are not executable hook edges.
                        if(bodies.size()>=maxMethods)throw new IllegalStateException("Original hook method graph exceeds bounded discovery");
                        var calls=new ArrayList<Call>();var writes=new ArrayList<Write>();var accesses=new ArrayList<Write>();bodies.put(new MethodKey(owner,name,descriptor),new Body(calls,writes,accesses));
                        return new MethodVisitor(Opcodes.ASM9){
                            private void edge(){if(++edges>maxEdges)throw new IllegalStateException("Original hook bytecode graph exceeds bounded discovery");}
                            @Override public void visitMethodInsn(int opcode,String target,String called,String desc,boolean itf){if(!called.startsWith("<")&&!api(target)){edge();calls.add(new Call(opcode,target,called,desc));}}
                            @Override public void visitFieldInsn(int opcode,String target,String name,String desc){if((opcode==Opcodes.GETSTATIC||opcode==Opcodes.PUTSTATIC)&&!api(target)){edge();var access=new Write(target,name,desc);accesses.add(access);if(opcode==Opcodes.PUTSTATIC)writes.add(access);}}
                            @Override public void visitInvokeDynamicInsn(String name,String desc,Handle bootstrap,Object... arguments){
                                for(var argument:arguments)if(argument instanceof Handle handle&&!handle.getName().startsWith("<")&&!api(handle.getOwner())){
                                    int opcode=switch(handle.getTag()){case Opcodes.H_INVOKESTATIC->Opcodes.INVOKESTATIC;case Opcodes.H_INVOKESPECIAL->Opcodes.INVOKESPECIAL;case Opcodes.H_INVOKEVIRTUAL->Opcodes.INVOKEVIRTUAL;case Opcodes.H_INVOKEINTERFACE->Opcodes.INVOKEINTERFACE;default->-1;};
                                    if(opcode>=0){edge();calls.add(new Call(opcode,handle.getOwner(),handle.getName(),handle.getDesc()));}
                                }
                            }
                        };
                    }
                },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
            }catch(java.io.IOException error){throw new IllegalStateException("Cannot inspect original hook/helper state",error);}
            return type;
        }
        private MethodKey resolve(String owner,String name,String descriptor){
            for(Class<?> type=load(owner);type!=null;type=type.getSuperclass()){
                String current=Type.getInternalName(type);if(load(current)==null)break;
                var key=new MethodKey(current,name,descriptor);if(bodies.containsKey(key))return key;
            }return null;
        }
        List<Field> fields(){
            var pending=new ArrayDeque<MethodKey>();String root=Type.getInternalName(implementation);
            for(String name:List.of("OnTickInGame","OnTickInGUI")){
                String descriptor=name.equals("OnTickInGame")?"(Llegacyconnector/vb1_7_3/net/minecraft/client/Minecraft;)Z":"(Llegacyconnector/vb1_7_3/net/minecraft/client/Minecraft;Llegacyconnector/vb1_7_3/net/minecraft/client/gui/screen/Screen;)Z";
                var method=resolve(root,name,descriptor);if(method!=null)pending.add(method);
            }
            return walk(pending,true);
        }
        List<Field> fieldsFrom(String owner,String name,String descriptor){
            var pending=new ArrayDeque<MethodKey>();var method=resolve(owner,name,descriptor);if(method!=null)pending.add(method);return walk(pending,false);
        }
        private boolean oldHierarchy(Class<?> type){
            return BaseMod.class.isAssignableFrom(type)&&type.isAssignableFrom(implementation);
        }
        private Field field(Write access){
            for(Class<?> type=load(access.owner);type!=null&&load(Type.getInternalName(type))!=null;type=type.getSuperclass())try{
                Field field=type.getDeclaredField(access.name);
                return Type.getDescriptor(field.getType()).equals(access.descriptor)?field:null;
            }catch(NoSuchFieldException ignored){}
            return null;
        }
        private static boolean ledger(Class<?> type){return type.isArray()||Collection.class.isAssignableFrom(type)||Map.class.isAssignableFrom(type);}
        private List<Field> walk(ArrayDeque<MethodKey> pending,boolean filterHelpers){
            String root=Type.getInternalName(implementation);
            var visited=new HashSet<MethodKey>();var result=new LinkedHashSet<Field>();
            var callers=new HashMap<MethodKey,Set<MethodKey>>();
            var sharedMethods=new HashSet<MethodKey>();var sharedOwners=new HashSet<Class<?>>();
            var writers=new HashMap<Field,Set<MethodKey>>();
            while(!pending.isEmpty()){
                var method=pending.removeFirst();if(!visited.add(method))continue;var body=bodies.get(method);if(body==null)continue;
                for(var call:body.calls){
                    Class<?> declared=load(call.owner);if(declared==null)continue;
                    String start=(call.opcode==Opcodes.INVOKEVIRTUAL||call.opcode==Opcodes.INVOKEINTERFACE)&&declared.isAssignableFrom(implementation)?root:call.owner;
                    var target=resolve(start,call.name,call.descriptor);
                    if(target!=null){pending.add(target);callers.computeIfAbsent(target,key->new HashSet<>()).add(method);}
                }
                if(filterHelpers)for(var access:body.accesses){
                    var field=field(access);
                    // A final reference still owns a mutable collection/array. Never read its value
                    // or initialize its helper just to decide whether that state can be actor-local.
                    if(field!=null&&Modifier.isStatic(field.getModifiers())&&ledger(field.getType())){
                        sharedMethods.add(method);sharedOwners.add(field.getDeclaringClass());
                    }
                }
                for(var write:body.writes){
                    Field field=field(write);if(field==null)continue;
                    int access=field.getModifiers();Class<?> kind=field.getType();
                    if(Modifier.isStatic(access)&&!Modifier.isFinal(access)&&!field.isSynthetic()&&(kind.isPrimitive()||kind==String.class||kind==legacyconnector.vb1_7_3.net.minecraft.world.World.class)){
                        if(!field.trySetAccessible())throw new IllegalStateException("Inaccessible original hook/helper scalar "+field);
                        result.add(field);writers.computeIfAbsent(field,key->new HashSet<>()).add(method);
                    }
                }
            }
            if(!filterHelpers)return List.copyOf(result); // Target callbacks intersect the already-eligible hook schema.
            var shared=new ArrayDeque<>(sharedMethods);
            while(!shared.isEmpty())for(var caller:callers.getOrDefault(shared.removeFirst(),Set.of()))
                if(sharedMethods.add(caller))shared.add(caller);
            // Preserve the previous BaseMod hierarchy contract. Only NEW helper state is rejected
            // when its clock/metadata coordinates a ledger that remains shared across all actors.
            return result.stream().filter(field->oldHierarchy(field.getDeclaringClass())
                    ||(!sharedOwners.contains(field.getDeclaringClass())
                    &&writers.get(field).stream().noneMatch(sharedMethods::contains))).toList();
        }
    }
    private static final ClassValue<java.util.concurrent.ConcurrentMap<String,Boolean>> TARGET_WRITERS=new ClassValue<>(){
        @Override protected java.util.concurrent.ConcurrentMap<String,Boolean> computeValue(Class<?> type){return new java.util.concurrent.ConcurrentHashMap<>();}
    };
    static boolean writesActorFields(Class<?> owner,String name,String descriptor,Collection<Class<?>> implementations){
        var scoped=new HashSet<Field>();
        for(var implementation:implementations){var discovery=new Discovery(implementation,512,16384,131072);if(discovery.sameArtifact(owner))scoped.addAll(FIELDS.get(implementation));}
        if(scoped.isEmpty())return false;
        var writes=new Discovery(owner,512,16384,131072).fieldsFrom(Type.getInternalName(owner),name,descriptor);
        return writes.stream().anyMatch(scoped::contains);
    }
    static UUID targetActor(boolean explicitPlayer,UUID candidate,boolean writesScopedFields){return !explicitPlayer&&writesScopedFields?candidate:null;}
    public static final class TargetScope implements AutoCloseable {
        private final legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.ContextScope context;
        private final legacyconnector.vb1_7_3.net.minecraft.client.Minecraft minecraft;
        private final legacyconnector.vb1_7_3.net.minecraft.world.World previousWorld;
        private TargetScope(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.ContextScope context,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft minecraft,legacyconnector.vb1_7_3.net.minecraft.world.World previousWorld){this.context=context;this.minecraft=minecraft;this.previousWorld=previousWorld;}
        @Override public void close(){if(context!=null)try{context.close();}finally{minecraft.world=previousWorld;}}
    }
    private static final TargetScope NO_TARGET=new TargetScope(null,null,null);
    /** Transformed ORIGINAL methods only: an explicit native player argument supplies a fallback, never overrides a caller. */
    public static TargetScope enterTarget(Class<?> owner,String name,String descriptor,legacyconnector.vb1_7_3.net.minecraft.entity.Entity argument){
        if(argument==null||!(argument.mcc$modern instanceof net.minecraft.server.level.ServerPlayer player)||player.isRemoved()||!player.server.isDedicatedServer()||!player.server.isSameThread())return NO_TARGET;
        var current=ACTIVE.get();
        boolean explicit=current!=null&&current.actor()!=null||MenusB173.currentActor()!=null||legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext()!=null;
        if(explicit)return NO_TARGET;
        boolean writes=TARGET_WRITERS.get(owner).computeIfAbsent(name+descriptor,key->writesActorFields(owner,name,descriptor,ContentB173.loadedMods().stream().map(Object::getClass).toList()));
        if(targetActor(false,player.getUUID(),writes)==null)return NO_TARGET;
        var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();var previousWorld=minecraft.world;
        return new TargetScope(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(player.level()),argument),minecraft,previousWorld);
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
        var initialized=new ArrayList<Field>();
        try{
            for(var field:fields){Object current=read(field);saved.put(field,current);if(!BOOTSTRAP.containsKey(field)){BOOTSTRAP.put(field,current);initialized.add(field);}write(field,values.containsKey(field)?values.get(field):BOOTSTRAP.get(field));}
        }catch(RuntimeException|Error failure){
            for(var entry:saved.entrySet())try{write(entry.getKey(),entry.getValue());}catch(RuntimeException|Error restore){failure.addSuppressed(restore);}
            initialized.forEach(BOOTSTRAP::remove);throw failure;
        }
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
    public static void clearLevel(net.minecraft.world.level.Level level){
        var worlds=Collections.newSetFromMap(new IdentityHashMap<legacyconnector.vb1_7_3.net.minecraft.world.World,Boolean>());
        for(var value:BOOTSTRAP.values())if(value instanceof legacyconnector.vb1_7_3.net.minecraft.world.World world&&world.mcc$level()==level)worlds.add(world);
        for(var values:ACTORS.values())for(var value:values.values())if(value instanceof legacyconnector.vb1_7_3.net.minecraft.world.World world&&world.mcc$level()==level)worlds.add(world);
        for(var field:BOOTSTRAP.keySet())if(field.getType()==legacyconnector.vb1_7_3.net.minecraft.world.World.class&&read(field) instanceof legacyconnector.vb1_7_3.net.minecraft.world.World world&&world.mcc$level()==level)worlds.add(world);
        worlds.forEach(HookActorStateB173::clearWorld);
    }
    public static void clearWorld(legacyconnector.vb1_7_3.net.minecraft.world.World world){
        if(world==null)return;
        for(var entry:BOOTSTRAP.entrySet())if(entry.getValue()==world)entry.setValue(null);
        for(var values:ACTORS.values())for(var entry:values.entrySet())if(entry.getValue()==world)entry.setValue(null);
        for(var field:BOOTSTRAP.keySet())if(field.getType()==legacyconnector.vb1_7_3.net.minecraft.world.World.class&&read(field)==world)write(field,null);
    }
    public static void clear(){ACTORS.clear();BOOTSTRAP.clear();ACTIVE.remove();}
}
