package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.Matrix3f;

/**
 * Client-side helpers of gap round 15 (1.20.1 mods on 1.21.1): members of {@code net.minecraft.client.*} and {@code com.mojang.blaze3d.*} that changed
 * shape. Reached from {@code mc-calls.tsv}, {@code mc-fields.tsv} and {@code mc-bridges.tsv} (rows under {@code # --- round 15 client}).
 */
public final class Round15ClientCompat {
    private Round15ClientCompat() {}

    // ---- Gui.HeartType ---------------------------------------------------------------------------------------------------------------

    /** {@code Gui.HeartType.getX(half, blinking)}: the x offset of the heart in the icons sheet (1.21.1 draws sprites instead). */
    public static int heartTypeX(Gui.HeartType type, boolean half, boolean blinking) {
        String name = type.name();
        int index = switch (name) {
            case "CONTAINER" -> 0;
            case "NORMAL" -> 2;
            case "POISIONED" -> 4;
            case "WITHERED" -> 6;
            case "ABSORBING" -> 8;
            case "FROZEN" -> 9;
            default -> Math.min(type.ordinal() * 2, 9);
        };
        int i;
        if ("CONTAINER".equals(name)) {
            i = blinking ? 1 : 0;
        } else {
            i = (half ? 1 : 0) + (blinking ? 2 : 0);
        }
        return 16 + (index * 2 + i) * 9;
    }

    // ---- gui texture constants that became sprites ------------------------------------------------------------------------------

    public static ResourceLocation accessibilityTexture() {
        return ResourceLocation.withDefaultNamespace("textures/gui/sprites/icon/accessibility.png");
    }

    /** {@code Screen.BACKGROUND_LOCATION} (the dirt tile): the menu background tile. */
    public static ResourceLocation screenBackground() {
        return Screen.MENU_BACKGROUND;
    }

    public static ResourceLocation panoramaOverlay() {
        return ResourceLocation.withDefaultNamespace("textures/gui/title/background/panorama_overlay.png");
    }

    private static Field panoramaField;

