package legacyconnector.v1_7_10.net.minecraftforge.common.util;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import legacyconnector.v1_7_10.net.minecraft.world.WorldServer;
import net.minecraft.server.level.ServerLevel;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

/** Forge's cache of fake players, backed by NeoForge's. */
public class FakePlayerFactory {
    private static final GameProfile MINECRAFT = new GameProfile(UUID.fromString("41C82C87-7AfB-4024-BA57-13D2C99CAE77"), "[Minecraft]");

    public static FakePlayer get(WorldServer world, GameProfile username) {
        ServerLevel level = (ServerLevel) world.mcc$fullLevel();
        return (FakePlayer) EntityBridge.wrap(net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, username));
    }

    public static FakePlayer getMinecraft(WorldServer world) {
        return get(world, MINECRAFT);
    }

    public static void unloadWorld(WorldServer world) {
    }
}
