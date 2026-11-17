package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EventBridge1710;

/**
 * 1.7.10's {@code RenderPlayer.renderSpecials}: {@code RenderPlayerEvent.Specials.Pre/Post} are posted here, after the model, in the model's own
 * matrix (the pose layers draw in), not at the camera as the other player events. Listeners that draw a model of their own at the player (Legends'
 * Kiryu mech) depend on that space.
 */
public class LegacyPlayerSpecialsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public LegacyPlayerSpecialsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick,
            float ageInTicks, float netHeadYaw, float headPitch) {
        EventBridge1710.postPlayerSpecials(player, partialTick, pose, buffers, light);
    }
}
