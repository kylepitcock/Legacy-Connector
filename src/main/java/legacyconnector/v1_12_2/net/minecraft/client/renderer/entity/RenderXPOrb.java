package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityXPOrb;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 experience orb renderer (mods subclass it for their own orbs: The Betweenlands' false XP orbs); the orb's quad with its size-indexed icon. */
public class RenderXPOrb extends Render<EntityXPOrb> {
    private static final ResourceLocation EXPERIENCE_ORB_TEXTURES = new ResourceLocation("textures/entity/experience_orb.png");

    public RenderXPOrb(RenderManager renderManagerIn) {
        super(renderManagerIn);
        this.shadowSize = 0.15F;
        this.shadowOpaque = 0.75F;
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityXPOrb entity) {
        return EXPERIENCE_ORB_TEXTURES;
    }
}
