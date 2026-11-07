package legacyconnector.v1_6_4.net.minecraft.network.packet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import legacyconnector.v1_6_4.net.minecraft.client.audio.SoundPool;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import org.junit.jupiter.api.Test;

/** The wire formats of the 1.6.4 packets mods serialize into their own channels, and the packet id / size statics CodeChicken's MetaPacket uses. */
class LegacyPacketsTest {
    private static byte[] write(Packet p) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        p.writePacketData(new DataOutputStream(bytes));
        return bytes.toByteArray();
    }

    private static void read(Packet p, byte[] data) throws IOException {
        p.readPacketData(new DataInputStream(new ByteArrayInputStream(data)));
    }

    @Test
    void entityVelocityKeepsEighthousandthsAndClampsLikeVanilla() throws IOException {
        Packet28EntityVelocity p = new Packet28EntityVelocity(7, 0.5D, -1.0D, 10.0D);
        assertEquals(4000, p.motionX);
        assertEquals(-8000, p.motionY);
        assertEquals((int) (3.9D * 8000.0D), p.motionZ, "motion is limited to 3.9 blocks per tick");
        byte[] data = write(p);
        assertEquals(10, data.length);
        assertEquals(p.getPacketSize(), data.length);
        Packet28EntityVelocity back = new Packet28EntityVelocity();
        read(back, data);
        assertEquals(7, back.entityId);
        assertEquals(p.motionX, back.motionX);
        assertEquals(p.motionY, back.motionY);
        assertEquals(p.motionZ, back.motionZ);
    }

    @Test
    void gameEventIsTwoBytes() throws IOException {
        Packet70GameEvent p = new Packet70GameEvent(3, 1);
        assertArrayEquals(new byte[] {3, 1}, write(p));
        Packet70GameEvent back = new Packet70GameEvent();
        read(back, new byte[] {2, 0});
        assertEquals(2, back.eventType);
        assertEquals(0, back.gameMode);
    }

    @Test
    void levelSoundQuantizesPositionAndPitch() throws IOException {
        Packet62LevelSound p = new Packet62LevelSound("random.explode", 10.5D, 64.0D, -3.25D, 2.0F, 0.5F);
        assertEquals(10.5D, p.getEffectX(), 1e-9);
        assertEquals(64.0D, p.getEffectY(), 1e-9);
        assertEquals(-3.25D, p.getEffectZ(), 1e-9);
        assertEquals(31 / 63.0F, p.getPitch(), 1e-6);
        Packet62LevelSound back = new Packet62LevelSound();
        read(back, write(p));
        assertEquals("random.explode", back.getSoundName());
        assertEquals(2.0F, back.getVolume());
        assertEquals(p.getPitch(), back.getPitch());
        assertEquals(p.getEffectZ(), back.getEffectZ());
    }

    @Test
    void experienceWritesLevelBeforeTotal() throws IOException {
        Packet43Experience p = new Packet43Experience(0.25F, 120, 9);
        Packet43Experience back = new Packet43Experience();
        byte[] data = write(p);
        assertEquals(8, data.length);
        read(back, data);
        assertEquals(0.25F, back.experience);
        assertEquals(120, back.experienceTotal);
        assertEquals(9, back.experienceLevel);
    }

    @Test
    void movementPacketsWriteOnlyTheirFields() throws IOException {
        Packet12PlayerLook look = new Packet12PlayerLook(90.0F, -10.0F, true);
        assertTrue(look.rotating);
        assertFalse(look.moving);
        assertEquals(9, write(look).length);
        Packet11PlayerPosition pos = new Packet11PlayerPosition(1.0D, 2.0D, 3.62D, 4.0D, false);
        assertTrue(pos.moving);
        assertEquals(33, write(pos).length);
        Packet13PlayerLookMove both = new Packet13PlayerLookMove(1.0D, 2.0D, 3.62D, 4.0D, 5.0F, 6.0F, true);
        byte[] data = write(both);
        assertEquals(41, data.length);
        assertEquals(both.getPacketSize(), data.length);
        Packet13PlayerLookMove back = new Packet13PlayerLookMove();
        read(back, data);
        assertEquals(1.0D, back.xPosition);
        assertEquals(3.62D, back.stance);
        assertEquals(4.0D, back.zPosition);
        assertEquals(5.0F, back.yaw);
        assertEquals(6.0F, back.pitch);
        assertTrue(back.onGround);
    }

    @Test
    void useEntityRoundTrips() throws IOException {
        Packet7UseEntity p = new Packet7UseEntity(1, 42, 1);
        Packet7UseEntity back = new Packet7UseEntity();
        read(back, write(p));
        assertEquals(1, back.playerEntityId);
        assertEquals(42, back.targetEntity);
        assertEquals(1, back.isLeftClick);
    }

    @Test
    void packetIdComesFromTheClassName() throws IOException {
        assertEquals(250, Packet.mcc$idOf(Packet250CustomPayload.class));
        assertEquals(131, Packet.mcc$idOf(Packet131MapData.class));
        assertEquals(12, Packet.mcc$idOf(Packet12PlayerLook.class));
        class Mine extends Packet43Experience {}
        assertEquals(43, Packet.mcc$idOf(Mine.class), "a subclass keeps the id of the vanilla packet it extends");
        Packet custom = new Packet() {};
        assertEquals(250, Packet.mcc$idOf(custom.getClass()));

        long before = Packet.sentSize;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Packet.writePacket(new Packet70GameEvent(1, 0), new DataOutputStream(bytes));
        assertArrayEquals(new byte[] {70, 1, 0}, bytes.toByteArray());
        assertEquals(before + 3, Packet.sentSize);
    }

    @Test
    void soundPoolGroupsFilesByEventName() {
        SoundPool pool = new SoundPool();
        pool.addSound("mod/boom1.ogg");
        pool.addSound("mod/boom2.ogg");
        pool.addSound("mod/hum.ogg");
        assertEquals(3, pool.numberOfSoundPoolEntries);
        String boom = pool.getRandomSoundFromSoundPool("mod.boom").getSoundName();
        assertTrue("mod/boom1.ogg".equals(boom) || "mod/boom2.ogg".equals(boom));
        assertEquals("mod/hum.ogg", pool.getRandomSoundFromSoundPool("mod.hum").getSoundName());
        assertEquals(java.util.List.of("mod/boom1.ogg", "mod/boom2.ogg", "mod/hum.ogg"), pool.names());
        assertNull(pool.getRandomSoundFromSoundPool("mod.missing"));
    }
}
