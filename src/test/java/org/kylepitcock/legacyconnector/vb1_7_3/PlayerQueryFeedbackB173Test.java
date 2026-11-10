package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.src.PlayerBase;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlayerQueryFeedbackB173Test {
    static class Actor extends ClientPlayerEntity {
        int calls;double requestedX,requestedY,requestedZ;Actor nested;
        Actor(){super(null);}
    }
    public static class Query extends PlayerBase {
        public Query(ClientPlayerEntity player){super(player);}
        @Override public double getDistanceSq(double x,double y,double z,double baseline){
            if(!(player instanceof Actor actor))return baseline;
            assertSame(actor,PlayerApiB173.currentPlayer());actor.calls++;actor.requestedX=x;actor.requestedY=y;actor.requestedZ=z;
            assertEquals(baseline,PlayerAbilitiesB173.distance(actor,x,y,z,baseline),"Nested query uses supplied baseline without recursion");
            if(actor.nested!=null){var other=actor.nested;actor.nested=null;assertEquals(11,other.getSquaredDistance(1,0,0));assertSame(actor,PlayerApiB173.currentPlayer());}
            return baseline+10;
        }
    }
    private static class MessagePlayer extends net.minecraft.world.entity.player.Player {
        Component received;boolean overlay;
        private MessagePlayer(){super(null,net.minecraft.core.BlockPos.ZERO,0,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Message fixture"));}
        @Override public boolean isCreative(){return false;}
        @Override public boolean isSpectator(){return false;}
        @Override public void displayClientMessage(Component message,boolean actionBar){received=message;overlay=actionBar;}
    }
    private static MessagePlayer blank() throws Exception {
        return blank(MessagePlayer.class);
    }
    private static <T>T blank(Class<T> clazz) throws Exception {
        var type=Class.forName("sun.misc.Unsafe");var f=type.getDeclaredField("theUnsafe");f.setAccessible(true);
        return clazz.cast(type.getMethod("allocateInstance",Class.class).invoke(f.get(null),clazz));
    }
    private static Actor nativeActor() throws Exception {
        var actor=new Actor();var player=blank();var level=blank(net.minecraft.client.multiplayer.ClientLevel.class);
        var levelField=net.minecraft.world.entity.Entity.class.getDeclaredField("level");levelField.setAccessible(true);levelField.set(player,level);
        var added=net.minecraft.world.entity.Entity.class.getDeclaredField("isAddedToLevel");added.setAccessible(true);added.setBoolean(player,true);
        var world=blank(legacyconnector.vb1_7_3.net.minecraft.world.World.class);
        var backing=world.getClass().getDeclaredField("mcc$level");backing.setAccessible(true);backing.set(world,level);
        actor.mcc$modern=player;actor.world=world;return actor;
    }
    @BeforeAll static void bootstrap() throws Exception {NativeStackComponentsB173Test.bootstrap();PlayerApiB173.register(Query.class);}
    @Test void sdkCoordinateAndEntityDistancesDispatchOnceWithOriginalCoordinates(){
        var actor=new Actor();actor.setPosition(1,2,3);
        assertEquals(35,actor.getSquaredDistance(4,6,3));assertEquals(1,actor.calls);
        assertEquals(4,actor.requestedX);assertEquals(6,actor.requestedY);assertEquals(3,actor.requestedZ);
        var target=new Actor();target.setPosition(4,6,3);assertEquals(25,actor.getSquaredDistance(target));assertEquals(1,actor.calls,"Entity overload remains independent of coordinate hook, as in Beta");
        assertNull(PlayerApiB173.currentPlayer());assertFalse(PlayerAbilitiesB173.active(actor,PlayerAbilitiesB173.Hook.DISTANCE));
    }
    @Test void nestedDifferentActorsAndUnchangedHooksRemainIsolated(){
        var actor=new Actor();var other=new Actor();actor.nested=other;
        assertEquals(19,actor.getSquaredDistance(0,3,0));assertEquals(1,actor.calls);assertEquals(1,other.calls);
        var ordinary=new ClientPlayerEntity(null){};assertEquals(9,ordinary.getSquaredDistance(0,3,0));
        assertNull(PlayerApiB173.currentPlayer());
    }
    @Test void bedKeysTranslateOnRecipientAndPlainOrUnknownTextRemainsUsable(){
        assertEquals("block.minecraft.bed.occupied",((TranslatableContents)PlayerMessagesB173.component("tile.bed.occupied").getContents()).getKey());
        assertEquals("block.minecraft.bed.no_sleep",((TranslatableContents)PlayerMessagesB173.component("tile.bed.noSleep").getContents()).getKey());
        assertEquals("Original chat Ω",PlayerMessagesB173.component("Original chat Ω").getString());
        assertEquals("fixture.unknown.key",PlayerMessagesB173.component("fixture.unknown.key").getString());
        ContentB173.addLocalization("fixture.player_feedback","Original localized feedback");
        assertEquals("Original localized feedback",PlayerMessagesB173.component("fixture.player_feedback").getString());
    }
    @Test void nativeRecipientReceivesChatWithoutBorrowingOtherActorOrSendingActionBar() throws Exception {
        var first=blank();var second=blank();var actor=new Actor();actor.mcc$modern=first;var other=new Actor();other.mcc$modern=second;
        actor.sendMessage("tile.bed.noSleep");assertNotNull(first.received);assertNull(second.received);assertFalse(first.overlay);
        other.sendMessage("Only second");assertEquals("Only second",second.received.getString());
        assertEquals("block.minecraft.bed.no_sleep",((TranslatableContents)first.received.getContents()).getKey());
        var detached=new Actor();detached.sendMessage("No native recipient");assertEquals("Only second",second.received.getString());
        actor.sendMessage(null);assertEquals("block.minecraft.bed.no_sleep",((TranslatableContents)first.received.getContents()).getKey());
    }
    @Test void nativeCoordinateQueryUsesMatchingPendingActorExactlyOnceAndDoesNotCreateViews() throws Exception {
        var actor=nativeActor();var player=actor.mcc$modern;actor.health=13;PlayerApiB173.init(actor);
        PlayerApiB173.withPlayer(actor,()->{
            assertEquals(35,PlayerAbilitiesB173.nativeDistance(player,4,6,3,25));
            assertEquals(1,actor.calls);assertEquals(13,actor.health);
            assertNull(PlayerViewB173.peek((net.minecraft.world.entity.player.Player)player));
            try(var scope=new PlayerAbilitiesB173.Scope(player,PlayerAbilitiesB173.Hook.DISTANCE)){
                assertEquals(25,PlayerAbilitiesB173.nativeDistance(player,4,6,3,25));
            }
            assertEquals(1,actor.calls);
        });
        assertNull(PlayerApiB173.currentPlayer());
        assertEquals(25,PlayerAbilitiesB173.nativeDistance(player,4,6,3,25),"No cached view means native baseline, not extension construction");
    }
    @Test void nativeActorIdentityWorldAndLifecycleGuardsPreserveBaseline() throws Exception {
        var actor=nativeActor();var unrelated=nativeActor();
        PlayerApiB173.withPlayer(actor,()->assertEquals(7,PlayerAbilitiesB173.nativeDistance(actor.mcc$modern,1,2,3,7),"A geometry query cannot initialize original player bases"));
        assertTrue(PlayerApiB173.existingBases(actor).isEmpty());
        PlayerApiB173.withPlayer(actor,()->assertEquals(7,PlayerAbilitiesB173.nativeDistance(unrelated.mcc$modern,1,2,3,7)));
        var validWorld=actor.world;actor.world=unrelated.world;
        PlayerApiB173.withPlayer(actor,()->assertEquals(7,PlayerAbilitiesB173.nativeDistance(actor.mcc$modern,1,2,3,7)));
        actor.world=validWorld;
        var added=net.minecraft.world.entity.Entity.class.getDeclaredField("isAddedToLevel");added.setAccessible(true);added.setBoolean(actor.mcc$modern,false);
        PlayerApiB173.withPlayer(actor,()->assertEquals(7,PlayerAbilitiesB173.nativeDistance(actor.mcc$modern,1,2,3,7)));
        assertEquals(0,actor.calls);assertEquals(0,unrelated.calls);
        assertEquals(7,PlayerAbilitiesB173.nativeDistance(null,1,2,3,7));
        assertEquals(7,PlayerAbilitiesB173.nativeDistance(blank(net.minecraft.world.entity.animal.Pig.class),1,2,3,7));
    }
    private static java.util.Map<String,java.util.List<String>> nativeCalls(Class<?> type) throws Exception {
        var calls=new java.util.HashMap<String,java.util.List<String>>();
        try(var stream=type.getResourceAsStream(type.getSimpleName()+".class")){
            assertNotNull(stream);new org.objectweb.asm.ClassReader(stream).accept(new org.objectweb.asm.ClassVisitor(9<<16){
                @Override public org.objectweb.asm.MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){
                    var list=calls.computeIfAbsent(name+desc,key->new java.util.ArrayList<>());
                    return new org.objectweb.asm.MethodVisitor(9<<16){
                        @Override public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean itf){list.add(owner+"."+method+descriptor);}
                    };
                }
            },org.objectweb.asm.ClassReader.SKIP_DEBUG|org.objectweb.asm.ClassReader.SKIP_FRAMES);
        }
        return calls;
    }
    @Test void nativeOverloadDelegationAndNearestPlayerCoordinateCallKeepHookScopeExact() throws Exception {
        var calls=nativeCalls(net.minecraft.world.entity.Entity.class);
        String coordinates="distanceToSqr(DDD)D",vector="distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D";
        assertTrue(calls.get(coordinates).stream().noneMatch(s->s.contains(".distanceToSqr")));
        assertTrue(calls.get(vector).stream().noneMatch(s->s.contains(".distanceToSqr")));
        assertEquals(1,calls.get("distanceToSqr(Lnet/minecraft/world/entity/Entity;)D").stream().filter(s->s.endsWith("."+vector)).count());
        assertTrue(nativeCalls(net.minecraft.world.level.EntityGetter.class).entrySet().stream()
                .filter(e->e.getKey().startsWith("getNearestPlayer("))
                .anyMatch(e->e.getValue().stream().anyMatch(s->s.endsWith("."+coordinates))),"Native nearest-player lookup uses the exact coordinate hook");
    }
}
