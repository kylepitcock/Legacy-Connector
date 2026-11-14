package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import org.kylepitcock.legacyconnector.core.LegacyLifecycle;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;

/** Bounded original animal lifecycle admission. Client models remain a separate acceptance gate. */
public final class AnimalsB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3/Animals");
    public record Info(ContentB173.Registration registration,EntityType<LegacyB173Animal> type){}
    private static final Map<Class<?>,Info> BY_CLASS=new LinkedHashMap<>();
    private static final Map<EntityType<?>,Info> BY_TYPE=new LinkedHashMap<>();
    private static final Set<String> SUPPORTED=Set.of("lookAt","onKilledOther","tick","tickMovement","tickLiving","interact","damage","attack","getTargetInRange","getPathfindingFavor","applyKnockback","onLanding","markDead","writeNbt","readNbt","canSpawn","canDespawn","getStandingEyeHeight","getPassengerRidingHeight","updatePassengerPosition","getRandomSound","getHurtSound","getDeathSound","onKilledBy","getLimitPerChunk","travel","move","checkWaterCollisions","isOnLadder","isMovementBlocked","isImmobile","isPushable","onCollision","isCollidable","dropItems","getDroppedItemId","getSoundVolume","getTexture","getShadowRadius","canBreatheInWater","processServerEntityStatus","onPlayerInteraction","initDataTracker","canSee","pathingUpdate","resetAttack","getHeldItem","isInsideWall","fall","applyDamage","setOnFire","scheduleVelocityUpdate","getEyeHeight","setPosition","setPositionAndAngles","setPositionAndAnglesAvoidEntities","setRotation","onStruckByLightning","animateSpawn","jump","getMinAmbientSoundDelay","bypassesSteppingEffects","toNbtList","tickRiding","addVelocity","shouldRender");
    private AnimalsB173(){}
    private static boolean api(Class<?> c){return c.getName().startsWith("legacyconnector.")||c.getName().startsWith("legacyconnector_synth.");}
    public static boolean supports(Class<?> c){return LivingEntity.class.isAssignableFrom(c)&&!MobsB173.cowFamily(c)&&unsupportedBehaviour(c)==null;}
    public static String unsupportedBehaviour(Class<?> type){
        return unsupportedBehaviour(type,Set.of());
    }
    static String unsupportedBehaviour(Class<?> type,Set<String> extraSupported){
        Set<String> supported=new HashSet<>(SUPPORTED);supported.addAll(extraSupported);
        Set<String> apiMethods=new HashSet<>();
        for(Class<?> parent=type.getSuperclass();parent!=null&&parent!=Object.class;parent=parent.getSuperclass()){
            if(!api(parent))continue;
            try(var input=parent.getResourceAsStream("/"+parent.getName().replace('.','/')+".class")){
                if(input==null)return "cannot inspect API superclass "+parent.getName();
                new org.objectweb.asm.ClassReader(input).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9){
                    @Override public org.objectweb.asm.MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                        if((access&(org.objectweb.asm.Opcodes.ACC_STATIC|org.objectweb.asm.Opcodes.ACC_PRIVATE))==0&&!name.startsWith("<"))apiMethods.add(name+descriptor);
                        return null;
                    }
                },org.objectweb.asm.ClassReader.SKIP_CODE|org.objectweb.asm.ClassReader.SKIP_DEBUG|org.objectweb.asm.ClassReader.SKIP_FRAMES);
            }catch(java.io.IOException|RuntimeException|LinkageError e){return "cannot inspect API superclass: "+e.getClass().getSimpleName();}
        }
        for(Class<?> c=type;c!=null&&!api(c);c=c.getSuperclass()){
            String[] failure={null};
            try(var input=c.getResourceAsStream("/"+c.getName().replace('.','/')+".class")){
                if(input==null)return "cannot inspect original callbacks";
                new org.objectweb.asm.ClassReader(input).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9){
                    @Override public org.objectweb.asm.MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){
                        if((access&(org.objectweb.asm.Opcodes.ACC_STATIC|org.objectweb.asm.Opcodes.ACC_SYNTHETIC|org.objectweb.asm.Opcodes.ACC_BRIDGE))==0&&apiMethods.contains(name+desc)&&!supported.contains(name))failure[0]="unsupported original override "+name;
                        return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9){
                        @Override public void visitMethodInsn(int opcode,String owner,String called,String descriptor,boolean itf){
                            if((owner.contains("/Missing")||owner.contains("legacyconnector/synth/Missing")||owner.startsWith("legacyconnector_synth/vb1_7_3/net/minecraft/"))&&!supportedVanillaStatic(type,opcode,owner,called,descriptor))failure[0]="unsupported synthesized callback "+owner+"."+called;
                            if(opcode==org.objectweb.asm.Opcodes.INVOKESPECIAL&&owner.startsWith("legacyconnector/vb1_7_3/net/minecraft/")&&!called.equals("<init>")&&!supported.contains(called))failure[0]="unsupported superclass callback "+called;
                        }
                    };}
                },org.objectweb.asm.ClassReader.SKIP_DEBUG|org.objectweb.asm.ClassReader.SKIP_FRAMES);
            }catch(java.io.IOException|RuntimeException|LinkageError e){return "cannot inspect original callbacks: "+e.getClass().getSimpleName();}
            if(failure[0]!=null)return failure[0];
        }
        return null;
    }
    /** Only real typed vanilla field views are exempt from synthesized-method rejection. */
    static boolean supportedVanillaStatic(Class<?> original,int opcode,String owner,String method,String descriptor){
        if(opcode!=org.objectweb.asm.Opcodes.INVOKESTATIC||!method.startsWith("sget$")||!descriptor.startsWith("()L"))return false;
        String prefix="legacyconnector_synth/vb1_7_3/";
        if(!owner.startsWith(prefix)||!owner.endsWith("$$Missing")||(!owner.startsWith(prefix+"net/minecraft/item/")&&!owner.startsWith(prefix+"net/minecraft/block/")))return false;
        String key=owner.substring(prefix.length(),owner.length()-"$$Missing".length()).replace('/','.')+"."+method.substring(5);
        try{
            Class<?> accessor=Class.forName(owner.replace('/','.'),false,original.getClassLoader());
            Method getter=accessor.getDeclaredMethod(method);
            if(!Modifier.isStatic(getter.getModifiers())||!org.objectweb.asm.Type.getMethodDescriptor(getter).equals(descriptor))return false;
            Object value=VanillaB173.resolveStatic(key,accessor);
            return value!=null&&getter.getReturnType().isInstance(value);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError e){return false;}
    }
    public static void registerEvents(IEventBus bus){
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e)->{for(var i:BY_TYPE.values())e.put(i.type(),PathfinderMob.createMobAttributes().build());});
        bus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event)->{
            for(var info:BY_TYPE.values())event.register(info.type(),
                    info.type().getCategory()==MobCategory.WATER_CREATURE?net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER:net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type,level,reason,pos,random)->true,
                    net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
    }
    public static void registerAll(){
        for(var r:ContentB173.entities()){
            Class<?> c=r.type();if(BY_CLASS.containsKey(c)||!LivingEntity.class.isAssignableFrom(c)||MobsB173.cowFamily(c))continue;
            if(r.mod()==null||LegacyLifecycle.isFailed(r.mod().modernId))continue;
            String why=unsupportedBehaviour(c);if(why!=null){LOG.warn("Beta animal {} not hosted: {}",c.getName(),why);continue;}
            why=AnimalRenderStateB173.unsupportedReason(c);
            if(why!=null||!RenderersB173.supports(c)){LOG.warn("Beta animal {} not hosted: {}",c.getName(),why!=null?why:"no supported original renderer");continue;}
            try{c.getConstructor(legacyconnector.vb1_7_3.net.minecraft.world.World.class);}catch(NoSuchMethodException e){LOG.warn("Beta animal {} has no public World constructor",c.getName());continue;}
            String path=r.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]+","_");if(path.isEmpty())path="entity_"+r.id();
            var id=ResourceLocation.fromNamespaceAndPath(r.mod().modernId,path);if(BuiltInRegistries.ENTITY_TYPE.containsKey(id))continue;
            EntityType<LegacyB173Animal> type=EntityType.Builder.<LegacyB173Animal>of((t,l)->new LegacyB173Animal(t,l,null),category(c)).sized(.6F,1.8F).clientTrackingRange(10).build(id.toString());
            LegacyRegistrar.register(BuiltInRegistries.ENTITY_TYPE,id,type);Info info=new Info(r,type);BY_CLASS.put(c,info);BY_TYPE.put(type,info);
            LOG.info("Hosted original Beta animal {} as {} with its original renderer",c.getName(),id);
        }
    }
    public static MobCategory category(Class<?> type){
        MobCategory family=legacyconnector.vb1_7_3.net.minecraft.entity.Monster.class.isAssignableFrom(type)?MobCategory.MONSTER
                :legacyconnector.vb1_7_3.net.minecraft.entity.WaterCreatureEntity.class.isAssignableFrom(type)?MobCategory.WATER_CREATURE
                :AnimalEntity.class.isAssignableFrom(type)?MobCategory.CREATURE:MobCategory.MISC;
        return SpawnRulesB173.categoryFor(type,family);
    }
    public static Info info(EntityType<?> type){return BY_TYPE.get(type);}
    public static List<Info> all(){return List.copyOf(BY_CLASS.values());}
    public static boolean spawn(Level level,LivingEntity legacy){
        Info i=BY_CLASS.get(legacy.getClass());if(level.isClientSide||i==null||legacy.mcc$modern!=null)return false;
        LegacyB173Animal host=null;boolean added=false;
        try{host=new LegacyB173Animal(i.type(),level,legacy);added=legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(level).mcc$spawnNative(host);if(added)EntityViewsB173.spawned(host,legacy);return added;}
        catch(RuntimeException|LinkageError e){LOG.error("Cannot spawn original Beta animal {}",legacy.getClass().getName(),e);return false;}
        finally{if(!added){legacy.mcc$modern=null;if(host!=null)host.discard();}}
    }
    public static byte[] textureBytes(Info info,String name){
        if(info==null||name==null)return null;String path=name.startsWith("/")?name.substring(1):name;
        if(path.contains(":")||Arrays.asList(path.replace('\\','/').split("/")).contains(".."))return null;
        try{Path file=info.registration().mod().container.getModInfo().getOwningFile().getFile().findResource(path.split("/"));return Files.isRegularFile(file)?Files.readAllBytes(file):null;}
        catch(java.io.IOException|RuntimeException e){return null;}
    }
}
