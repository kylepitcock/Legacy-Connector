package legacyconnector.v1_7_10.net.minecraftforge.common.util;

import com.mojang.authlib.GameProfile;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.world.WorldServer;
import net.minecraft.server.level.ServerLevel;

/** Forge's fake player: a server player no connection belongs to (machines use it to act for a player). */
public class FakePlayer extends EntityPlayerMP {
    public FakePlayer(WorldServer world, GameProfile profile) {
        super(net.neoforged.neoforge.common.util.FakePlayerFactory.get((ServerLevel) world.mcc$fullLevel(), profile));
    }

    /** The view of an existing modern fake player. */
    public FakePlayer(net.minecraft.server.level.ServerPlayer modern) {
        super(modern);
    }
}
