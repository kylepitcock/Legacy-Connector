package legacyconnector.v1_6_4.net.minecraft.src;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * ModLoaderMP's packet 230 over the layer's channel, without a game: the wire bytes a sender builds ({@code wrap} = {@code encode}) reach the mod's
 * {@code handlePacket} through the receiving end, with the mod found by the id in the packet (the hash of its class name).
 */
class ModLoaderMpLoopbackTest {
    public static class EchoMod extends BaseModMp {
        Packet230ModLoader seen;

        @Override
        public String getVersion() {
            return "1";
        }

        @Override
        public void load() {}

        @Override
        public void handlePacket(Packet230ModLoader packet) {
            seen = packet;
        }
    }

    public static class OtherMod extends EchoMod {}

    @Test
    void clientReceivesThePacketTheServerHalfEncoded() {
        EchoMod mod = new EchoMod();
        ModLoaderMp.mcc$put(mod);
        Packet230ModLoader out = new Packet230ModLoader();
        out.modId = mod.getId();
        out.packetType = 3;
        out.dataInt = new int[] {7, -1};
        out.dataFloat = new float[] {0.5F};
        out.dataDouble = new double[] {1.25};
        out.dataString = new String[] {"hello", "wörld"};
        ModLoaderMp.receiveOnClient(out.encode());
        Packet230ModLoader in = mod.seen;
        assertNotNull(in);
        assertEquals(3, in.packetType);
        assertArrayEquals(out.dataInt, in.dataInt);
        assertArrayEquals(out.dataFloat, in.dataFloat);
        assertArrayEquals(out.dataDouble, in.dataDouble);
        assertArrayEquals(out.dataString, in.dataString);
    }

    @Test
    void packetOfAnotherModIsNotDeliveredToThisOne() {
        EchoMod mod = new EchoMod();
        OtherMod other = new OtherMod();
        ModLoaderMp.mcc$put(mod);
        ModLoaderMp.mcc$put(other);
        Packet230ModLoader out = new Packet230ModLoader();
        out.modId = other.getId();
        ModLoaderMp.receiveOnClient(out.encode());
        assertNull(mod.seen);
        assertNotNull(other.seen);
    }

    @Test
    void serverEndIgnoresAnUnknownSenderAndUnknownMod() {
        EchoMod mod = new EchoMod();
        ModLoaderMp.mcc$put(mod);
        Packet230ModLoader out = new Packet230ModLoader();
        out.modId = mod.getId();
        // a sender that is not a player (or a null) must be dropped, not thrown on
        ModLoaderMp.receiveOnServer(out.encode(), null);
        out.modId = 1234567;
        ModLoaderMp.receiveOnServer(out.encode(), null);
        assertNull(mod.seen);
    }
}
