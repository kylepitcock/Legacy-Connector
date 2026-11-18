package legacyconnector.v1_6_4.forward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import legacyconnector.v1_6_4.facade.net.minecraft.entity.DataWatcher;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet201PlayerInfo;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet20NamedEntitySpawn;
import legacyconnector.v1_7_10.net.minecraft.entity.DataWatcher.WatchableObject;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;
import org.junit.jupiter.api.Test;

/** The 1.6.4 wire formats added in gap round 3: the watched values, the player list entry and the player spawn packet. */
class GapRound3WireTest {
    @Test
    void watchedValuesRoundTripInTheLegacyFormat() throws IOException {
        List<WatchableObject> list = List.of(new WatchableObject(0, 3, (byte) 9), new WatchableObject(1, 4, (short) -2), new WatchableObject(2, 5, 70000),
                new WatchableObject(3, 6, 1.5F), new WatchableObject(4, 7, "Steve"), new WatchableObject(6, 8, new ChunkCoordinates(1, -2, 3)));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataWatcher.writeObjectsInListToStream(list, new DataOutputStream(bytes));
        byte[] data = bytes.toByteArray();
        assertEquals(3, data[0], "type 0 in the top three bits, id 3 in the low five");
        assertEquals(127, data[data.length - 1], "the end marker");
        List<WatchableObject> back = DataWatcher.readWatchableObjects(new DataInputStream(new ByteArrayInputStream(data)));
        assertEquals(list.size(), back.size());
        assertEquals((byte) 9, back.get(0).getObject());
        assertEquals((short) -2, back.get(1).getObject());
        assertEquals(70000, back.get(2).getObject());
        assertEquals(1.5F, back.get(3).getObject());
        assertEquals("Steve", back.get(4).getObject());
        ChunkCoordinates c = (ChunkCoordinates) back.get(5).getObject();
        assertEquals(-2, c.posY);
        assertEquals(8, back.get(5).getDataValueId());
    }

    @Test
    void playerInfoAndNamedEntitySpawnAreRegisteredByIdAndRead() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Packet.writePacket(new Packet201PlayerInfo("Alex", true, 42), new DataOutputStream(bytes));
        Packet20NamedEntitySpawn spawn = new Packet20NamedEntitySpawn();
        spawn.entityId = 77;
        spawn.name = "Alex";
        spawn.xPosition = 320;
        spawn.rotation = 64;
        spawn.currentItem = 276;
        Packet.writePacket(spawn, new DataOutputStream(bytes));
        assertEquals(201, bytes.toByteArray()[0] & 255);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        Packet info = Packets164.readPacket(null, in, false, null);
        assertInstanceOf(Packet201PlayerInfo.class, info);
        assertEquals("Alex", ((Packet201PlayerInfo) info).playerName);
        assertEquals(42, ((Packet201PlayerInfo) info).ping);
        Packet read = Packets164.readPacket(null, in, false, null);
        assertInstanceOf(Packet20NamedEntitySpawn.class, read);
        Packet20NamedEntitySpawn s = (Packet20NamedEntitySpawn) read;
        assertEquals(77, s.entityId);
        assertEquals(320, s.xPosition);
        assertEquals(64, s.rotation);
        assertEquals(276, s.currentItem);
    }
}
