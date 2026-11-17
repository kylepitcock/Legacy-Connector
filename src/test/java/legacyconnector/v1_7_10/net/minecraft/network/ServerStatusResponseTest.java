package legacyconnector.v1_7_10.net.minecraft.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText;
import legacyconnector.v1_7_10.net.minecraftforge.common.ForgeVersion;
import org.junit.jupiter.api.Test;

/** The server list status object mods read and replace, and the update-check types of {@code ForgeVersion}. */
class ServerStatusResponseTest {
    @Test
    void playerCountDataKeepsWhatAModSetsAndTheSample() {
        ServerStatusResponse response = new ServerStatusResponse();
        ServerStatusResponse.PlayerCountData data = new ServerStatusResponse.PlayerCountData(20, 3);
        GameProfile[] sample = {new GameProfile(UUID.randomUUID(), "Steve")};
        data.setPlayers(sample);
        response.setPlayerCountData(data);
        assertSame(data, response.getPlayerCountData());
        assertEquals(20, data.getMaxPlayers());
        assertEquals(3, data.getOnlinePlayerCount());
        assertSame(sample, data.getPlayers());
    }

    @Test
    void withoutAServerTheCountsAreZeroAndTheDescriptionIsWhatWasSet() {
        ServerStatusResponse response = new ServerStatusResponse();
        assertEquals(0, response.getPlayerCountData().getMaxPlayers());
        response.setServerDescription(new ChatComponentText("hello"));
        assertEquals("hello", response.getServerDescription().getUnformattedText());
        assertEquals("1.7.10", response.getProtocolVersionInfo().getName());
        assertNull(response.getFavicon());
    }

    @Test
    void forgeVersionStatusFlagsFollowForge() {
        assertTrue(ForgeVersion.Status.OUTDATED.shouldDraw());
        assertTrue(ForgeVersion.Status.BETA_OUTDATED.isAnimated());
        assertEquals(2, ForgeVersion.Status.UP_TO_DATE.getSheetOffset());
        assertEquals(ForgeVersion.Status.PENDING, ForgeVersion.getResult(null).status);
    }
}
