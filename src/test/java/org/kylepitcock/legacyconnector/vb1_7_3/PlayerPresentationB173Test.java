package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.src.PlayerBase;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlayerPresentationB173Test {
    public static class PresentationBase extends PlayerBase {
        public int color=7;public boolean hidden;private String cape="initial";
        public char glyph='a';public long ticks;public float alpha=1;public double offset;
        public static int shared=91;public final int fixed=92;public Object arbitrary=new Object();
        public PresentationBase(ClientPlayerEntity player){super(player);}
    }
    public static class UnregisteredBase extends PlayerBase {
        public int privateState=1;public UnregisteredBase(ClientPlayerEntity player){super(player);}
    }
    @BeforeAll static void bootstrap() throws Exception {NativeStackComponentsB173Test.bootstrap();PlayerApiB173.register(PresentationBase.class);}
    private static ClientPlayerEntity player(){return new ClientPlayerEntity(null){};}
    @Test void presentationSchemaAndLinkageFailuresCannotSkipSubsequentOwnerWork(){
        var owner=new java.util.concurrent.atomic.AtomicInteger();
        PlayerPresentationNetworkB173.guard("test-schema",()->{throw new IllegalStateException("unsupported original schema");});
        owner.incrementAndGet();
        PlayerPresentationNetworkB173.guard("test-network",()->{throw new NoClassDefFoundError("optional presentation dependency");});
        owner.incrementAndGet();
        PlayerPresentationNetworkB173.guard("test-valid",owner::incrementAndGet);
        assertEquals(3,owner.get());
    }
    @Test void declaredOriginalScalarsUpdateOnlyTheAddressedPlayersBase(){
        var source=new PresentationBase(player());source.color=23;source.hidden=true;source.cape="Original Ω";source.ticks=Long.MAX_VALUE;source.glyph='Ω';source.alpha=.25F;source.offset=-.75;
        var remote=new PresentationBase(player());var self=new PresentationBase(player());
        var wire=PlayerPresentationStateB173.captureScalars(List.of(source));
        assertTrue(PlayerPresentationStateB173.applyScalars(List.of(remote),wire));
        assertEquals(23,remote.color);assertTrue(remote.hidden);assertEquals("Original Ω",remote.cape);assertEquals('Ω',remote.glyph);
        assertEquals(Long.MAX_VALUE,remote.ticks);assertEquals(.25F,remote.alpha);assertEquals(-.75,remote.offset);
        assertEquals(7,self.color);assertFalse(self.hidden);assertEquals("initial",self.cape);assertEquals(23,source.color);
        var names=new HashSet<String>();for(var raw:wire.getList("fields",Tag.TAG_COMPOUND))names.add(((CompoundTag)raw).getString("field"));
        assertFalse(names.contains("player"));assertFalse(names.contains("shared"));assertFalse(names.contains("fixed"));assertFalse(names.contains("arbitrary"));
        assertEquals(91,PresentationBase.shared);assertEquals(92,remote.fixed);
    }
    @Test void unregisteredClassNamesUnknownFieldsAndTypeForgeryCannotMutateAnyEarlierField(){
        var source=new PresentationBase(player());source.color=99;var remote=new PresentationBase(player());
        var wire=PlayerPresentationStateB173.captureScalars(List.of(source));var entries=wire.getList("fields",Tag.TAG_COMPOUND);
        entries.getCompound(entries.size()-1).putString("base","untrusted.ClassName");
        assertFalse(PlayerPresentationStateB173.applyScalars(List.of(remote),wire));assertEquals(7,remote.color);
        wire=PlayerPresentationStateB173.captureScalars(List.of(source));wire.getList("fields",Tag.TAG_COMPOUND).getCompound(0).putString("type","java.lang.Object");
        assertFalse(PlayerPresentationStateB173.applyScalars(List.of(remote),wire));assertEquals(7,remote.color);
        assertTrue(PlayerPresentationStateB173.captureScalars(List.of(new UnregisteredBase(player()))).getList("fields",Tag.TAG_COMPOUND).isEmpty());
    }
    @Test void nullStringsAndHardLimitsHaveDefinedBoundedBehavior(){
        var source=new PresentationBase(player());source.cape=null;var remote=new PresentationBase(player());
        assertTrue(PlayerPresentationStateB173.applyScalars(List.of(remote),PlayerPresentationStateB173.captureScalars(List.of(source))));assertNull(remote.cape);
        source.cape="x".repeat(PlayerPresentationStateB173.MAX_STRING+1);
        assertThrows(IllegalStateException.class,()->PlayerPresentationStateB173.captureScalars(List.of(source)));
        assertThrows(IllegalStateException.class,()->PlayerPresentationStateB173.captureScalars(Collections.nCopies(33,source)));
        source.cape="valid";source.alpha=Float.NaN;assertThrows(IllegalStateException.class,()->PlayerPresentationStateB173.captureScalars(List.of(source)));
    }
    @Test void twoPlayerIdentityNativeEntityReuseDimensionSelfAndRevisionRemainIsolated(){
        UUID self=UUID.randomUUID(),other=UUID.randomUUID(),third=UUID.randomUUID();var realm=ResourceLocation.withDefaultNamespace("overworld");
        var wire=new PlayerPresentationStateB173.Identity(other,42,realm,9);
        assertTrue(PlayerPresentationStateB173.matches(wire,self,other,42,realm,8));
        assertFalse(PlayerPresentationStateB173.matches(wire,self,other,42,realm,9));
        assertFalse(PlayerPresentationStateB173.matches(wire,self,other,42,realm,10));
        assertFalse(PlayerPresentationStateB173.matches(wire,other,other,42,realm,0));
        assertFalse(PlayerPresentationStateB173.matches(wire,self,third,42,realm,0));
        assertFalse(PlayerPresentationStateB173.matches(wire,self,other,43,realm,0));
        assertFalse(PlayerPresentationStateB173.matches(wire,self,other,42,ResourceLocation.withDefaultNamespace("the_nether"),0));
    }
    @Test void ownerInitialScalarsHydrateFreshDeathAndReconnectMirrorsWithoutInventoryOrNativeWrites(){
        var authority=new PresentationBase(player());authority.color=22;
        var state=PlayerPresentationStateB173.captureScalars(List.of(authority));
        // Owner inventory uses its existing transaction protocol. Even an unrelated inventory
        // payload must not be decoded/applied by scalar hydration.
        state.putString("inventories","not an owner inventory transaction");
        var old=new Object();var replacement=new Object();UUID self=UUID.randomUUID();
        var realm=ResourceLocation.withDefaultNamespace("overworld");
        long prior=9;
        assertEquals(prior,PlayerPresentationStateB173.ownerRevision(old,old,prior));
        for(var lifetime:List.of(replacement,new Object())){
            var freshPlayer=player();freshPlayer.health=13;freshPlayer.velocityY=-.7;freshPlayer.fallDistance=4;
            var fresh=new PresentationBase(freshPlayer);Object untouched=fresh.arbitrary;
            long revision=PlayerPresentationStateB173.ownerRevision(old,lifetime,prior);
            var wire=new PlayerPresentationStateB173.Identity(self,42,realm,1);
            assertTrue(PlayerPresentationStateB173.matchesOwner(wire,self,42,realm,revision));
            assertTrue(PlayerPresentationStateB173.applyScalars(List.of(fresh),state));
            assertEquals(22,fresh.color);assertSame(untouched,fresh.arbitrary);
            assertEquals(13,freshPlayer.health);assertEquals(-.7,freshPlayer.velocityY);assertEquals(4,freshPlayer.fallDistance);
            assertFalse(PlayerPresentationStateB173.matchesOwner(wire,self,42,realm,1));
            assertFalse(PlayerPresentationStateB173.matchesOwner(wire,UUID.randomUUID(),42,realm,0));
            assertFalse(PlayerPresentationStateB173.matchesOwner(wire,self,43,realm,0));
            assertFalse(PlayerPresentationStateB173.matchesOwner(wire,self,42,ResourceLocation.withDefaultNamespace("the_nether"),0));
        }
    }
    @Test void subjectEnvelopeRoundtripsAndMalformedOrOversizedPacketsAreRejected(){
        var tag=new CompoundTag();UUID subject=UUID.randomUUID();tag.putUUID("subject",subject);tag.putInt("entity",42);tag.putLong("revision",3);tag.putString("dimension","minecraft:overworld");tag.put("state",new CompoundTag());
        assertEquals(tag,PlayerPresentationNetworkB173.decode(PlayerPresentationNetworkB173.encode(tag)));
        assertEquals(subject,PlayerPresentationNetworkB173.identity(tag).subject());
        var removed=tag.copy();removed.remove("state");removed.putBoolean("remove",true);assertNotNull(PlayerPresentationNetworkB173.identity(removed));
        var invalid=tag.copy();invalid.remove("subject");assertNull(PlayerPresentationNetworkB173.decode(PlayerPresentationNetworkB173.encode(invalid)));
        invalid=tag.copy();invalid.putLong("revision",0);assertNull(PlayerPresentationNetworkB173.identity(invalid));
        assertNull(PlayerPresentationNetworkB173.decode(new byte[]{1,2,3}));
        assertNull(PlayerPresentationNetworkB173.decode(new byte[PlayerPresentationNetworkB173.MAX_BYTES+1]));
        tag.putByteArray("oversized",new byte[PlayerPresentationNetworkB173.MAX_BYTES]);assertNull(PlayerPresentationNetworkB173.encode(tag));
    }
    private static final class MirrorInventory extends PlayerInventoryStateB173Test.OwnedInventory {
        int writes,dirty;
        MirrorInventory(){super(1);}
        @Override public void setStack(int slot,legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack){writes++;}
        @Override public void markDirty(){dirty++;}
    }
    private static CompoundTag withInventory(CompoundTag state,String binding,int size){
        var inventory=new CompoundTag();inventory.putInt("version",1);var entries=new ListTag();var entry=new CompoundTag();
        entry.putString("binding",binding);entry.putInt("size",size);var stacks=new ListTag();
        for(int i=0;i<size;i++)stacks.add(new CompoundTag());entry.put("stacks",stacks);entries.add(entry);
        inventory.put("inventories",entries);state.put("inventories",inventory);return state;
    }
    @Test void badInventoryRejectsEntirePresentationBeforeScalarMutationOrAcceptedRevision(){
        var source=new PresentationBase(player());source.color=99;var remote=new PresentationBase(player());
        var inventory=new MirrorInventory();var bindings=List.of(new PlayerInventoryStateB173.Binding("equipment",inventory));
        var accepted=new java.util.concurrent.atomic.AtomicLong(4);
        for(var bad:List.of(withInventory(PlayerPresentationStateB173.captureScalars(List.of(source)),"unknown",1),
                withInventory(PlayerPresentationStateB173.captureScalars(List.of(source)),"equipment",2))){
            var prepared=PlayerPresentationStateB173.prepare(List.of(remote),bindings,bad,t->net.minecraft.world.item.ItemStack.EMPTY);
            if(prepared!=null){prepared.run();accepted.set(5);}
            assertNull(prepared);assertEquals(7,remote.color);assertEquals(0,inventory.writes);assertEquals(0,inventory.dirty);assertEquals(4,accepted.get());
        }
        var badStack=withInventory(PlayerPresentationStateB173.captureScalars(List.of(source)),"equipment",1);
        badStack.getCompound("inventories").getList("inventories",Tag.TAG_COMPOUND).getCompound(0).getList("stacks",Tag.TAG_COMPOUND).getCompound(0).putString("id","missing:unrepresentable");
        assertNull(PlayerPresentationStateB173.prepare(List.of(remote),bindings,badStack,t->net.minecraft.world.item.ItemStack.EMPTY));
        assertEquals(7,remote.color);assertEquals(0,inventory.writes);
        var badScalar=withInventory(PlayerPresentationStateB173.captureScalars(List.of(source)),"equipment",1);
        badScalar.getList("fields",Tag.TAG_COMPOUND).getCompound(0).putString("type","java.lang.Object");
        assertNull(PlayerPresentationStateB173.prepare(List.of(remote),bindings,badScalar,t->net.minecraft.world.item.ItemStack.EMPTY));
        assertEquals(7,remote.color);assertEquals(0,inventory.writes);
        var valid=withInventory(PlayerPresentationStateB173.captureScalars(List.of(source)),"equipment",1);
        var prepared=PlayerPresentationStateB173.prepare(List.of(remote),bindings,valid,t->net.minecraft.world.item.ItemStack.EMPTY);
        assertNotNull(prepared);assertEquals(7,remote.color);assertEquals(0,inventory.writes);
        prepared.run();accepted.set(5);assertEquals(99,remote.color);assertEquals(1,inventory.writes);assertEquals(1,inventory.dirty);assertEquals(5,accepted.get());
    }
    @Test void mirrorEvictionAndLifetimeResetReleaseEveryRetainedOwnerExactlyOnce(){
        var retained=new LinkedHashMap<Integer,Object>();var released=new ArrayList<Object>();
        var first=new Object();var second=new Object();var third=new Object();retained.put(1,first);retained.put(2,second);retained.put(3,third);
        PlayerPresentationStateB173.trimMirrors(retained,2,released::add);
        assertEquals(List.of(first),released);assertEquals(List.of(2,3),List.copyOf(retained.keySet()));
        PlayerPresentationStateB173.releaseMirrors(retained,released::add);
        assertTrue(retained.isEmpty());assertEquals(List.of(first,second,third),released);
        PlayerPresentationStateB173.releaseMirrors(retained,released::add);assertEquals(3,released.size());
    }
}
