package legacyconnector.v1_12_2.net.minecraftforge.common.util;

import com.mojang.authlib.GameProfile;
import legacyconnector.v1_12_2.net.minecraft.world.WorldServer;

/** Forge's fake player cache: NeoForge's own, so a profile in a world is one fake player whoever asks for it. */
public class FakePlayerFactory {
    private static final GameProfile MINECRAFT = new GameProfile(java.util.UUID.fromString("41C82C87-7AfB-4024-BA57-13D2C99CAE77"), "[Minecraft]");

    public static FakePlayer getMinecraft(WorldServer world) {
        return get(world, MINECRAFT);
    }

    public static FakePlayer get(WorldServer world, GameProfile profile) {
        if (world == null || !(world.mcc$level() instanceof net.minecraft.server.level.ServerLevel level)) return null;
        var modern = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, profile);
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(modern) instanceof FakePlayer p ? p : new FakePlayer(modern);
    }

    public static void unloadWorld(WorldServer world) {
        if (world != null && world.mcc$level() instanceof net.minecraft.server.level.ServerLevel level) {
            net.neoforged.neoforge.common.util.FakePlayerFactory.unloadLevel(level);
        }
    }
}
