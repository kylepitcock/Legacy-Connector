package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Client-side 1.20.1 members that 1.21.1 reshaped, ATM 9 gap round 7 (rows in {@code mc-calls.tsv}); kept apart from {@link Round7Compat} so a server never loads them. */
public final class Round7ClientCompat {
    private Round7ClientCompat() {
    }

    /** {@code new SheetedDecalTextureGenerator(delegate, pose, normal, scale)}: 1.21.1 takes the pose entry (both versions invert it). */
    public static SheetedDecalTextureGenerator newSheetedDecal(VertexConsumer delegate, Matrix4f pose, Matrix3f normal, float scale) {
        PoseStack stack = new PoseStack();
        stack.last().pose().set(pose);
        stack.last().normal().set(normal);
        return new SheetedDecalTextureGenerator(delegate, stack.last(), scale);
    }

    /** {@code SkullBlockRenderer.getRenderType(type, profile)}: the profile is a resolvable profile now. */
    public static RenderType skullRenderType(SkullBlock.Type type, GameProfile profile) {
        return SkullBlockRenderer.getRenderType(type, profile == null ? null : new ResolvableProfile(profile));
    }

    /** {@code SkinManager.getInsecureSkinInformation(profile)}: the textures a profile carries, by type (the session service answers a record now). */
    public static Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> insecureSkinInformation(SkinManager manager, GameProfile profile) {
        MinecraftProfileTextures textures = Minecraft.getInstance().getMinecraftSessionService().getTextures(profile);
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> out = new EnumMap<>(MinecraftProfileTexture.Type.class);
        if (textures.skin() != null) out.put(MinecraftProfileTexture.Type.SKIN, textures.skin());
        if (textures.cape() != null) out.put(MinecraftProfileTexture.Type.CAPE, textures.cape());
        if (textures.elytra() != null) out.put(MinecraftProfileTexture.Type.ELYTRA, textures.elytra());
        return out;
    }

    /** {@code BakedModel.useAmbientOcclusion(state)}: the model's own flag. */
    public static boolean useAmbientOcclusion(BakedModel model, BlockState state) {
        return model.useAmbientOcclusion();
    }

    /** {@code BakedModel.useAmbientOcclusion(state, renderType)}: the per-render-type answer when the model gives one, else its own flag. */
    public static boolean useAmbientOcclusion(BakedModel model, BlockState state, RenderType renderType) {
        var answer = model.useAmbientOcclusion(state, ModelData.EMPTY, renderType);
        return answer.isDefault() ? model.useAmbientOcclusion() : answer.isTrue();
    }
}
