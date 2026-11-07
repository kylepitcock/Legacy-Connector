package org.kylepitcock.legacyconnector.v1_12_2.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Modern players made for 1.12.2 player objects a mod constructs on the client (kept apart: client classes are never loaded by a server). */
public final class Players1122 {
    private Players1122() {}

    /** A remote player of the client world, or null when the level is not the client's. */
    public static Player remote(Level level, GameProfile profile) {
        return level instanceof ClientLevel client ? new RemotePlayer(client, profile) : null;
    }
}
