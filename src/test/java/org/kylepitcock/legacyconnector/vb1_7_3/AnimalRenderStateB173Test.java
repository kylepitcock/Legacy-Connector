package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class AnimalRenderStateB173Test {
    static class Parent {private long count=19;int same=7;}
    static class State extends Parent {
        static int shared=90;final int fixed=91;Object reference=new Object();
        int same=3;boolean chosen;float growth=.5F;String name;transient double transientValue=3.5;
        byte small=-2;short medium=400;char character='\u03bb';
    }
    @Test void roundTripsInheritedAndHiddenScalarsWithoutReferencesOrConstants() {
        State server=new State(),client=new State();server.same=42;((Parent)server).same=43;
        server.chosen=true;server.name="original";server.growth=.75F;
        Object clientReference=client.reference;
        assertNull(AnimalRenderStateB173.unsupportedReason(State.class));
        CompoundTag snapshot=AnimalRenderStateB173.capture(server);
        AnimalRenderStateB173.apply(client,null,snapshot);
        assertEquals(snapshot,AnimalRenderStateB173.capture(client));
        assertEquals(42,client.same);assertEquals(43,((Parent)client).same);
        assertSame(clientReference,client.reference);
        assertFalse(snapshot.getAllKeys().stream().anyMatch(k->k.endsWith("#shared")||k.endsWith("#fixed")||k.endsWith("#reference")));
    }
    @Test void serverDeltaPreservesClientRenderChoicesUntilServerActuallyChangesThem() {
        State server=new State(),client=new State();
        CompoundTag first=AnimalRenderStateB173.capture(server);
        AnimalRenderStateB173.apply(client,null,first);
        client.chosen=true;client.same=4;client.name="client texture choice";
        server.growth=.6F;
        CompoundTag second=AnimalRenderStateB173.capture(server);
        AnimalRenderStateB173.apply(client,first,second);
        assertTrue(client.chosen);assertEquals(4,client.same);assertEquals("client texture choice",client.name);
        assertEquals(.6F,client.growth);
        server.same=2;server.name="server choice";
        AnimalRenderStateB173.apply(client,second,AnimalRenderStateB173.capture(server));
        assertEquals(2,client.same);assertEquals("server choice",client.name);
    }
    @Test void nullStringsAndMalformedPacketsAreHandledWithoutPartialApplication() {
        State server=new State(),client=new State();client.name="previous";
        CompoundTag initial=AnimalRenderStateB173.capture(server);
        AnimalRenderStateB173.apply(client,null,initial);assertNull(client.name);
        server.growth=.9F;CompoundTag malformed=AnimalRenderStateB173.capture(server);
        String last=malformed.getAllKeys().stream().max(String::compareTo).orElseThrow();
        malformed.remove(last);
        CompoundTag before=AnimalRenderStateB173.capture(client);
        assertThrows(IllegalArgumentException.class,()->AnimalRenderStateB173.apply(client,initial,malformed));
        assertEquals(before,AnimalRenderStateB173.capture(client));
    }
    @Test void rejectsUnresolvableOriginalFieldSignaturesWithDiagnostic() {
        var writer=new org.objectweb.asm.ClassWriter(0);
        writer.visit(org.objectweb.asm.Opcodes.V1_8,org.objectweb.asm.Opcodes.ACC_PUBLIC,"renderfixture/Unresolved",null,"java/lang/Object",null);
        writer.visitField(org.objectweb.asm.Opcodes.ACC_PUBLIC,"unknown","Lrenderfixture/NotPresent;",null,null).visitEnd();
        writer.visitEnd();
        class FixtureLoader extends ClassLoader {
            Class<?> define(byte[] bytes){return defineClass("renderfixture.Unresolved",bytes,0,bytes.length);}
        }
        Class<?> broken=new FixtureLoader().define(writer.toByteArray());
        String reason=AnimalRenderStateB173.unsupportedReason(broken);
        assertNotNull(reason);assertTrue(reason.contains("cannot inspect original render fields"));
    }

}
