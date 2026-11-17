package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;

/** Client-side 1.20.1 members that 1.21.1 reshaped, ATM 9 gap round 8 (rows in {@code mc-calls.tsv}); apart from the common helpers so a server never loads them. */
public final class Round8ClientCompat {
    private Round8ClientCompat() {
    }

    /** {@code Sheets.armorTrimsSheet()}: the non-decal sheet ({@code armorTrimsSheet(boolean decal)} now). */
    public static RenderType armorTrimsSheet() {
        return Sheets.armorTrimsSheet(false);
    }

    /** {@code PanoramaRenderer.render(partialTick, alpha)}: the size and the graphics context come from the window now. */
    public static void panoramaRender(PanoramaRenderer renderer, float partialTick, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        renderer.render(g, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), alpha, partialTick);
        g.flush();
    }

    /** {@code new RenderBuffers()}: the buffer count is a parameter now (one section builder pool entry per worker). */
    public static RenderBuffers newRenderBuffers() {
        return new RenderBuffers(Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2)));
    }

    /** {@code new MultiBufferSource.BufferSource(fallback, layerBuilders)}: byte buffers now; the old builders only supplied memory (layers get theirs on demand). */
    public static MultiBufferSource.BufferSource newBufferSource(CompatBufferBuilder ignored, Map<RenderType, CompatBufferBuilder> ignoredMap) {
        return MultiBufferSource.immediateWithBuffers(new LinkedHashMap<>(), new ByteBufferBuilder(256));
    }

    /**
     * {@code SkinManager.registerTexture(texture, type)}: registers a downloading texture under the hash of the url and answers its location at once (the old
     * synchronous contract); 1.21.1 only offers the asynchronous, cached profile lookup.
     */
    public static ResourceLocation registerTexture(SkinManager manager, MinecraftProfileTexture texture, MinecraftProfileTexture.Type type) {
        Minecraft mc = Minecraft.getInstance();
        String hash = texture.getHash();
        String folder = type.name().toLowerCase(java.util.Locale.ROOT) + "s";
        ResourceLocation id = ResourceLocation.withDefaultNamespace(folder + "/" + hash);
        if (mc.getTextureManager().getTexture(id, null) == null) {
            File file = new File(new File(new File(mc.gameDirectory, "assets"), "skins"), (hash.length() > 2 ? hash.substring(0, 2) : "xx") + File.separator + hash);
            mc.getTextureManager().register(id, new HttpTexture(file, texture.getUrl(), DefaultPlayerSkin.getDefaultTexture(),
                    type == MinecraftProfileTexture.Type.SKIN, null));
        }
        return id;
    }

    /** {@code User.getGameProfile()}: the profile is made from the id and the name (the textures are fetched through the session service anyway). */
    public static com.mojang.authlib.GameProfile userProfile(net.minecraft.client.User user) {
        return new com.mojang.authlib.GameProfile(user.getProfileId(), user.getName());
    }

    /** {@code Lighting.setupLevel(Matrix4f)}: the light directions of the level are fixed now. */
    public static void setupLevelLighting(org.joml.Matrix4f ignored) {
        com.mojang.blaze3d.platform.Lighting.setupLevel();
    }

    /** {@code MobEffectTextureManager.get(MobEffect)}: keyed by holder now. */
    public static net.minecraft.client.renderer.texture.TextureAtlasSprite effectSprite(net.minecraft.client.resources.MobEffectTextureManager manager,
            net.minecraft.world.effect.MobEffect effect) {
        return manager.get(HolderCompat.holder(effect));
    }
}
