package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelHorse;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.LayeredTexture;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityHorse;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.client.LegacyEntityRenderer1122;

/**
 * 1.12.2 horse renderer. A vanilla horse (a modern entity) is drawn by the modern renderer; a mod's horse uses the 1.12 model with its
 * coat and markings layered into one texture per variant, as 1.12 did (mods usually override the texture with their own 1.12 one;
 * the 1.21 vanilla horse textures have the post-1.14 layout).
 */
public class RenderHorse extends RenderLiving<EntityHorse> {
    private static final Map<String, ResourceLocation> LAYERED_LOCATION_CACHE = new HashMap<>();
    private static final String[] HORSE_TEXTURES = {"white", "creamy", "chestnut", "brown", "black", "gray", "darkbrown"};
    private static final String[] HORSE_TEXTURES_ABBR = {"hwh", "hcr", "hch", "hbr", "hbl", "hgr", "hdb"};
    private static final String[] HORSE_MARKING_TEXTURES = {null, "white", "whitefield", "whitedots", "blackdots"};
    private static final String[] HORSE_MARKING_TEXTURES_ABBR = {"", "wo_", "wmo", "wdo", "bdo"};

    public RenderHorse(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelHorse(), 0.75F);
    }

    @Override
    public void doRender(EntityHorse entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.mcc$entity != null && !(entity.mcc$entity instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122)
                && getClass() == RenderHorse.class) {
            LegacyEntityRenderer1122.drawModern(entity.mcc$entity, x, y, z, entityYaw, partialTicks);
            return;
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityHorse entity) {
        int variant = entity.getHorseVariant();
        int color = Math.floorMod(variant & 255, HORSE_TEXTURES.length);
        int markings = Math.floorMod((variant & 65280) >> 8, HORSE_MARKING_TEXTURES.length);
        String key = "horse/" + HORSE_TEXTURES_ABBR[color] + HORSE_MARKING_TEXTURES_ABBR[markings];
        synchronized (LAYERED_LOCATION_CACHE) {
            ResourceLocation location = LAYERED_LOCATION_CACHE.get(key);
            if (location == null) {
                location = new ResourceLocation("legacyconnector", key);
                String coat = "textures/entity/horse/horse_" + HORSE_TEXTURES[color] + ".png";
                String marks = HORSE_MARKING_TEXTURES[markings] == null ? null
                        : "textures/entity/horse/horse_markings_" + HORSE_MARKING_TEXTURES[markings] + ".png";
                Minecraft.getMinecraft().getTextureManager().loadTexture(location, new LayeredTexture(coat, marks));
                LAYERED_LOCATION_CACHE.put(key, location);
            }
            return location;
        }
    }
}
