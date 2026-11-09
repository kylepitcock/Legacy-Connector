package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import org.kylepitcock.legacyconnector.core.LegacyLifecycle;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;

/** First mob family: registered CowEntity subclasses, using native cow AI with supported original callbacks. */
public final class MobsB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3/Mobs");
    public record Info(ContentB173.Registration registration,EntityType<LegacyB173Cow> type,Method interact,Method ambient) {}
    private static final Map<Class<?>,Info> BY_CLASS=new LinkedHashMap<>();
    private static final Map<EntityType<?>,Info> BY_TYPE=new LinkedHashMap<>();
    private static final Set<String> REPORTED=new HashSet<>();
    private static final Set<String> SUPPORTED=Set.of("interact","getRandomSound","writeNbt","readNbt");
    private static final Set<String> UNSUPPORTED_BEHAVIOUR=Set.of("tick","tickMovement","tickLiving","tickAI","travel","damage","attack","dropItems","getDroppedItemId","onKilledBy","canSpawn","getHurtSound","getDeathSound");
    private MobsB173(){}
    public static void registerEvents(IEventBus modBus,IEventBus game){
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e)->{
            for(var info:BY_TYPE.values())e.put(info.type(),net.minecraft.world.entity.animal.Cow.createAttributes().build());
        });
        modBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event)->{
            // Only registered hosted cows enter the Beta candidate walk; native cows keep their own placement predicate.
            for(var info:BY_TYPE.values())event.register(info.type(),net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type,level,reason,pos,random)->true,
                    net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        org.kylepitcock.legacyconnector.core.pack.GeneratedPack.addContributor((type,out)->{
            if(type!=net.minecraft.server.packs.PackType.SERVER_DATA)return;
            // Beta Cow.getDroppedItemId is leather; EntityLiving.dropItems chooses random.nextInt(3), with no beef.
            byte[] loot=("{\"type\":\"minecraft:entity\",\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:leather\",\"functions\":[{\"function\":\"minecraft:set_count\",\"count\":{\"type\":\"minecraft:uniform\",\"min\":0,\"max\":2}}]}]}]}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            for(var info:BY_TYPE.values()){
                var key=info.type().getDefaultLootTable().location();
                out.put("data/"+key.getNamespace()+"/loot_table/"+key.getPath()+".json",()->loot);
            }
        });
        MobSelfTestB173.register(game);
        if(Boolean.getBoolean("legacyconnector.plasticCraftBlocksSelfTest"))game.addListener(
                (net.neoforged.neoforge.event.server.ServerStartedEvent event)->PlasticCraftBlocksSelfTestB173.run(event.getServer().overworld()));
    }
    private static boolean api(Class<?> type){String name=type.getName();return name.startsWith("legacyconnector.")||name.startsWith("legacyconnector_synth.");}
    public static boolean cowFamily(Class<?> type){
        for(Class<?> c=type;c!=null;c=c.getSuperclass())if(api(c)&&c.getName().endsWith(".net.minecraft.entity.passive.CowEntity"))return true;
        return false;
    }
    /** Reject inherited custom simulation too; do not replace an unsupported AI with a silent inert approximation. */
    public static String unsupportedBehaviour(Class<?> type){
        for(Class<?> c=type;c!=null&&!api(c);c=c.getSuperclass())for(Method method:c.getDeclaredMethods()){
            if(Modifier.isStatic(method.getModifiers())||method.isSynthetic()||method.isBridge())continue;
            if(UNSUPPORTED_BEHAVIOUR.contains(method.getName()))return c.getSimpleName()+"."+method.getName();
            if(SUPPORTED.contains(method.getName()))continue;
            for(Class<?> parent=c.getSuperclass();parent!=null&&parent!=Object.class;parent=parent.getSuperclass())try{
                parent.getDeclaredMethod(method.getName(),method.getParameterTypes());
                if(api(parent))return c.getSimpleName()+"."+method.getName();
            }catch(NoSuchMethodException ignored){}
        }
        return unsupportedSuperCalls(type);
    }
    private static String unsupportedSuperCalls(Class<?> type){
        for(Class<?> c=type;c!=null&&!api(c);c=c.getSuperclass()){
            String[] failure={null};
            try(var input=c.getResourceAsStream("/"+c.getName().replace('.','/')+".class")){
                if(input==null)return "cannot inspect superclass callbacks of "+c.getSimpleName();
                new org.objectweb.asm.ClassReader(input).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9){
                    @Override public org.objectweb.asm.MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                        return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9){
                            @Override public void visitMethodInsn(int opcode,String owner,String called,String desc,boolean itf){
                                if(opcode==org.objectweb.asm.Opcodes.INVOKESPECIAL&&SUPPORTED.contains(called))failure[0]="unsupported superclass callback "+called;
                            }
                        };
                    }
                },org.objectweb.asm.ClassReader.SKIP_DEBUG|org.objectweb.asm.ClassReader.SKIP_FRAMES);
            }catch(java.io.IOException|RuntimeException|LinkageError e){return "cannot inspect superclass callbacks of "+c.getSimpleName()+" ("+e.getClass().getSimpleName()+")";}
            if(failure[0]!=null)return failure[0];
        }
        return null;
    }
    private static Method override(Class<?> type,String name,Class<?>... arguments){
        for(Class<?> c=type;c!=null&&!api(c);c=c.getSuperclass())try{
            Method method=c.getDeclaredMethod(name,arguments);method.setAccessible(true);return method;
        }catch(NoSuchMethodException ignored){}
        return null;
    }
    public static void registerAll(){
        for(var registration:ContentB173.entities()){
            Class<?> legacy=registration.type();
            if(!LivingEntity.class.isAssignableFrom(legacy)||BY_CLASS.containsKey(legacy))continue;
            if(registration.mod()==null||LegacyLifecycle.isFailed(registration.mod().modernId))continue;
            if(!cowFamily(legacy))continue; // every other family is admitted (or reported) by AnimalsB173
            String reason=unsupportedBehaviour(legacy);
            if(reason!=null){reject(legacy,reason);continue;}
            try{legacy.getConstructor(World.class);}catch(NoSuchMethodException e){reject(legacy,"missing public World constructor");continue;}
            String path=registration.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]+","_");
            if(path.isEmpty())path="entity_"+registration.id();
            ResourceLocation id=ResourceLocation.fromNamespaceAndPath(registration.mod().modernId,path);
            if(BuiltInRegistries.ENTITY_TYPE.containsKey(id)){reject(legacy,"duplicate entity registry key "+id);continue;}
            EntityType<LegacyB173Cow> type=EntityType.Builder.<LegacyB173Cow>of((t,l)->{
                try{return new LegacyB173Cow(t,l,null);}catch(RuntimeException|LinkageError e){LOG.error("Cannot construct Beta mob {}",legacy.getName(),e);return null;}
            },MobCategory.CREATURE).sized(.9F,1.4F).clientTrackingRange(10).build(id.toString());
            LegacyRegistrar.register(BuiltInRegistries.ENTITY_TYPE,id,type);
            Info info=new Info(registration,type,override(legacy,"interact",legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity.class),override(legacy,"getRandomSound"));
            BY_CLASS.put(legacy,info);BY_TYPE.put(type,info);
            LOG.info("Hosted Beta mob {} as {} (native cow AI/model fallback; original interaction/save and legacy spawn callbacks)",legacy.getName(),id);
        }
    }
    private static void reject(Class<?> type,String reason){if(REPORTED.add(type.getName()+":"+reason))LOG.warn("Beta mob {} not hosted: {}",type.getName(),reason);}
    public static List<Info> all(){return List.copyOf(BY_CLASS.values());}
    public static Info info(EntityType<?> type){return BY_TYPE.get(type);}
    public static boolean spawn(Level level,LivingEntity legacy){
        if(level.isClientSide)return false;
        Info info=BY_CLASS.get(legacy.getClass());
        if(info==null){reject(legacy.getClass(),"no supported registered mob type");return false;}
        if(legacy.mcc$modern!=null)return false;
        LegacyB173Cow host=null;boolean added=false;
        try{host=new LegacyB173Cow(info.type(),level,legacy);added=World.mcc$wrap(level).mcc$spawnNative(host);return added;}
        catch(RuntimeException|LinkageError e){LOG.error("Cannot spawn Beta mob {}",legacy.getClass().getName(),e);return false;}
        finally{if(!added){legacy.mcc$modern=null;if(host!=null)host.discard();}}
    }
    public static byte[] textureBytes(Info info,String name){
        if(info==null||name==null)return null;
        String path=name.startsWith("/")?name.substring(1):name;
        if(path.contains(":")||Arrays.asList(path.replace('\\','/').split("/")).contains(".."))return null;
        try{
            Path file=info.registration().mod().container.getModInfo().getOwningFile().getFile().findResource(path.split("/"));
            return Files.isRegularFile(file)?Files.readAllBytes(file):null;
        }catch(java.io.IOException|RuntimeException e){return null;}
    }
}
