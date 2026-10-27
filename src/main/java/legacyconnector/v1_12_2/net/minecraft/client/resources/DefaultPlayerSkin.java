package legacyconnector.v1_12_2.net.minecraft.client.resources;

import java.util.UUID;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** The skins players without their own get: the game's default skin for the UUID (the same one the game draws). */
public class DefaultPlayerSkin {
    private static ResourceLocation legacy(net.minecraft.resources.ResourceLocation id) {
        return new ResourceLocation(id.getNamespace(), id.getPath());
    }

    public static ResourceLocation getDefaultSkinLegacy() {
        return new ResourceLocation("textures/entity/steve.png");
    }

    public static ResourceLocation getDefaultSkin(UUID id) {
        return legacy(net.minecraft.client.resources.DefaultPlayerSkin.get(id).texture());
    }

    /** "slim" or "default". */
    public static String getSkinType(UUID id) {
        return net.minecraft.client.resources.DefaultPlayerSkin.get(id).model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM ? "slim" : "default";
    }
}
