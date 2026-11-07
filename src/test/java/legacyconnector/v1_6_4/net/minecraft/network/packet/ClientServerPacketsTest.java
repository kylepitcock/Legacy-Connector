package legacyconnector.v1_6_4.net.minecraft.network.packet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition;
import org.junit.jupiter.api.Test;

/** Wire formats and mappings of the 1.6.4 window, placement, entity-movement, explosion, kick and ping packets. */
class ClientServerPacketsTest {
    private static byte[] write(Packet p) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        p.writePacketData(new DataOutputStream(bytes));
        return bytes.toByteArray();
    }

    private static void read(Packet p, byte[] data) throws IOException {
        p.readPacketData(new DataInputStream(new ByteArrayInputStream(data)));
    }

    @Test
    void openWindowRoundTripsAndMapsTheWindowTypeToAMenu() throws IOException {
        Packet100OpenWindow p = new Packet100OpenWindow(5, 0, "container.chest", 54, true);
        byte[] data = write(p);
        assertEquals(2 + 2 + 2 * "container.chest".length() + 2, data.length);
        Packet100OpenWindow back = new Packet100OpenWindow();
        read(back, data);
        assertEquals(5, back.windowId);
        assertEquals("container.chest", back.windowTitle);
        assertEquals(54, back.slotsCount);
        assertTrue(back.useProvidedWindowTitle);
        assertEquals("minecraft:generic_9x6", Packet100OpenWindow.menuTypeKey(0, 54));
        assertEquals("minecraft:generic_9x3", Packet100OpenWindow.menuTypeKey(0, 27));
        assertEquals("minecraft:hopper", Packet100OpenWindow.menuTypeKey(9, 5));
        assertEquals("minecraft:anvil", Packet100OpenWindow.menuTypeKey(8, 3));
        assertNull(Packet100OpenWindow.menuTypeKey(42, 0));
    }

    @Test
    void windowClickKeepsEveryFieldAndMapsModesToClickTypes() throws IOException {
        Packet102WindowClick p = new Packet102WindowClick(3, 17, 1, 1, null, (short) 9);
        byte[] data = write(p);
        assertArrayEquals(new byte[] {3, 0, 17, 1, 0, 9, 1, -1, -1}, data);
        Packet102WindowClick back = new Packet102WindowClick();
        read(back, data);
        assertEquals(3, back.window_Id);
        assertEquals(17, back.inventorySlot);
        assertEquals(1, back.mouseClick);
        assertEquals(9, back.action);
        assertEquals(1, back.holdingShift);
        assertNull(back.itemStack);
        // the modern ClickType enum lists PICKUP, QUICK_MOVE, SWAP, CLONE, THROW, QUICK_CRAFT, PICKUP_ALL: the legacy modes 0 to 6
        for (int mode = 0; mode <= 6; mode++) assertEquals(mode, Packet102WindowClick.clickOrdinal(mode));
        assertEquals(0, Packet102WindowClick.clickOrdinal(99));
        assertEquals(0, Packet102WindowClick.clickOrdinal(-1));
    }

    @Test
    void placeQuantizesTheClickOffsetToSixteenths() throws IOException {
        Packet15Place p = new Packet15Place(-12, 70, 345, 1, null, 0.5F, 1.0F / 16.0F, 0.0F);
        byte[] data = write(p);
        Packet15Place back = new Packet15Place();
        read(back, data);
        assertEquals(-12, back.getXPosition());
        assertEquals(70, back.getYPosition());
        assertEquals(345, back.getZPosition());
        assertEquals(1, back.getDirection());
        assertNull(back.getItemStack());
        assertEquals(0.5F, back.getXOffset());
        assertEquals(0.0625F, back.getYOffset());
        assertEquals(0.0F, back.getZOffset());
        Packet15Place air = new Packet15Place(-1, 255, -1, 255, null, 0, 0, 0);
        Packet15Place airBack = new Packet15Place();
        read(airBack, write(air));
        assertEquals(255, airBack.direction);
        assertEquals(255, airBack.yPosition);
    }

    @Test
    void entityMovePacketsShareTheIdPrefixAndAddTheirFields() throws IOException {
        Packet31RelEntityMove move = new Packet31RelEntityMove(77, (byte) 3, (byte) -4, (byte) 5);
        assertArrayEquals(new byte[] {0, 0, 0, 77, 3, -4, 5}, write(move));
        Packet31RelEntityMove moveBack = new Packet31RelEntityMove();
        read(moveBack, write(move));
        assertEquals(77, moveBack.entityId);
        assertEquals(-4, moveBack.yPosition);

        Packet32EntityLook look = new Packet32EntityLook(8, (byte) 64, (byte) -32);
        assertArrayEquals(new byte[] {0, 0, 0, 8, 64, -32}, write(look));
        Packet32EntityLook lookBack = new Packet32EntityLook();
        read(lookBack, write(look));
        assertEquals(64, lookBack.yaw);
        assertEquals(-32, lookBack.pitch);
        assertTrue(lookBack.rotating);

        Packet33RelEntityMoveLook both = new Packet33RelEntityMoveLook(9, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertArrayEquals(new byte[] {0, 0, 0, 9, 1, 2, 3, 4, 5}, write(both));
        Packet33RelEntityMoveLook bothBack = new Packet33RelEntityMoveLook();
        read(bothBack, write(both));
        assertEquals(3, bothBack.zPosition);
        assertEquals(5, bothBack.pitch);
        assertEquals(31, Packet.mcc$idOf(Packet31RelEntityMove.class));
        assertEquals(33, Packet.mcc$idOf(Packet33RelEntityMoveLook.class));
    }

    @Test
    void explosionStoresBlocksRelativeToTheCentre() throws IOException {
        Packet60Explosion p = new Packet60Explosion(10.5, 64.0, -3.5, 4.0F,
                List.of(new ChunkPosition(11, 63, -4), new ChunkPosition(9, 66, -2)), legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(0.25, 0.5, -0.75));
        byte[] data = write(p);
        assertEquals(p.getPacketSize(), data.length);
        Packet60Explosion back = new Packet60Explosion();
        read(back, data);
        assertEquals(10.5, back.explosionX);
        assertEquals(4.0F, back.explosionSize);
        assertEquals(2, back.chunkPositionRecords.size());
        ChunkPosition second = (ChunkPosition) back.chunkPositionRecords.get(1);
        assertEquals(9, second.chunkPosX);
        assertEquals(66, second.chunkPosY);
        assertEquals(-2, second.chunkPosZ);
        assertEquals(-0.75F, back.getPlayerVelocityZ());
    }

    @Test
    void kickAndPingCarryTheirStrings() throws IOException {
        Packet255KickDisconnect kick = new Packet255KickDisconnect("Flying is not enabled");
        Packet255KickDisconnect kickBack = new Packet255KickDisconnect();
        read(kickBack, write(kick));
        assertEquals("Flying is not enabled", kickBack.reason);
        assertEquals(255, Packet.mcc$idOf(Packet255KickDisconnect.class));

        Packet254ServerPing ping = new Packet254ServerPing(78, "example.org", 25565);
        assertEquals(254, ping.getPacketId());
        Packet254ServerPing pingBack = new Packet254ServerPing();
        read(pingBack, write(ping));
        assertEquals(78, pingBack.field_82559_a);
        assertEquals("example.org", pingBack.field_82558_b);
        assertEquals(25565, pingBack.field_82557_c);
    }

    @Test
    void mapChunkWireCarriesTheCompressedPayload() throws IOException {
        byte[] payload = legacyconnector.v1_6_4.forward.ChunkWire164.deflate(new byte[4096 + 4096]);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(-3);
        out.writeInt(12);
        out.writeBoolean(true);
        out.writeShort(1);
        out.writeShort(0);
        out.writeInt(payload.length);
        out.write(payload);
        Packet51MapChunk p = new Packet51MapChunk();
        read(p, bytes.toByteArray());
        assertEquals(-3, p.xCh);
        assertEquals(12, p.zCh);
        assertTrue(p.includeInitialize);
        assertEquals(1, p.yChMin);
        assertArrayEquals(payload, p.getCompressedChunkData());
        assertArrayEquals(bytes.toByteArray(), write(p));
        assertEquals(bytes.size(), p.getPacketSize());
    }

    @Test
    void digPacketMapsItsStatusToTheModernAction() throws IOException {
        Packet14BlockDig p = new Packet14BlockDig(2, 100, 64, -100, 5);
        byte[] data = write(p);
        assertEquals(11, data.length);
        Packet14BlockDig back = new Packet14BlockDig();
        read(back, data);
        assertEquals(2, back.getStatus());
        assertEquals(100, back.getXPosition());
        assertEquals(64, back.getYPosition());
        assertEquals(-100, back.getZPosition());
        assertEquals(5, back.getFace());
        assertEquals("START_DESTROY_BLOCK", Packet14BlockDig.actionName(0));
        assertEquals("STOP_DESTROY_BLOCK", Packet14BlockDig.actionName(2));
        assertEquals("DROP_ITEM", Packet14BlockDig.actionName(4));
        assertNull(Packet14BlockDig.actionName(9));
    }

    @Test
    void blockChangeReadsTheLegacyLayout() throws IOException {
        Packet53BlockChange p = new Packet53BlockChange();
        read(p, new byte[] {0, 0, 0, 5, 64, 0, 0, 0, 7, 0, 3, 9});
        assertEquals(5, p.xPosition);
        assertEquals(64, p.yPosition);
        assertEquals(7, p.zPosition);
        assertEquals(3, p.type);
        assertEquals(9, p.metadata);
        assertArrayEquals(new byte[] {0, 0, 0, 5, 64, 0, 0, 0, 7, 0, 3, 9}, write(p));
    }
}
