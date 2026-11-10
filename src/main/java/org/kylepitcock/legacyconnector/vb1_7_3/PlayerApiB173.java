package org.kylepitcock.legacyconnector.vb1_7_3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.src.PlayerBase;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Registered PlayerAPI extensions belong to the stable legacy player view. */
public final class PlayerApiB173 {
    public static final class State {
        private final Map<Class<?>,PlayerBase> bases=new LinkedHashMap<>();
        private final Map<Class<?>,RuntimeException> failures=new LinkedHashMap<>();
        private final Set<String> reported=new HashSet<>();
        private boolean constructing;
        private Path mirrorDirectory;
        private long callbackFailures;
    }
    private static final Set<Class<?>> REGISTERED=new LinkedHashSet<>();
    private static final ThreadLocal<ClientPlayerEntity> ACTOR=new ThreadLocal<>();
    private PlayerApiB173(){}

    public static synchronized void register(Class<?> type){
        Objects.requireNonNull(type,"Player base class");
        if(!PlayerBase.class.isAssignableFrom(type))throw new IllegalArgumentException("Not a PlayerBase: "+type.getName());
        try{type.getConstructor(ClientPlayerEntity.class);}
        catch(NoSuchMethodException error){throw new IllegalArgumentException("PlayerBase requires a ClientPlayerEntity constructor: "+type.getName(),error);}
        REGISTERED.add(type);
    }
    private static synchronized List<Class<?>> registered(){return List.copyOf(REGISTERED);}
    public static synchronized boolean hasRegistered(){return !REGISTERED.isEmpty();}
    public static long callbackFailures(ClientPlayerEntity player){return player.mcc$playerApi.callbackFailures;}
    public static ClientPlayerEntity currentPlayer(){return ACTOR.get();}
    public static <T> T withPlayer(ClientPlayerEntity player,Supplier<T> action){
        ClientPlayerEntity previous=ACTOR.get();ACTOR.set(player);
        try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(player.world,player)){
            if(player.mcc$modern instanceof net.minecraft.server.level.ServerPlayer serverPlayer){
                var result=new java.util.concurrent.atomic.AtomicReference<T>();
                MenusB173.withActor(serverPlayer,()->result.set(action.get()));
                return result.get();
            }
            return action.get();
        }finally{if(previous==null)ACTOR.remove();else ACTOR.set(previous);}
    }
    public static void withPlayer(ClientPlayerEntity player,Runnable action){withPlayer(player,()->{action.run();return null;});}

    public static void ensure(ClientPlayerEntity player){
        Objects.requireNonNull(player,"Player");State state=player.mcc$playerApi;
        if(state.constructing)return;
        state.constructing=true;
        try{
            for(Class<?> type:registered()){
                if(state.failures.containsKey(type))continue;
                if(state.bases.containsKey(type))continue;
                try{
                    PlayerBase base=withPlayer(player,()->{
                        try{return (PlayerBase)type.getConstructor(ClientPlayerEntity.class).newInstance(player);}
                        catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot construct PlayerBase "+type.getName(),error.getCause()==null?error:error.getCause());}
                    });
                    state.bases.put(type,base);
                    invoke(player,type,"playerInit",()->{base.playerInit();return false;});
                }catch(RuntimeException|LinkageError error){
                    RuntimeException failure=error instanceof RuntimeException runtime?runtime:new IllegalStateException(error);
                    state.failures.put(type,failure);state.bases.remove(type);state.callbackFailures++;
                    report(player,type,"construct",error);
                }
            }
        }finally{state.constructing=false;}
    }
    public static List<PlayerBase> bases(ClientPlayerEntity player){ensure(player);return List.copyOf(player.mcc$playerApi.bases.values());}
    public static PlayerBase get(ClientPlayerEntity player,Class<?> type){ensure(player);return player.mcc$playerApi.bases.get(type);}
    private static void report(ClientPlayerEntity player,Class<?> type,String hook,Throwable error){
        if(player.mcc$playerApi.reported.add(type.getName()+"#"+hook))
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").error("PlayerAPI {} callback failed for {}",hook,type.getName(),error);
    }
    private static boolean invoke(ClientPlayerEntity player,Class<?> type,String hook,Supplier<Boolean> callback){
        try{return withPlayer(player,callback);}
        catch(RuntimeException|LinkageError error){report(player,type,hook,error);throw error;}
    }
    private static boolean dispatch(ClientPlayerEntity player,String hook,java.util.function.Function<PlayerBase,Boolean> callback){
        boolean replaced=false;
        for(PlayerBase base:bases(player))try{replaced|=invoke(player,base.getClass(),hook,()->callback.apply(base));}
        catch(RuntimeException|LinkageError error){player.mcc$playerApi.callbackFailures++;}
        return replaced;
    }
    public static void init(ClientPlayerEntity player){ensure(player);}
    public static boolean tick(ClientPlayerEntity player){return dispatch(player,"onLivingUpdate",PlayerBase::onLivingUpdate);}
    public static boolean read(ClientPlayerEntity player,NbtCompound tag){return dispatch(player,"readEntityFromNBT",base->base.readEntityFromNBT(tag));}
    public static boolean write(ClientPlayerEntity player,NbtCompound tag){return dispatch(player,"writeEntityToNBT",base->base.writeEntityToNBT(tag));}
    public static boolean death(ClientPlayerEntity player){return dispatch(player,"setEntityDead",PlayerBase::setEntityDead);}

    /** SP-style files written from a PlayerAPI callback belong to that actor, not every player in the world. */
    public static File storageDirectory(World world){
        ClientPlayerEntity actor=currentPlayer();
        if(actor==null&&registeredCaller())actor=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext();
        if(actor!=null&&actor.mcc$modern instanceof net.minecraft.world.entity.player.Player player){
            if(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer){
                return playerStorageDirectory(serverPlayer.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT),player.getUUID());
            }
            State state=actor.mcc$playerApi;
            if(state.mirrorDirectory==null)try{state.mirrorDirectory=Files.createTempDirectory("legacy-b173-player-");}
            catch(IOException error){throw new IllegalStateException("Cannot create client PlayerAPI mirror directory",error);}
            return directory(state.mirrorDirectory);
        }
        if(world!=null&&world.mcc$level() instanceof net.minecraft.server.level.ServerLevel level)
            return directory(level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT));
        throw new IllegalStateException("No native world storage directory available");
    }
    /** Helpers in the same original named module share the registered API extension's player storage ownership. */
    static boolean registeredCaller(){
        Set<Module> modules=new HashSet<>();
        for(Class<?> type:registered()){Module module=type.getModule();if(module.isNamed())modules.add(module);}
        return !modules.isEmpty()&&StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            .walk(frames->frames.anyMatch(frame->modules.contains(frame.getDeclaringClass().getModule())));
    }
    static File playerStorageDirectory(Path worldRoot,UUID player){return directory(worldRoot.resolve("playerdata").resolve(player.toString()).resolve("legacy-b173"));}
    private static File directory(Path path){
        try{Files.createDirectories(path);return path.toFile();}
        catch(IOException error){throw new IllegalStateException("Cannot open legacy storage directory "+path,error);}
    }
}
