package legacyconnector.v1_7_10.net.minecraft.client.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.entity.player.Player;

public abstract class AbstractClientPlayer extends EntityPlayer {
    protected AbstractClientPlayer(Player modern) {
        super(modern);
    }

    /** Legacy mods created stand-in players (mannequins drawn by tile entity renderers): a modern remote player is the host. */
    public AbstractClientPlayer(legacyconnector.v1_7_10.net.minecraft.world.World world, com.mojang.authlib.GameProfile profile) {
        this(Remote.create(profile));
    }

    private static final class Remote {
        static Player create(com.mojang.authlib.GameProfile profile) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            return new net.minecraft.client.player.RemotePlayer(level, profile);
        }
    }

    public legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation getLocationSkin() {
        return new legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation("textures/entity/steve.png");
    }

    public static legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation getLocationSkin(String username) {
        return new legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation("textures/entity/steve.png");
    }
}
