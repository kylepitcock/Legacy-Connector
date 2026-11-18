package legacyconnector.v1_7_10.net.minecraft.client.entity;

import net.minecraft.world.entity.player.Player;

public class EntityOtherPlayerMP extends AbstractClientPlayer {
    public EntityOtherPlayerMP(Player modern) {
        super(modern);
    }

    /** 1.6.4 shape {@code new EntityOtherPlayerMP(world, name)}: an offline profile of that name. */
    public EntityOtherPlayerMP(legacyconnector.v1_7_10.net.minecraft.world.World world, String name) {
        this(world, new com.mojang.authlib.GameProfile(net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(name == null ? "" : name), name));
    }

    /** A client-side player made by a mod (Fisk's Heroes' book previews), hosted by a detached modern remote player. */
    public EntityOtherPlayerMP(legacyconnector.v1_7_10.net.minecraft.world.World world, com.mojang.authlib.GameProfile profile) {
        super(world, profile);
    }
}
