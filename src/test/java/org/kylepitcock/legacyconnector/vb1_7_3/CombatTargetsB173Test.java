package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;

class CombatTargetsB173Test {
    private static net.minecraft.server.level.ServerLevel LEVEL;
    private static int classes;
    @BeforeAll static void bootstrap()throws Exception {NativeStackComponentsB173Test.bootstrap();LEVEL=blank(net.minecraft.server.level.ServerLevel.class);}
    private static <T>T blank(Class<T> type)throws Exception {
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe)field.get(null)).allocateInstance(type));
    }
    public static class FixtureWorld extends World {
        @Override public net.minecraft.world.level.Level mcc$level(){return LEVEL;}
        @Override public List<Entity> getEntities(Entity except,legacyconnector.vb1_7_3.net.minecraft.util.math.Box box){
            var result=new ArrayList<Entity>();for(Object value:players)if(value!=except)result.add((Entity)value);return result;
        }
        @Override public List<Entity> collectEntitiesByClass(Class<?> type,legacyconnector.vb1_7_3.net.minecraft.util.math.Box box){
            var result=getEntities(null,box);result.removeIf(value->!type.isInstance(value));return result;
        }
    }
    public static class Actor extends legacyconnector.vb1_7_3.net.minecraft.entity.passive.AnimalEntity {
        public Entity prey,owner;
        public Actor(){super(new FixtureWorld());}
    }
    public static class Canonical extends legacyconnector.vb1_7_3.net.minecraft.entity.mob.MonsterEntity {
        public Canonical(){super(new FixtureWorld());}
    }
    public static class NativePlayer extends net.minecraft.server.level.ServerPlayer {
        boolean creative,spectator,enemy=true;int dismounts;net.minecraft.world.entity.Entity riding;Runnable onMount;
        private NativePlayer(){super(null,null,null,null);}
        @Override public boolean isCreative(){return creative;}
        @Override public boolean isSpectator(){return spectator;}
        @Override public boolean canBeSeenAsEnemy(){return enemy;}
        @Override public net.minecraft.world.entity.Entity getVehicle(){return riding;}
        @Override public boolean startRiding(net.minecraft.world.entity.Entity entity,boolean force){riding=entity;if(onMount!=null)onMount.run();return true;}
        @Override public void stopRiding(){dismounts++;riding=null;}
    }
    public static class PlayerView extends PlayerEntity {
        int distances;double distanceScale=1;
        PlayerView(World world){super(world);}
        @Override public double getSquaredDistance(double x,double y,double z){distances++;return super.getSquaredDistance(x,y,z)*distanceScale;}
    }
    public static class Host extends net.minecraft.world.entity.Entity implements B173EntityHost {
        Entity original;
        private Host(){super(net.minecraft.world.entity.EntityType.MARKER,null);}
        @Override public net.minecraft.world.level.Level level(){return LEVEL;}
        @Override public Entity legacy(){return original;}
        @Override public boolean callbackActive(){return false;}
        @Override public void syncView(){}
        @Override public void flushView(){}
        @Override public void mcc$baseTick(){}
        @Override public int baseTickCount(){return 0;}
        @Override public void baseMove(double x,double y,double z){}
        @Override public boolean baseDamage(Entity source,int amount){return false;}
        @Override public void baseMarkDead(){}
        @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder){}
        @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){}
        @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){}
    }
    private static <T extends Entity>T owned(T actor)throws Exception {var host=blank(Host.class);host.original=actor;actor.mcc$modern=host;return actor;}
    private static PlayerView player(World world,double x,boolean creative,boolean spectator)throws Exception {
        var nativePlayer=blank(NativePlayer.class);nativePlayer.creative=creative;nativePlayer.spectator=spectator;nativePlayer.enemy=true;
        var player=new PlayerView(world);player.mcc$modern=nativePlayer;player.setPosition(x,0,0);world.players.add(player);return player;
    }
    private static Actor proven()throws Exception {
        String name="combatfixture/Actor"+classes++;
        var writer=new ClassWriter(0);writer.visit(61,Opcodes.ACC_PUBLIC,name,null,Type.getInternalName(Actor.class),null);
        var constructor=writer.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);constructor.visitCode();constructor.visitVarInsn(Opcodes.ALOAD,0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL,Type.getInternalName(Actor.class),"<init>","()V",false);constructor.visitInsn(Opcodes.RETURN);constructor.visitMaxs(1,1);constructor.visitEnd();
        var method=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC|Opcodes.ACC_SYNTHETIC,"mcc$combatRoles","()[Ljava/lang/String;",null,null);
        method.visitCode();method.visitInsn(Opcodes.ICONST_3);method.visitTypeInsn(Opcodes.ANEWARRAY,"java/lang/String");
        String[] rows={"F|"+Type.getInternalName(Actor.class)+"|prey|Llegacyconnector/vb1_7_3/net/minecraft/entity/Entity;","Q|combat-site","C|capture-site"};
        for(int index=0;index<rows.length;index++){method.visitInsn(Opcodes.DUP);method.visitIntInsn(Opcodes.BIPUSH,index);method.visitLdcInsn(rows[index]);method.visitInsn(Opcodes.AASTORE);}
        method.visitInsn(Opcodes.ARETURN);method.visitMaxs(4,0);method.visitEnd();writer.visitEnd();
        class FixtureLoader extends ClassLoader {FixtureLoader(){super(CombatTargetsB173Test.class.getClassLoader());}Class<?> define(byte[] bytes){return defineClass(null,bytes,0,bytes.length);}}
        return owned((Actor)new FixtureLoader().define(writer.toByteArray()).getConstructor().newInstance());
    }
    @Test void provenQueriesSelectFartherEligiblePlayerAndPreserveCloakRawDistanceAndOrdinaryQueries()throws Exception {
        var actor=proven();var creative=player(actor.world,1,true,false);var spectator=player(actor.world,2,false,true);
        var cloaked=player(actor.world,3,false,false);cloaked.distanceScale=1000;var survival=player(actor.world,4,false,false);
        assertSame(creative,actor.world.getClosestPlayer(actor,32));
        try(var scope=CombatTargetsB173.enter(actor)){
            assertSame(survival,CombatTargetsB173.closest(actor.world,actor,32,"combat-site"));
            assertTrue(cloaked.distances>0);
            assertSame(creative,CombatTargetsB173.closest(actor.world,actor,32,"ordinary-site"));
            assertEquals(List.of(cloaked,survival),CombatTargetsB173.entities(actor.world,null,null,"combat-site"));
            assertEquals(List.of(cloaked,survival),CombatTargetsB173.typedEntities(actor.world,PlayerEntity.class,null,"combat-site"));
        }
        assertSame(creative,CombatTargetsB173.closest(actor.world,actor,32,"combat-site"));
        assertTrue(actor.world.getEntities(null,null).contains(spectator));
    }
    @Test void modeChangeClearsOnlyProvenCombatFieldAndCanonicalTargetWithoutChangingOwnerFollow()throws Exception {
        var actor=proven();var target=player(actor.world,1,false,false);actor.prey=target;actor.owner=target;actor.target=target;
        CombatTargetsB173.beforeAI(actor);assertSame(target,actor.prey);
        ((NativePlayer)target.mcc$modern).creative=true;CombatTargetsB173.beforeAI(actor);
        assertNull(actor.prey);assertSame(target,actor.owner);assertSame(target,actor.target);
        String owner=Type.getInternalName(Actor.class),descriptor="Llegacyconnector/vb1_7_3/net/minecraft/entity/Entity;";
        try(var scope=CombatTargetsB173.enter(actor)){
            assertNull(CombatTargetsB173.stored(actor,target,owner,"prey",descriptor));
            assertSame(target,CombatTargetsB173.stored(actor,target,owner,"owner",descriptor));
            var another=proven();assertSame(target,CombatTargetsB173.stored(another,target,owner,"prey",descriptor));
        }
        var monster=owned(new Canonical());monster.target=target;CombatTargetsB173.beforeAI(monster);assertNull(monster.target);
        assertNull(CombatTargetsB173.retaliation(monster,target));
        ((NativePlayer)target.mcc$modern).creative=false;assertSame(target,CombatTargetsB173.retaliation(monster,target));
        monster.target=target;((NativePlayer)target.mcc$modern).spectator=true;CombatTargetsB173.beforeAI(monster);assertNull(monster.target);
    }
    @Test void onlyRecordedAutonomousCaptureIsReleasedAndChangedVehicleRemainsUntouched()throws Exception {
        var actor=proven();var player=player(actor.world,1,false,false);var nativePlayer=(NativePlayer)player.mcc$modern;
        try(var scope=CombatTargetsB173.enter(actor)){CombatTargetsB173.capture(player,actor,"capture-site");}
        assertSame(actor.mcc$modern,nativePlayer.getVehicle());nativePlayer.creative=true;CombatTargetsB173.playerTick(nativePlayer);
        assertNull(nativePlayer.getVehicle());assertNull(player.vehicle);assertNull(actor.passenger);assertEquals(1,nativePlayer.dismounts);
        player.setVehicle(actor);CombatTargetsB173.beforeAI(actor);
        assertSame(actor.mcc$modern,nativePlayer.getVehicle());assertEquals(1,nativePlayer.dismounts);
        player.setVehicle(null);nativePlayer.creative=false;
        try(var scope=CombatTargetsB173.enter(actor)){CombatTargetsB173.capture(player,actor,"capture-site");}
        var other=owned(new Actor());player.setVehicle(other);nativePlayer.spectator=true;int before=nativePlayer.dismounts;
        CombatTargetsB173.beforeAI(actor);assertSame(other.mcc$modern,nativePlayer.getVehicle());assertEquals(before,nativePlayer.dismounts);
    }
    @Test void malformedRolesCannotClaimSharedSdkRelationshipsAndUnhostedActorsRemainUnchanged()throws Exception {
        assertThrows(IllegalArgumentException.class,()->CombatTargetsB173.roles(Actor.class,new String[]{"F|legacyconnector/vb1_7_3/net/minecraft/entity/Entity|passenger|Llegacyconnector/vb1_7_3/net/minecraft/entity/Entity;"}));
        assertThrows(IllegalArgumentException.class,()->CombatTargetsB173.roles(Actor.class,new String[]{"F|"+Type.getInternalName(Actor.class)+"|prey|Ljava/lang/String;"}));
        var ordinary=new Actor();var creative=player(ordinary.world,1,true,false);
        assertTrue(CombatTargetsB173.allowed(ordinary,creative));
        try(var scope=CombatTargetsB173.enter(ordinary)){assertSame(creative,CombatTargetsB173.closest(ordinary.world,ordinary,32,"combat-site"));}
    }
    @Test void nativeEnemyEligibilityAndIndependentCaptureEdgesAreRespected()throws Exception {
        var actor=proven();var hidden=player(actor.world,1,false,false);var farther=player(actor.world,3,false,false);
        ((NativePlayer)hidden.mcc$modern).enemy=false;
        try(var scope=CombatTargetsB173.enter(actor)){
            assertSame(farther,CombatTargetsB173.closest(actor.world,actor,32,"combat-site"));
            CombatTargetsB173.capture(hidden,actor,"capture-site");
            assertNull(((NativePlayer)hidden.mcc$modern).getVehicle());
        }
        var secondActor=proven();var second=player(secondActor.world,2,false,false);
        try(var scope=CombatTargetsB173.enter(actor)){CombatTargetsB173.capture(farther,actor,"capture-site");}
        try(var scope=CombatTargetsB173.enter(secondActor)){CombatTargetsB173.capture(second,secondActor,"capture-site");}
        var firstNative=(NativePlayer)farther.mcc$modern;firstNative.spectator=true;
        CombatTargetsB173.playerTick(firstNative);
        assertNull(firstNative.getVehicle());
        assertSame(secondActor.mcc$modern,((NativePlayer)second.mcc$modern).getVehicle());
    }
    @Test void fieldManifestUsesTheSameTotalRecordBoundAsTheLoader()throws Exception {
        String name="combatfixture/ManyFields"+classes++,descriptor=Type.getDescriptor(Entity.class);
        var writer=new ClassWriter(0);writer.visit(61,Opcodes.ACC_PUBLIC,name,null,Type.getInternalName(Actor.class),null);
        var rows=new String[33];
        for(int index=0;index<rows.length;index++){
            String field="target"+index;writer.visitField(Opcodes.ACC_PUBLIC,field,descriptor,null,null).visitEnd();
            rows[index]="F|"+name+"|"+field+"|"+descriptor;
        }
        writer.visitEnd();
        class FixtureLoader extends ClassLoader {FixtureLoader(){super(CombatTargetsB173Test.class.getClassLoader());}Class<?> define(byte[] bytes){return defineClass(null,bytes,0,bytes.length);}}
        Class<?> type=new FixtureLoader().define(writer.toByteArray());
        assertDoesNotThrow(()->{CombatTargetsB173.roles(type,rows);});
        assertThrows(IllegalArgumentException.class,()->CombatTargetsB173.roles(type,new String[257]));
    }
    private static PlayerEntity sharedQuery(Actor actor){return CombatTargetsB173.closest(actor.world,actor,32,"combat-site");}
    @Test void riderCallbackWaitsForOriginalPublicationAndRejectsReentrantModeRelease()throws Exception {
        var actor=proven();var rider=player(actor.world,1,false,false);var nativeRider=(NativePlayer)rider.mcc$modern;
        var passengers=net.minecraft.world.entity.Entity.class.getDeclaredField("passengers");passengers.setAccessible(true);
        passengers.set(actor.mcc$modern,com.google.common.collect.ImmutableList.of(nativeRider));
        int[] calls={0};
        nativeRider.onMount=()->{if(RiderPositionB173.attached(actor,nativeRider))calls[0]++;};
        try(var ai=CombatTargetsB173.enter(actor)){CombatTargetsB173.capture(rider,actor,"capture-site");}
        assertEquals(0,calls[0],"native eager rider callback precedes original link publication");
        assertTrue(RiderPositionB173.attached(actor,nativeRider));
        nativeRider.riding=null;
        assertFalse(RiderPositionB173.attached(actor,nativeRider),"stale host list and original links cannot replace the native backlink");
        nativeRider.riding=actor.mcc$modern;
        nativeRider.creative=true;CombatTargetsB173.playerTick(nativeRider);
        assertFalse(RiderPositionB173.attached(actor,nativeRider),"even a stale native passenger list cannot admit a released original edge");
        assertNull(rider.vehicle);assertNull(actor.passenger);
        nativeRider.onMount=null;rider.setVehicle(actor);
        assertTrue(RiderPositionB173.attached(actor,nativeRider),"explicit Creative mount remains valid");
    }
    @Test void physicalContinuationRequiresTheSameUnmaskedAutonomousActor()throws Exception {
        var actor=proven();var creative=player(actor.world,1,true,false);var survival=player(actor.world,3,false,false);
        var other=proven();var otherCreative=player(other.world,1,true,false);
        try(var ai=CombatTargetsB173.enter(actor)){
            try(var physical=CombatTargetsB173.continuation(actor)){assertSame(survival,sharedQuery(actor));}
            try(var physical=CombatTargetsB173.continuation(other)){assertSame(otherCreative,sharedQuery(other));}
            assertSame(survival,sharedQuery(actor));
            try(var manual=CombatTargetsB173.enter(null)){
                try(var physical=CombatTargetsB173.continuation(actor)){assertSame(creative,sharedQuery(actor));}
            }
            assertSame(survival,sharedQuery(actor));
        }
        try(var physical=CombatTargetsB173.continuation(actor)){assertSame(creative,sharedQuery(actor));}
    }
    private static void sharedCapture(PlayerEntity player,Actor actor){CombatTargetsB173.capture(player,actor,"capture-site");}
    @Test void provenCaptureSitePreservesExistingToggleReleaseAndOnlyLeasesNewCaptures()throws Exception {
        var actor=proven();var rider=player(actor.world,1,true,false);var nativeRider=(NativePlayer)rider.mcc$modern;
        rider.setVehicle(actor);
        try(var ai=CombatTargetsB173.enter(actor)){
            sharedCapture(rider,actor);assertNull(rider.vehicle);assertNull(nativeRider.getVehicle());
            sharedCapture(rider,actor);assertNull(nativeRider.getVehicle());
            nativeRider.creative=false;sharedCapture(rider,actor);assertSame(actor,rider.vehicle);
            nativeRider.spectator=true;sharedCapture(rider,actor);assertNull(nativeRider.getVehicle());
        }
        // A later manual mount must not inherit the released autonomous edge's lease.
        rider.setVehicle(actor);CombatTargetsB173.playerTick(nativeRider);
        assertSame(actor.mcc$modern,nativeRider.getVehicle());
        CombatTargetsB173.capture(rider,null,"capture-site");
        assertNull(rider.vehicle);assertNull(nativeRider.getVehicle());
    }
    @Test void identicalHelpersRemainNeutralDuringManualCallbacksIncludingNestedCallbacks()throws Exception {
        var actor=proven();var creative=player(actor.world,1,true,false);var survival=player(actor.world,3,false,false);
        try(var ai=CombatTargetsB173.enter(actor)){
            assertSame(survival,sharedQuery(actor));sharedCapture(creative,actor);
            assertNull(((NativePlayer)creative.mcc$modern).getVehicle());
            try(var manual=CombatTargetsB173.enter(null)){
                assertSame(creative,sharedQuery(actor));sharedCapture(creative,actor);
                assertSame(actor.mcc$modern,((NativePlayer)creative.mcc$modern).getVehicle());
            }
            assertSame(survival,sharedQuery(actor));
        }
        CombatTargetsB173.playerTick((NativePlayer)creative.mcc$modern);
        assertSame(actor.mcc$modern,((NativePlayer)creative.mcc$modern).getVehicle());
    }
}
