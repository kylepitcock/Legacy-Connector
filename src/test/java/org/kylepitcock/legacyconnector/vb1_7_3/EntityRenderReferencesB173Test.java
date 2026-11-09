package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EntityRenderReferencesB173Test {
    @BeforeAll static void bootstrap()throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    static class State {Entity target;Object arbitrary=new Object();int count;}
    static class EqualState extends State {
        @Override public boolean equals(Object other){return other instanceof EqualState;}
        @Override public int hashCode(){return 1;}
    }
    static class TypedState {legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity target;}
    private static Entity view(int id,UUID uuid){
        // Only native id/UUID participate in this codec. Native Entity construction additionally
        // requires FML's registered fluid types, which are unrelated to this portable identity test.
        net.minecraft.world.entity.item.ItemEntity nativeEntity;
        try{
            var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
            nativeEntity=(net.minecraft.world.entity.item.ItemEntity)((sun.misc.Unsafe)field.get(null)).allocateInstance(net.minecraft.world.entity.item.ItemEntity.class);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot create native identity fixture",error);}
        nativeEntity.setId(id);nativeEntity.setUUID(uuid);
        var view=new Entity(null){};view.mcc$modern=nativeEntity;return view;
    }
    @Test void nullChangesAndUnchangedServerReferencePreserveLocalCosmeticChoice(){
        var server=new State();var client=new State();var a=view(71,UUID.randomUUID());var b=view(72,UUID.randomUUID());client.target=a;
        var first=AnimalRenderStateB173.capture(server);AnimalRenderStateB173.apply(client,null,first,(id,uuid)->a);assertNull(client.target);
        server.target=a;var second=AnimalRenderStateB173.capture(server);AnimalRenderStateB173.apply(client,first,second,(id,uuid)->a);assertSame(a,client.target);
        client.target=b;AnimalRenderStateB173.apply(client,second,second,(id,uuid)->a);assertSame(b,client.target);
        server.target=b;var third=AnimalRenderStateB173.capture(server);AnimalRenderStateB173.apply(client,second,third,(id,uuid)->b);assertSame(b,client.target);
        server.target=null;AnimalRenderStateB173.apply(client,third,AnimalRenderStateB173.capture(server),(id,uuid)->b);assertNull(client.target);
        assertFalse(first.getAllKeys().stream().anyMatch(key->key.endsWith("#arbitrary")));
    }
    @Test void delayedReferentRetriesWithoutMetadataChangeAndRejectsReusedNumericId(){
        var server=new State();var client=new State();var expected=view(81,UUID.randomUUID());server.target=expected;
        var packet=AnimalRenderStateB173.capture(server);AnimalRenderStateB173.apply(client,null,packet,(id,uuid)->null);assertNull(client.target);
        var impostor=view(81,UUID.randomUUID());AnimalRenderStateB173.retryReferences(client,(id,uuid)->impostor);assertNull(client.target);
        AnimalRenderStateB173.retryReferences(client,(id,uuid)->expected);assertSame(expected,client.target);
        // Once resolved, an unchanged server value must not undo a renderer's local choice.
        client.target=null;AnimalRenderStateB173.apply(client,packet,packet,(id,uuid)->expected);assertNull(client.target);
    }
    @Test void nullUpdateCancelsPendingReferenceAndMalformedReferenceCannotPartiallyApplyScalars(){
        var server=new State();var client=new State();server.target=view(91,UUID.randomUUID());var first=AnimalRenderStateB173.capture(server);
        AnimalRenderStateB173.apply(client,null,first,(id,uuid)->null);server.target=null;var second=AnimalRenderStateB173.capture(server);
        AnimalRenderStateB173.apply(client,first,second,(id,uuid)->null);AnimalRenderStateB173.retryReferences(client,(id,uuid)->view(id,uuid));assertNull(client.target);
        server.target=view(92,UUID.randomUUID());server.count=8;var malformed=AnimalRenderStateB173.capture(server);
        String key=malformed.getAllKeys().stream().filter(k->k.endsWith("#target")).findFirst().orElseThrow();malformed.getCompound(key).remove("entityUUID");
        assertThrows(IllegalArgumentException.class,()->AnimalRenderStateB173.apply(client,second,malformed,(id,uuid)->server.target));assertEquals(0,client.count);
    }
    @Test void onlyAssignableLegacyEntityViewsCanSatisfyTypedReference(){
        var server=new TypedState();var client=new TypedState();var wrong=view(101,UUID.randomUUID());
        var right=new legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity(null){};right.mcc$modern=wrong.mcc$modern;server.target=right;
        AnimalRenderStateB173.apply(client,null,AnimalRenderStateB173.capture(server),(id,uuid)->wrong);assertNull(client.target);
        AnimalRenderStateB173.retryReferences(client,(id,uuid)->right);assertSame(right,client.target);
    }
    @Test void equalButDistinctOriginalsKeepIndependentPendingServerReferences(){
        var a=view(111,UUID.randomUUID());var b=view(112,UUID.randomUUID());
        var serverA=new EqualState();var serverB=new EqualState();serverA.target=a;serverB.target=b;
        var clientA=new EqualState();var clientB=new EqualState();assertEquals(clientA,clientB);assertNotSame(clientA,clientB);
        AnimalRenderStateB173.apply(clientA,null,AnimalRenderStateB173.capture(serverA),(id,uuid)->null);
        AnimalRenderStateB173.apply(clientB,null,AnimalRenderStateB173.capture(serverB),(id,uuid)->null);
        AnimalRenderStateB173.retryReferences(clientA,(id,uuid)->a);assertSame(a,clientA.target);assertNull(clientB.target);
        AnimalRenderStateB173.retryReferences(clientB,(id,uuid)->b);assertSame(b,clientB.target);assertSame(a,clientA.target);
    }
}
