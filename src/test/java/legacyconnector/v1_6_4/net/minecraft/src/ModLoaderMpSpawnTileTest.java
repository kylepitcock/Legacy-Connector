package legacyconnector.v1_6_4.net.minecraft.src;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet250CustomPayload;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.junit.jupiter.api.Test;

/**
 * ModLoaderMP's tile entity packet and custom entity spawns, checked against the real classes (javap of ModLoaderMP 1.2.5 client and server) without a game:
 * the tile packet's layout {@code {mod id, x, y, z, type, ints...}} on the reserved id "ModLoaderMP" type 1, and the "Spawn" packet that an {@link ISpawnable} entity
 * writes with its type id (ids above 127 folded to the signed byte) and a registered client class reads.
 */
class ModLoaderMpSpawnTileTest {
    @org.junit.jupiter.api.BeforeAll
    static void mc125On() {
        System.setProperty(org.kylepitcock.legacyconnector.v1_6_4.bridge.Mc125Host.PROPERTY, "true"); // the loader sets it when it hosts a 1.2.5 jar
    }

    @org.junit.jupiter.api.AfterAll
    static void mc125Off() {
        System.clearProperty(org.kylepitcock.legacyconnector.v1_6_4.bridge.Mc125Host.PROPERTY);
    }

    public static class TileMod extends BaseModMp {
        int[] seen;
        float[] floats;
        String[] strings;

        @Override
        public String getVersion() {
            return "tile";
        }

        @Override
        public void load() {}

        @Override
        public void handleTileEntityPacket(int x, int y, int z, int type, int[] ints, float[] f, String[] s) {
            seen = new int[] {x, y, z, type, ints.length > 0 ? ints[0] : -1, ints.length};
            floats = f;
            strings = s;
        }
    }

    public static class Arrow extends Entity implements ISpawnable {
        Packet230ModLoader spawned;

        Arrow() {
            super((World) null);
        }

        @Override
        public Packet230ModLoader getSpawnPacket() {
            Packet230ModLoader p = new Packet230ModLoader();
            p.dataInt = new int[] {42};
            p.dataFloat = new float[] {1.5F, -2F};
            return p;
        }

        @Override
        public void spawn(Packet230ModLoader packet) {
            spawned = packet;
        }
    }

    public static class Unregistered extends Arrow {}

    /** A client build entity: only spawn(), no getSpawnPacket (what the real client interface declared). */
    public static class ClientOnly extends Entity implements ISpawnable {
        ClientOnly() {
            super((World) null);
        }

    }

    @Test
    void tilePacketHasTheRealLayoutAndReachesTheModsHandler() {
        TileMod mod = new TileMod();
        ModLoaderMp.mcc$put(mod);
        var packet = ModLoaderMp.getTileEntityPacket(mod, 10, 64, -3, 7, new int[] {99, 100}, new float[] {0.25F}, new String[] {"owner"});
        Packet250CustomPayload payload = assertInstanceOf(Packet250CustomPayload.class, packet);
        assertEquals(ModLoaderMp.CHANNEL, payload.channel);
        Packet230ModLoader raw = Packet230ModLoader.decode(payload.mcc$bytes());
        assertEquals("ModLoaderMP".hashCode(), raw.modId);
        assertEquals(1, raw.packetType);
        assertArrayEquals(new int[] {mod.getId(), 10, 64, -3, 7, 99, 100}, raw.dataInt);
        ModLoaderMp.receiveOnClient(payload.mcc$bytes());
        assertArrayEquals(new int[] {10, 64, -3, 7, 99, 2}, mod.seen);
        assertArrayEquals(new float[] {0.25F}, mod.floats);
        assertArrayEquals(new String[] {"owner"}, mod.strings);
    }

    @Test
    void tilePacketWithoutExtraIntsAndABadOneAreHandled() {
        TileMod mod = new TileMod();
        ModLoaderMp.mcc$put(mod);
        var payload = (Packet250CustomPayload) ModLoaderMp.getTileEntityPacket(mod, 1, 2, 3, 4, null, null, null);
        ModLoaderMp.receiveOnClient(payload.mcc$bytes());
        assertArrayEquals(new int[] {1, 2, 3, 4, -1, 0}, mod.seen);
        mod.seen = null;
        Packet230ModLoader bad = new Packet230ModLoader();
        bad.modId = "ModLoaderMP".hashCode();
        bad.packetType = 1;
        bad.dataInt = new int[] {mod.getId(), 1, 2};
        ModLoaderMp.receiveOnClient(bad.encode());   // "Bad TileEntityPacket received.", not an exception
        assertNull(mod.seen);
    }

    @Test
    void spawnPacketCarriesTheTypeIdAndReachesTheRegisteredClass() {
        ModLoaderMp.registerEntityTrackerEntry(Arrow.class, 200);          // above 127: the wire byte is 200 - 256
        ModLoaderMp.registerNetClientHandlerEntity(Arrow.class, 200);
        assertNotNull(ModLoaderMp.handleNetClientHandlerEntities(-56));
        assertSame(Arrow.class, ModLoaderMp.handleNetClientHandlerEntities(-56).entityClass);
        assertEquals(200, ModLoaderMp.handleEntityTrackerEntries(new Arrow()).entityId);
        Arrow server = new Arrow();
        ByteBuf wire = Unpooled.buffer();
        server.writeSpawnData(wire);
        Packet230ModLoader onWire = Packet230ModLoader.decode(bytes(wire));
        assertEquals("Spawn".hashCode(), onWire.modId);
        assertEquals(-56, onWire.packetType);
        Arrow client = new Arrow();
        client.readSpawnData(wire);
        assertNotNull(client.spawned);
        assertEquals(-56, client.spawned.packetType);
        assertArrayEquals(new int[] {42}, client.spawned.dataInt);
        assertArrayEquals(new float[] {1.5F, -2F}, client.spawned.dataFloat);
    }

    private static byte[] bytes(ByteBuf b) {
        byte[] out = new byte[b.readableBytes()];
        b.getBytes(b.readerIndex(), out);
        return out;
    }

    @Test
    void unregisteredEntitiesAndClientBuildsSendNothingAndAnUnknownTypeDoesNotSpawn() {
        ByteBuf wire = Unpooled.buffer();
        new Unregistered().writeSpawnData(wire);
        assertEquals(0, wire.readableBytes(), "the server registered no tracker entry for the class");
        ModLoaderMp.registerEntityTrackerEntry(ClientOnly.class, 201);
        new ClientOnly().writeSpawnData(wire);
        assertEquals(0, wire.readableBytes(), "a client build has no getSpawnPacket: single player never sent a packet");
        // a packet whose type no client class is registered for does not call spawn
        Packet230ModLoader p = new Packet230ModLoader();
        p.modId = "Spawn".hashCode();
        p.packetType = 77;
        Arrow client = new Arrow();
        client.readSpawnData(Unpooled.wrappedBuffer(p.encode()));
        assertNull(client.spawned);
    }

    @Test
    void registrationErrorsKeepTheFirstEntry() {
        ModLoaderMp.registerNetClientHandlerEntity(Arrow.class, 300);   // greater than 255: refused
        assertNull(ModLoaderMp.handleNetClientHandlerEntities(300 - 256));
        ModLoaderMp.registerNetClientHandlerEntity(ClientOnly.class, true, 90);
        ModLoaderMp.registerNetClientHandlerEntity(Arrow.class, false, 90);   // already registered: the first stays
        var row = ModLoaderMp.handleNetClientHandlerEntities(90);
        assertSame(ClientOnly.class, row.entityClass);
        assertTrue(row.entityHasOwner);
    }
}