    /** {@code TitleScreen.panorama}: the panorama renderer is the shared static of {@code Screen} now. */
    public static PanoramaRenderer titlePanorama(net.minecraft.client.gui.screens.TitleScreen screen) {
        try {
            if (panoramaField == null) {
                Field f = Screen.class.getDeclaredField("PANORAMA");
                f.setAccessible(true);
                panoramaField = f;
            }
            return (PanoramaRenderer) panoramaField.get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    // ---- render buffers ---------------------------------------------------------------------------------------------------------

    /** {@code MultiBufferSource.BufferSource.fixedBuffers} (a {@code Map<RenderType, BufferBuilder>} in 1.20.1; the map of {@code ByteBufferBuilder}s now). */
    public static Map<Object, Object> bufferSourceFixed(MultiBufferSource.BufferSource source) {
        return FixedBuffersView.ofSource(source);
    }

    // ---- vertex formats ---------------------------------------------------------------------------------------------------------

    public static ImmutableList<String> vertexFormatNames(VertexFormat format) {
        return ImmutableList.copyOf(format.getElementAttributeNames());
    }

    /** {@code VertexFormat.offsets}: the byte offset of each element, in element order. */
    public static IntList vertexFormatOffsets(VertexFormat format) {
        IntArrayList list = new IntArrayList();
        for (VertexFormatElement element : format.getElements()) list.add(format.getOffset(element));
        return list;
    }

    // ---- sheets -----------------------------------------------------------------------------------------------------------------

    private static Holder<BannerPattern> pattern(ResourceKey<BannerPattern> key) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        return level.registryAccess().registryOrThrow(Registries.BANNER_PATTERN).getHolder(key).orElse(null);
    }

    public static Material bannerMaterial(ResourceKey<BannerPattern> key) {
        Holder<BannerPattern> holder = pattern(key);
        return holder == null ? Sheets.BANNER_BASE : Sheets.getBannerMaterial(holder);
    }

    public static Material shieldMaterial(ResourceKey<BannerPattern> key) {
        Holder<BannerPattern> holder = pattern(key);
        return holder == null ? Sheets.SHIELD_BASE : Sheets.getShieldMaterial(holder);
    }

    // ---- models -----------------------------------------------------------------------------------------------------------------

    /** {@code new BlockElementFace(cull, tint, texture, uv, faceData)}: 1.21.1 adds the owning element holder. */
    public static BlockElementFace newElementFace(Direction cull, int tint, String texture, BlockFaceUV uv, ExtraFaceData data) {
        return new BlockElementFace(cull, tint, texture, uv, data, new MutableObject<>());
    }

    private static Field texturesByName;

    /** {@code TextureAtlas.getTextureLocations()}: the names of the stitched sprites. */
    @SuppressWarnings("unchecked")
    public static Set<ResourceLocation> atlasLocations(TextureAtlas atlas) {
        try {
            if (texturesByName == null) {
                Field f = TextureAtlas.class.getDeclaredField("texturesByName");
                f.setAccessible(true);
                texturesByName = f;
            }
            return Set.copyOf(((Map<ResourceLocation, ?>) texturesByName.get(atlas)).keySet());
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Set.of();
        }
    }

    // ---- maps -------------------------------------------------------------------------------------------------------------------

    public static void mapUpdate(MapRenderer renderer, int id, MapItemSavedData data) {
        renderer.update(new MapId(id), data);
    }

    // ---- sounds -----------------------------------------------------------------------------------------------------------------

    /** {@code SoundEventListener.onPlaySound(sound, events)}: 1.21.1 passes the range the sound is audible in. */
    public static void subtitlePlaySound(SubtitleOverlay overlay, SoundInstance sound, WeighedSoundEvents events) {
        overlay.onPlaySound(sound, events, 16.0F);
    }

    public static void listenerPlaySound(net.minecraft.client.sounds.SoundEventListener listener, SoundInstance sound, WeighedSoundEvents events) {
        listener.onPlaySound(sound, events, 16.0F);
    }

    // ---- minecraft / player / server --------------------------------------------------------------------------------------------

    public static float pausePartialTick(Minecraft mc) {
        return mc.getTimer().getGameTimeDeltaPartialTick(false);
    }

    public static net.minecraft.resources.ResourceLocation insecureSkinLocation(SkinManager manager, GameProfile profile) {
        return manager.getInsecureSkin(profile).texture();
    }

    public static String serverBrand(LocalPlayer player) {
        return player.connection == null ? null : player.connection.serverBrand();
    }

    /** {@code IntegratedServer.getAverageTickTime()}: milliseconds (1.21.1 counts nanoseconds). */
    public static float averageTickTime(IntegratedServer server) {
        return server.getAverageTickTimeNanos() / 1_000_000.0F;
    }

    public static net.minecraft.client.renderer.chunk.SectionRenderDispatcher sectionDispatcher(LevelRenderer renderer) {
        return renderer.getSectionRenderDispatcher();
    }

    // ---- recipe book ------------------------------------------------------------------------------------------------------------

    public static Recipe<?> ghostRecipe(GhostRecipe ghost) {
        RecipeHolder<?> holder = ghost.getRecipe();
        return holder == null ? null : holder.value();
    }

    public static Recipe<?> buttonRecipe(RecipeButton button) {
        RecipeHolder<?> holder = button.getRecipe();
        return holder == null ? null : holder.value();
    }

    private static RecipeHolder<?> holder(Recipe<?> recipe) {
        if (recipe == null) return null;
        ResourceLocation id = RecipeCompat.getId(recipe);
        return new RecipeHolder<>(id != null ? id : ResourceLocation.fromNamespaceAndPath("legacyconnector", "unknown"), recipe);
    }

    public static void handlePlaceRecipe(MultiPlayerGameMode mode, int containerId, Recipe<?> recipe, boolean shift) {
        RecipeHolder<?> holder = holder(recipe);
        if (holder != null) mode.handlePlaceRecipe(containerId, holder, shift);
    }

    // ---- render system ----------------------------------------------------------------------------------------------------------

    private static Matrix3f inverseViewRotation;

    /** {@code RenderSystem.assertInInitPhase()}: a thread check of the old render system; nothing to check here. */
    public static void assertInInitPhase() {}

    public static void setInverseViewRotationMatrix(Matrix3f matrix) {
        inverseViewRotation = matrix == null ? null : new Matrix3f(matrix);
    }

    /** The matrix the old level renderer stored each frame: the inverse of the camera's view rotation (the camera's own orientation). */
    public static Matrix3f getInverseViewRotationMatrix() {
        if (inverseViewRotation != null) return new Matrix3f(inverseViewRotation);
        Camera camera = Minecraft.getInstance().gameRenderer == null ? null : Minecraft.getInstance().gameRenderer.getMainCamera();
        return camera == null ? new Matrix3f() : new Matrix3f().rotation(camera.rotation());
    }

    // ---- batch 2: widgets, layouts, shaders --------------------------------------------------------------------------------------

    /** {@code StateSwitchingButton.initTextureValues(x, y, xDiff, yDiff, texture)}: sheet regions became sprites; the vanilla recipe-book filter sprites stand in. */
    public static void initTextureValues(net.minecraft.client.gui.components.StateSwitchingButton button, int xTex, int yTex, int xDiff, int yDiff, ResourceLocation texture) {
        button.initTextureValues(new net.minecraft.client.gui.components.WidgetSprites(
                ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled"), ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled"),
                ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled_highlighted"), ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled_highlighted")));
    }

    /** {@code MultiLineEditBox.tick()} is gone (the cursor blink is computed when it renders). */
    public static void editBoxTick(net.minecraft.client.gui.components.MultiLineEditBox box) {}

    public static ResourceLocation furnaceLitSprite(ResourceLocation texture) {
        return ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    }

    public static ResourceLocation furnaceBurnSprite(ResourceLocation texture) {
        return ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    }

    /** {@code new LinearLayout(x, y, width, height, orientation)}: 1.21.1 takes the size and a position is set afterwards. */
    public static net.minecraft.client.gui.layouts.LinearLayout newLinearLayout(int x, int y, int width, int height, net.minecraft.client.gui.layouts.LinearLayout.Orientation orientation) {
        net.minecraft.client.gui.layouts.LinearLayout layout = new net.minecraft.client.gui.layouts.LinearLayout(width, height, orientation);
        layout.setPosition(x, y);
        return layout;
    }

    public static boolean elementIsPosition(VertexFormatElement element) {
        return element.usage() == VertexFormatElement.Usage.POSITION;
    }

    /** {@code new Sound(String name, ...)}: the name is a location now. */
    public static ResourceLocation soundLocation(String name) {
        return ResourceLocation.parse(name);
    }

    public static boolean onDrawHighlight(LevelRenderer renderer, Camera camera, net.minecraft.world.phys.HitResult hit, float partialTick,
                                          com.mojang.blaze3d.vertex.PoseStack stack, MultiBufferSource buffers) {
        return net.neoforged.neoforge.client.ClientHooks.onDrawHighlight(renderer, camera, hit, Minecraft.getInstance().getTimer(), stack, buffers);
    }
}
