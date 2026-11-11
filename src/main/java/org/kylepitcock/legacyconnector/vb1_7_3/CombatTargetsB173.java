package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.MobEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.mob.MonsterEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.util.math.Box;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Type;

/** Native combat admission at proven original AI sites; ordinary player queries and ownership remain unchanged. */
public final class CombatTargetsB173 {
    private static final String METHOD="mcc$combatRoles";
    private record Roles(Map<String,Field> fields,Set<String> queries,Set<String> captures,boolean canonical){}
    private static final Roles EMPTY=new Roles(Map.of(),Set.of(),Set.of(),false);
    private static final ThreadLocal<Entity> ACTOR=new ThreadLocal<>();
    private static final Map<Entity,WeakReference<Entity>> RETALIATION=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private record Navigation(WeakReference<net.minecraft.world.level.pathfinder.Path> path,WeakReference<net.minecraft.world.entity.Entity> target){}
    private static final Map<Entity,Navigation> NAVIGATION=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private record Capture(WeakReference<Entity> actor,WeakReference<Entity> passenger,WeakReference<net.minecraft.world.entity.Entity> vehicle){}
    private static final Map<Player,Capture> CAPTURES=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private static final Set<Class<?>> REPORTED=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static boolean installed;
    private static final ClassValue<Roles> ROLES=new ClassValue<>(){
        @Override protected Roles computeValue(Class<?> type){
            try{
                String[] rows=new String[0];
                try{var method=type.getDeclaredMethod(METHOD);
                    if(!method.isSynthetic()||!Modifier.isPublic(method.getModifiers())||!Modifier.isStatic(method.getModifiers())||method.getReturnType()!=String[].class)return EMPTY;
                    rows=(String[])method.invoke(null);
                }catch(NoSuchMethodException absent){}
                return roles(type,rows);
            }catch(ReflectiveOperationException|RuntimeException|LinkageError error){
                if(REPORTED.size()<32&&REPORTED.add(type))org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Cannot validate original combat roles for {}",type.getName(),error);
                return EMPTY;
            }
        }
    };
    private CombatTargetsB173(){}
    static Roles roles(Class<?> type,String[] rows)throws ReflectiveOperationException{
        if(rows==null||rows.length>256)throw new IllegalArgumentException("Oversized original combat roles");
        var fields=new LinkedHashMap<String,Field>();var queries=new HashSet<String>();var captures=new HashSet<String>();int bytes=0;
        for(String row:rows){
            if(row==null||(bytes+=row.length()*2)>16_384)throw new IllegalArgumentException("Oversized original combat role");
            String[] parts=row.split("\\|",-1);
            if(parts.length==2&&(parts[0].equals("Q")||parts[0].equals("C"))){
                if(parts[1].isEmpty()||parts[1].length()>1024)throw new IllegalArgumentException("Invalid combat site");
                (parts[0].equals("Q")?queries:captures).add(parts[1]);continue;
            }
            if(parts.length!=4||!parts[0].equals("F"))throw new IllegalArgumentException("Invalid combat field role");
            Field found=null;
            for(Class<?> current=type;current!=null&&Entity.class.isAssignableFrom(current);current=current.getSuperclass())
                if(Type.getInternalName(current).equals(parts[1])){found=current.getDeclaredField(parts[2]);break;}
            if(found==null||Modifier.isStatic(found.getModifiers())||!Entity.class.isAssignableFrom(found.getType())||!Type.getDescriptor(found.getType()).equals(parts[3]))throw new IllegalArgumentException("Invalid original combat field");
            // The SDK's canonical target is the sole API relationship field that can be a combat role.
            if(found.getDeclaringClass().getName().startsWith("legacyconnector.")&&!(found.getDeclaringClass()==MobEntity.class&&found.getName().equals("target")))throw new IllegalArgumentException("Combat role cannot own a shared SDK relationship");
            if(!found.trySetAccessible())throw new IllegalArgumentException("Inaccessible combat field");
            String key=parts[1]+"|"+parts[2]+"|"+parts[3];if(fields.putIfAbsent(key,found)!=null)throw new IllegalArgumentException("Duplicate combat field");
        }
        boolean canonical=false;
        if(MonsterEntity.class.isAssignableFrom(type)){
            for(Class<?> current=type;current!=null;current=current.getSuperclass())try{
                current.getDeclaredMethod("getTargetInRange");canonical=current==MonsterEntity.class;break;
            }catch(NoSuchMethodException absent){}
        }
        return new Roles(Map.copyOf(fields),Set.copyOf(queries),Set.copyOf(captures),canonical);
    }
    public static synchronized void install(){
        if(installed)return;installed=true;var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST,false,(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event)->{
            if(!(event.getEntity() instanceof B173EntityHost host)||host.legacy()==null||!owned(host.legacy()))return;
            var target=event.getNewAboutToBeSetTarget();
            if(target instanceof Player player&&!playerAllowed(player)&&provenTarget(host.legacy(),target))event.setNewAboutToBeSetTarget(null);
        });
        bus.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event)->{
            if(!event.getEntity().level().isClientSide)playerTick(event.getEntity());
        });
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->{RETALIATION.clear();NAVIGATION.clear();CAPTURES.clear();REPORTED.clear();ACTOR.remove();});
    }
    public static final class Scope implements AutoCloseable {
        private final Entity previous;private boolean closed;
        private Scope(Entity actor){previous=ACTOR.get();ACTOR.set(actor);}
        @Override public void close(){if(closed)return;closed=true;if(previous==null)ACTOR.remove();else ACTOR.set(previous);}
    }
    /** A null actor explicitly masks an enclosing AI scope during a manual/native utility callback. */
    public static Scope enter(Entity actor){return new Scope(actor);}
    /** Physical callbacks continue only their own already active autonomous callback. */
    public static Scope continuation(Entity actor){return new Scope(ACTOR.get()==actor?actor:null);}
    private static boolean owned(Entity actor){
        return actor!=null&&actor.mcc$modern instanceof B173EntityHost host&&host.legacy()==actor
                &&actor.mcc$modern.level()!=null&&!actor.mcc$modern.level().isClientSide;
    }
    static boolean playerAllowed(Player player){return !player.isCreative()&&!player.isSpectator()&&player.canBeSeenAsEnemy();}
    public static boolean allowed(Entity actor,Entity candidate){
        return !owned(actor)||candidate==null||!(candidate.mcc$modern instanceof Player player)||playerAllowed(player);
    }
    private static boolean sameWorld(Entity actor,World world){return owned(actor)&&world!=null&&world.mcc$level()==actor.mcc$modern.level();}
    private static boolean site(World world,String site,boolean capture){
        var actor=ACTOR.get();if(!sameWorld(actor,world))return false;var roles=ROLES.get(actor.getClass());
        return (capture?roles.captures():roles.queries()).contains(site);
    }
    /** Canonical SDK Monster acquisition retains the original virtual raw squared-distance hook. */
    public static PlayerEntity monsterClosest(MonsterEntity actor,double range){
        if(actor.world==null)return null;
        return owned(actor)?nearest(actor.world.players,actor.x,actor.y,actor.z,range,p->allowed(actor,p)):actor.world.getClosestPlayer(actor,range);
    }
    static PlayerEntity nearest(Iterable<?> players,double x,double y,double z,double range,java.util.function.Predicate<PlayerEntity> admission){
        PlayerEntity closest=null;double best=range<0?Double.POSITIVE_INFINITY:range*range;
        for(Object object:players)if(object instanceof PlayerEntity player&&admission.test(player)){
            double distance=player.getSquaredDistance(x,y,z);if(distance<best){best=distance;closest=player;}
        }
        return closest;
    }
    public static PlayerEntity closest(World world,Entity center,double range,String site){
        var actor=ACTOR.get();return site(world,site,false)?nearest(world.players,center.x,center.y,center.z,range,p->allowed(actor,p)):world.getClosestPlayer(center,range);
    }
    public static PlayerEntity closest(World world,double x,double y,double z,double range,String site){
        var actor=ACTOR.get();return site(world,site,false)?nearest(world.players,x,y,z,range,p->allowed(actor,p)):world.getClosestPlayer(x,y,z,range);
    }
    public static List<Entity> entities(World world,Entity except,Box box,String site){
        var result=world.getEntities(except,box);if(site(world,site,false)){var actor=ACTOR.get();result.removeIf(value->!allowed(actor,value));}return result;
    }
    public static List<Entity> typedEntities(World world,Class<?> type,Box box,String site){
        var result=world.collectEntitiesByClass(type,box);if(site(world,site,false)){var actor=ACTOR.get();result.removeIf(value->!allowed(actor,value));}return result;
    }
    public static Entity stored(Entity actor,Entity candidate,String owner,String name,String descriptor){
        if(ACTOR.get()!=actor||!owned(actor)||!ROLES.get(actor.getClass()).fields().containsKey(owner+"|"+name+"|"+descriptor))return candidate;
        return allowed(actor,candidate)?candidate:null;
    }
    public static Entity retaliation(MonsterEntity actor,Entity candidate){
        if(candidate==null){RETALIATION.remove(actor);return null;}
        if(!allowed(actor,candidate))return null;
        if(owned(actor)&&candidate!=null)RETALIATION.put(actor,new WeakReference<>(candidate));
        return candidate;
    }
    private static boolean provenTarget(Entity actor,net.minecraft.world.entity.Entity target){
        var roles=ROLES.get(actor.getClass());if(roles.canonical())return true;
        if(actor instanceof MobEntity mob&&mob.target!=null&&mob.target.mcc$modern==target){
            var retaliation=RETALIATION.get(actor);if(retaliation!=null&&retaliation.get()==mob.target)return true;
        }
        try{for(var field:roles.fields().values()){var value=(Entity)field.get(actor);if(value!=null&&value.mcc$modern==target)return true;}}
        catch(IllegalAccessException error){throw new IllegalStateException(error);}
        return false;
    }
    /** Called after a proven standard-AI navigation request; a foreign replacement path is never stopped. */
    public static void navigation(Entity actor,Entity target){
        if(!owned(actor)||target==null||target.mcc$modern==null||!provenTarget(actor,target.mcc$modern)||!(actor.mcc$modern instanceof net.minecraft.world.entity.Mob mob))return;
        var path=mob.getNavigation().getPath();
        if(path==null){NAVIGATION.remove(actor);return;}
        NAVIGATION.put(actor,new Navigation(new WeakReference<>(path),new WeakReference<>(target.mcc$modern)));
    }
    public static void beforeAI(Entity actor){
        if(!owned(actor))return;
        var roles=ROLES.get(actor.getClass());
        try{for(var field:roles.fields().values()){var value=(Entity)field.get(actor);if(value!=null&&!allowed(actor,value)){field.set(actor,null);}}}
        catch(IllegalAccessException error){throw new IllegalStateException(error);}
        if(actor instanceof MobEntity mob&&mob.target!=null&&!allowed(actor,mob.target)){
            var retaliation=RETALIATION.get(actor);
            if(roles.canonical()||retaliation!=null&&retaliation.get()==mob.target){mob.target=null;RETALIATION.remove(actor);}
        }
        var lease=NAVIGATION.get(actor);
        if(lease!=null){
            var target=lease.target().get();var path=lease.path().get();
            if(target==null||path==null)NAVIGATION.remove(actor);
            else if(target instanceof Player player&&!playerAllowed(player)){
                if(actor.mcc$modern instanceof net.minecraft.world.entity.Mob mob&&mob.getNavigation().getPath()==path)mob.getNavigation().stop();
                NAVIGATION.remove(actor);
            }
        }
        for(var entry:List.copyOf(CAPTURES.entrySet()))if(entry.getValue().actor().get()==actor)releaseCapture(entry.getKey(),entry.getValue());
    }
    /** Only a loader-proven autonomous capture can create a release ledger entry. */
    public static void capture(Entity passenger,Entity vehicle,String site){
        var actor=ACTOR.get();boolean proven=vehicle==actor&&site(actor==null?null:actor.world,site,true);
        var previousVehicle=passenger.vehicle;
        boolean releasing=vehicle!=null&&previousVehicle==vehicle;
        if(proven&&!releasing&&!allowed(actor,passenger))return;
        var nativePlayer=passenger.mcc$modern instanceof Player player?player:null;
        var previousCapture=nativePlayer==null?null:CAPTURES.get(nativePlayer);
        passenger.setVehicle(vehicle);
        if(previousCapture!=null&&previousVehicle!=null&&previousCapture.actor().get()==previousVehicle
                &&previousCapture.passenger().get()==passenger&&previousCapture.vehicle().get()==previousVehicle.mcc$modern
                &&passenger.vehicle!=previousVehicle&&nativePlayer.getVehicle()!=previousVehicle.mcc$modern)
            CAPTURES.remove(nativePlayer,previousCapture);
        if(proven&&!releasing&&nativePlayer!=null&&vehicle.mcc$modern!=null&&nativePlayer.getVehicle()==vehicle.mcc$modern)
            CAPTURES.put(nativePlayer,new Capture(new WeakReference<>(actor),new WeakReference<>(passenger),new WeakReference<>(vehicle.mcc$modern)));
    }
    static void playerTick(Player player){var entry=CAPTURES.get(player);if(entry!=null)releaseCapture(player,entry);}
    private static void releaseCapture(Player player,Capture entry){
        var actor=entry.actor().get();var passenger=entry.passenger().get();var vehicle=entry.vehicle().get();
        if(!owned(actor)||passenger==null||vehicle==null||vehicle!=actor.mcc$modern||passenger.mcc$modern!=player||player.getVehicle()!=vehicle){CAPTURES.remove(player,entry);return;}
        if(allowed(actor,passenger))return;
        player.stopRiding();
        if(passenger.vehicle==actor)passenger.vehicle=null;
        if(actor.passenger==passenger)actor.passenger=null;
        CAPTURES.remove(player,entry);
    }
}
