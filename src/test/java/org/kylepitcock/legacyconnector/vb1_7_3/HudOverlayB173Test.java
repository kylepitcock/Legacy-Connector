package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HudOverlayB173Test {
    @BeforeAll static void bootstrap() throws Exception {InteractionManagerB173Test.bootstrap();}
    private static <T>T blank(Class<T> type) throws Exception {
        var unsafe=Class.forName("sun.misc.Unsafe");var field=unsafe.getDeclaredField("theUnsafe");field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance",Class.class).invoke(field.get(null),type));
    }
    @Test void originalTextAndUnicodeRoundtripWithoutTranslationOrAudio(){
        String text=HudOverlayB173.text("Original Ω record");assertEquals("Now playing: Original Ω record",text);
        assertEquals(text,HudOverlayB173.decode(HudOverlayB173.encode(text)));
        assertEquals("Now playing: null",HudOverlayB173.text(null));
        assertNull(HudOverlayB173.encode("Ω".repeat(9000)));assertNull(HudOverlayB173.decode(new byte[16385]));
    }
    @Test void serverActorNeverFallsThroughToInstalledLocalClient() throws Exception {
        var server=blank(net.minecraft.server.level.ServerPlayer.class);var remote=new AtomicInteger();
        HudOverlayB173.dispatch(server,true,"text",p->{assertSame(server,p);remote.incrementAndGet();},s->fail("server text leaked locally"));
        assertEquals(1,remote.get());
    }
    @Test void actorlessOrServerSideOrdinaryPlayerNeverBorrowsLocalOverlay() throws Exception {
        var player=blank(InteractionManagerB173Test.NativePlayer.class);var local=new AtomicInteger();
        HudOverlayB173.dispatch(null,true,"text",p->fail(),s->fail());
        HudOverlayB173.dispatch(player,false,"text",p->fail(),s->fail());
        HudOverlayB173.dispatch(player,true,"text",p->fail(),s->{assertEquals("text",s);local.incrementAndGet();});
        assertEquals(1,local.get());
    }
}
