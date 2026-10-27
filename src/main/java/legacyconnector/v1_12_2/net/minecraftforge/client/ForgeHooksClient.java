package legacyconnector.v1_12_2.net.minecraftforge.client;

import java.nio.FloatBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GLAllocation;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;

/** Forge's client hooks that rendering code calls directly. */
public class ForgeHooksClient {
    private static final FloatBuffer MATRIX_BUF = GLAllocation.createDirectFloatBuffer(16);
    /** Items drawn by a tile entity renderer (registerTESRItemStack): item -> meta -> tile entity class. */
    private static final Map<Item, Map<Integer, Class<?>>> TESR_ITEMS = new ConcurrentHashMap<>();

    public static void setRenderLayer(BlockRenderLayer layer) {
        if (layer == null) MinecraftForgeClient.RENDER_LAYER.remove();
        else MinecraftForgeClient.RENDER_LAYER.set(layer);
    }

    public static void setRenderPass(int pass) {
        MinecraftForgeClient.RENDER_PASS.set(pass);
    }

    @Deprecated
    public static Pair<? extends IBakedModel, Matrix4f> handlePerspective(IBakedModel model, ItemCameraTransforms.TransformType type) {
        TRSRTransformation tr = TRSRTransformation.from(model.getItemCameraTransforms().getTransform(type));
        Matrix4f mat = null;
        if (!tr.equals(TRSRTransformation.identity())) mat = tr.getMatrix();
        return Pair.of(model, mat);
    }

    /** Applies a model's display transform to the GL matrix (mirrored in x for the left hand) and returns the model to draw. */
    public static IBakedModel handleCameraTransforms(IBakedModel model, ItemCameraTransforms.TransformType cameraTransformType, boolean leftHandHackery) {
        Pair<? extends IBakedModel, Matrix4f> pair = model.handlePerspective(cameraTransformType);
        if (pair.getRight() != null) {
            Matrix4f matrix = new Matrix4f(pair.getRight());
            if (leftHandHackery) {
                Matrix4f flip = new Matrix4f();
                flip.setIdentity();
                flip.m00 = -1;
                matrix.mul(flip, matrix);
                matrix.mul(flip);
            }
            multiplyCurrentGlMatrix(matrix);
        }
        return pair.getLeft();
    }

    public static void multiplyCurrentGlMatrix(Matrix4f matrix) {
        synchronized (MATRIX_BUF) {
            MATRIX_BUF.clear();
            float[] t = new float[4];
            for (int i = 0; i < 4; i++) {
                matrix.getColumn(i, t);
                MATRIX_BUF.put(t);
            }
            MATRIX_BUF.flip();
            legacyconnector.v1_12_2.org.lwjgl.opengl.GL11.glMultMatrix(MATRIX_BUF);
        }
    }

    /** Writes the face normal of an item-format quad (from its corner positions) into each vertex. */
    public static void fillNormal(int[] faceData, EnumFacing facing) {
        Vector3f v1 = getVertexPos(faceData, 3);
        Vector3f t = getVertexPos(faceData, 1);
        Vector3f v2 = getVertexPos(faceData, 2);
        v1.sub(t);
        t.set(getVertexPos(faceData, 0));
        v2.sub(t);
        v1.cross(v2, v1);
        v1.normalize();
        int x = ((byte) Math.round(v1.x * 127)) & 0xFF;
        int y = ((byte) Math.round(v1.y * 127)) & 0xFF;
        int z = ((byte) Math.round(v1.z * 127)) & 0xFF;
        int normal = x | (y << 0x08) | (z << 0x10);
        for (int i = 0; i < 4; i++) faceData[i * 7 + 6] = normal;
    }

    private static Vector3f getVertexPos(int[] data, int vertex) {
        int idx = vertex * 7;
        return new Vector3f(Float.intBitsToFloat(data[idx]), Float.intBitsToFloat(data[idx + 1]), Float.intBitsToFloat(data[idx + 2]));
    }

    /** Forge's item rendering through a tile entity renderer; remembered for the item renderer (see mcc$tesrItem). */
    @Deprecated
    public static void registerTESRItemStack(Item item, int metadata, Class<?> tileEntityClass) {
        if (item != null && tileEntityClass != null) TESR_ITEMS.computeIfAbsent(item, k -> new ConcurrentHashMap<>()).put(metadata, tileEntityClass);
    }

    public static Class<?> mcc$tesrItem(Item item, int metadata) {
        Map<Integer, Class<?>> m = TESR_ITEMS.get(item);
        return m == null ? null : m.get(metadata);
    }

    public static boolean shouldCauseBlockBreakReset(legacyconnector.v1_12_2.net.minecraft.item.ItemStack from, legacyconnector.v1_12_2.net.minecraft.item.ItemStack to) {
        if (from == null || to == null || from.isEmpty() || to.isEmpty()) return from != to;
        return from.getItem() != to.getItem() || from.getMetadata() != to.getMetadata();
    }

    // ------------------------------------------------------------------ area R2b: hooks that post Forge's client events

    /** Draws a screen between its DrawScreenEvent Pre (cancelling skips the drawing) and Post. */
    public static void drawScreen(legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen screen, int mouseX, int mouseY, float partialTicks) {
        var bus = legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        if (!bus.post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Pre(screen, mouseX, mouseY, partialTicks))) {
            screen.drawScreen(mouseX, mouseY, partialTicks);
        }
        bus.post(new legacyconnector.v1_12_2.net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post(screen, mouseX, mouseY, partialTicks));
    }

    /** Posts ScreenshotEvent for an image about to be saved and returns it (cancelled, or with a changed file or message). */
    public static legacyconnector.v1_12_2.net.minecraftforge.client.event.ScreenshotEvent onScreenshot(java.awt.image.BufferedImage image, java.io.File file) {
        var event = new legacyconnector.v1_12_2.net.minecraftforge.client.event.ScreenshotEvent(image, file);
        legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    /** Posts PlaySoundEvent; returns the sound to play instead (null: nothing). */
    public static legacyconnector.v1_12_2.net.minecraft.client.audio.ISound playSound(legacyconnector.v1_12_2.net.minecraft.client.audio.SoundManager manager,
            legacyconnector.v1_12_2.net.minecraft.client.audio.ISound sound) {
        var event = new legacyconnector.v1_12_2.net.minecraftforge.client.event.sound.PlaySoundEvent(manager, sound);
        legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        return event.getResultSound();
    }

    /** Posts RenderHandEvent; true when a handler cancelled the hands. */
    public static boolean renderFirstPersonHand(legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderGlobal context, float partialTicks, int renderPass) {
        return legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new legacyconnector.v1_12_2.net.minecraftforge.client.event.RenderHandEvent(context, partialTicks, renderPass));
    }

    /** Forge drew its version lines on the title screen here; the game's title screen shows NeoForge's. Returns the splash text. */
    public static String renderMainMenu(legacyconnector.v1_12_2.net.minecraft.client.gui.GuiMainMenu gui, legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer font,
            int width, int height, String splashText) {
        return splashText;
    }
}
