package org.kylepitcock.legacyconnector.vb1_7_3;

import java.lang.reflect.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Actual original collision branches; private mod jars are runtime inputs only. */
public final class MoreCreepsEffectsSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173CreepsEffectsTest");
    private MoreCreepsEffectsSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 CREEPS EFFECTS TEST failed",error);}});}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException(detail);}
    private static Field field(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {field(object,name).set(object,value);}
    private static float number(Object object,String name)throws ReflectiveOperationException{return ((Number)field(object,name).get(object)).floatValue();}
    private static net.minecraft.server.level.ServerPlayer actor(ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("legacyconnector:creepseffects".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"B173CreepsEffect");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        return player;
    }

    private static Object get(Object object,String name)throws ReflectiveOperationException{return field(object,name).get(object);}
    private static final class FixedRandom extends Random {@Override public int nextInt(int bound){return 0;}@Override public float nextFloat(){return .5F;}}
    private static final class Context implements AutoCloseable {
        final ServerLevel level;final World world;final ContentB173.Registration registration;final net.minecraft.server.level.ServerPlayer player;
        final Set<Integer> before=new HashSet<>();final net.minecraft.core.BlockPos origin;final List<Object> previousPlayers;
        Context(ServerLevel level,ContentB173.Registration registration)throws ReflectiveOperationException {
            this.level=level;this.registration=registration;world=World.mcc$wrap(level);for(var e:level.getAllEntities())before.add(e.getId());
            origin=level.getSharedSpawnPos().offset(8,36,8);level.getChunk(origin);player=actor(level);put(player,"spawnInvulnerableTime",0);player.setPos(origin.getX()+1.5,origin.getY(),origin.getZ()+.5);
            previousPlayers=world.players;world.players=new ArrayList<>();try(var scope=EntityViewsB173.callbackScope()){world.players.add(EntityViewsB173.wrap(player));}
        }
        Class<?> type(String name)throws ClassNotFoundException{return Class.forName(registration.type().getPackageName()+"."+name,true,registration.type().getClassLoader());}
        LegacyB173Animal spawn(String name)throws ReflectiveOperationException {
            var original=(LivingEntity)type(name).getConstructor(World.class).newInstance(world);original.setPosition(origin.getX()+.5,origin.getY(),origin.getZ()+.5);
            require(world.spawnEntity(original)&&original.mcc$modern instanceof LegacyB173Animal,"original remaining-case spawn uses shared host: "+name);EntityViewsB173.flush();put(original,"random",new FixedRandom());
            require(level.getEntities((net.minecraft.world.entity.Entity)null,original.mcc$modern.getBoundingBox().inflate(.1)).contains(original.mcc$modern),"original enters loaded query sections");return (LegacyB173Animal)original.mcc$modern;
        }
        void movement(LegacyB173Animal host){try(var scope=EntityViewsB173.callbackScope()){EntityViewsB173.wrap(player);MenusB173.withActor(player,host::aiStep);}}
        @Override public void close(){EntityViewsB173.flush();world.players=previousPlayers;player.stopRiding();player.getInventory().clearContent();var added=new ArrayList<net.minecraft.world.entity.Entity>();for(var e:level.getAllEntities())if(!before.contains(e.getId()))added.add(e);added.forEach(net.minecraft.world.entity.Entity::discard);player.discard();EntityViewsB173.flush();}
    }

    /** Capture the exact native writes, including block entity NBT, so original structures leave no fixture blocks. */
    private static final class Blocks implements AutoCloseable {
        final ServerLevel level;final boolean capturing,restoring;final int start;
        Blocks(ServerLevel level){this.level=level;capturing=level.captureBlockSnapshots;restoring=level.restoringBlockSnapshots;start=level.capturedBlockSnapshots.size();level.captureBlockSnapshots=true;}
        @Override public void close(){
            var snapshots=new ArrayList<>(level.capturedBlockSnapshots.subList(start,level.capturedBlockSnapshots.size()));level.capturedBlockSnapshots.subList(start,level.capturedBlockSnapshots.size()).clear();level.captureBlockSnapshots=false;level.restoringBlockSnapshots=true;
            try{boolean restored=true;for(int i=snapshots.size()-1;i>=0;i--){var snapshot=snapshots.get(i);if(!snapshot.restore(2))snapshot.restoreBlockEntity(level,snapshot.getPos());restored&=level.getBlockState(snapshot.getPos()).equals(snapshot.getState());}require(restored,"restore original effect block snapshots");}finally{level.captureBlockSnapshots=capturing;level.restoringBlockSnapshots=restoring;}
        }
    }
    private static void jailTeleport(Context c)throws ReflectiveOperationException {
        var lawyer=c.spawn("CREEPSEntityLawyerFromHell");var mod=c.registration.mod().instance;
        var saved=new LinkedHashMap<Field,Object>();for(String name:List.of("jailBuilt","currentJailX","currentJailY","currentJailZ","currentfine")){var f=field(mod,name);saved.put(f,f.get(null));}
        var jail=c.origin.offset(1,12,1);for(int x=-1;x<=18;x+=4)for(int z=-1;z<=15;z+=4)c.level.getChunk(jail.offset(x,0,z));
        try(var blocks=new Blocks(c.level)){
            // Reuse-site input bypasses terrain suitability and randomized container loot, retaining original jail construction/teleport.
            put(mod,"jailBuilt",true);put(mod,"currentJailX",jail.getX());put(mod,"currentJailY",jail.getY());put(mod,"currentJailZ",jail.getZ());put(mod,"currentfine",3000);put(lawyer.legacy(),"area",0);
            final boolean[] built={false};
            try(var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(c.world,PlayerViewB173.of(c.player));var views=EntityViewsB173.callbackScope()){
                MenusB173.withActor(c.player,()->{try{built[0]=(Boolean)lawyer.legacy().getClass().getMethod("buildJail").invoke(lawyer.legacy());}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}});
            }
            require(built[0],"actual Lawyer builds controlled loaded jail site");
            double eye;try(var views=EntityViewsB173.callbackScope()){eye=EntityViewsB173.wrap(c.player).standingEyeHeight;}
            require(c.player.position().distanceTo(new net.minecraft.world.phys.Vec3(jail.getX()+7,jail.getY()+2-eye,jail.getZ()+7))<.000001,"actual Lawyer jail callback teleports real native player to original cell coordinates: "+c.player.position());
            require(c.level.capturedBlockSnapshots.size()>blocks.start,"actual Lawyer jail callback constructs native cell blocks");
        }finally{for(var entry:saved.entrySet())entry.getKey().set(null,entry.getValue());}
    }
    private static void camelSeat(Context c)throws ReflectiveOperationException {
        var camel=c.spawn("CREEPSEntityCamel");require(c.player.startRiding(camel,true),"real player mounts actual Camel for native rider-placement dispatch");
        double eye;try(var views=EntityViewsB173.callbackScope()){eye=EntityViewsB173.wrap(c.player).standingEyeHeight;}
        double x=camel.legacy().x,y=camel.legacy().y+5-eye,z=camel.legacy().z;
        c.player.setPos(camel.getX()+2,camel.getY()+1,camel.getZ()+2);camel.positionRider(c.player);
        require(c.player.position().distanceTo(new net.minecraft.world.phys.Vec3(x,y,z))<.000001,"native positionRider dispatches actual Camel.updatePassengerPosition custom seat with Beta eye normalization: "+c.player.position()+" expected="+new net.minecraft.world.phys.Vec3(x,y,z));
    }
    private static void scientistExperiment(Context c)throws ReflectiveOperationException {
        var scientist=c.spawn("CREEPSEntityEvilScientist");var original=scientist.legacy();int health=(Integer)get(original,"basehealth");
        put(original,"stage",4);put(original,"numexperiments",1);put(original,"towerX",c.origin.getX());put(original,"towerY",c.origin.getY());put(original,"towerZ",c.origin.getZ());put(original,"towerHeight",3);scientist.flushView();
        var before=new HashSet<Integer>();for(var e:c.level.getAllEntities())before.add(e.getId());
        try(var blocks=new Blocks(c.level)){c.movement(scientist);}
        require((Boolean)get(original,"trulyevil")&&(Integer)get(original,"basehealth")==health+50&&scientist.getHealth()==health+50,"actual scientist completed experiment changes form and raises native health by fifty");
        var summons=new ArrayList<LegacyB173Animal>();for(var e:c.level.getAllEntities())if(!before.contains(e.getId())&&e instanceof LegacyB173Animal child&&child.legacy().getClass().getSimpleName().equals("CREEPSEntityEvilSnowman"))summons.add(child);
        require(summons.size()==2,"actual deterministic scientist experiment summons two original EvilSnowmen through shared hosts: "+summons.size());
        for(var child:summons)require(child.legacy().mcc$modern==child&&!child.isRemoved(),"actual experiment summons retain exact live original/native identities");
    }
    private static void placementMerge(Context c){
        var start=c.player.position();
        try(var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(c.world,PlayerViewB173.of(c.player));var views=EntityViewsB173.callbackScope()){
            EntityViewsB173.wrap(c.player);
            c.player.setPos(start.x+2,start.y+3,start.z+4);c.player.setYRot(31);c.player.setXRot(-17);
        }
        require(c.player.position().distanceTo(start.add(2,3,4))<.000001&&c.player.getYRot()==31&&c.player.getXRot()==-17,"read-only ordinary view preserves unrelated later native position and rotation");
        start=c.player.position();
        try(var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(c.world,PlayerViewB173.of(c.player));var views=EntityViewsB173.callbackScope()){
            var view=EntityViewsB173.wrap(c.player);view.y=start.y+view.standingEyeHeight+5;view.pitch=23;
            c.player.setPos(start.x+6,start.y,start.z+7);c.player.setYRot(49);
        }
        require(c.player.position().distanceTo(start.add(6,5,7))<.000001&&c.player.getYRot()==49&&c.player.getXRot()==23,"ordinary placement merges original Y/pitch writes with later native X/Z/yaw changes");
    }
    private static void run(ServerLevel level){
        var registration=ContentB173.entities().stream().filter(r->r.type().getSimpleName().equals("CREEPSEntityLawyerFromHell")&&r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")).findFirst();if(registration.isEmpty())return;
        var failures=new ArrayList<String>();int passed=0;
        for(String name:List.of("Lawyer jail teleport","Camel native seat","Scientist experiment","ordinary placement merge"))try(var c=new Context(level,registration.get())){
            switch(name){case "Lawyer jail teleport"->jailTeleport(c);case "Camel native seat"->camelSeat(c);case "Scientist experiment"->scientistExperiment(c);default->placementMerge(c);}
            passed++;LOG.info("B1.7.3 CREEPS EFFECTS TEST: {} original specialized branch passed",name);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures.add(name+": "+error);LOG.error("B1.7.3 CREEPS EFFECTS TEST failed: {}",name,error);}
        require(failures.isEmpty(),"B1.7.3 CREEPS EFFECTS TEST: "+failures);LOG.info("B1.7.3 CREEPS EFFECTS TEST: all {} actual specialized branches passed",passed);
    }
}
