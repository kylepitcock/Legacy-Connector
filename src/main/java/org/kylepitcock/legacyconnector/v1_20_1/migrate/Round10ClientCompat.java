package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;

/**
 * Client-side helpers of gap round 10 (1.20.1 mods on 1.21.1). Reached from {@code mc-calls.tsv}, {@code mc-fields.tsv} and {@code mc-bridges.tsv}.
 */
public final class Round10ClientCompat {
    private Round10ClientCompat() {}

    // ---- particle render types ---------------------------------------------------------------------------------------------------------------

    /**
     * {@code ParticleRenderType.begin(BufferBuilder, TextureManager)} called by a mod: 1.21.1 starts the builder on the tesselator and answers it, the 1.20.1
     * builder object (see {@link CompatBufferBuilder}) takes that builder over, so the mod's {@code builder.end()} / {@code tesselator.end()} finish it.
     */
    public static void particleBegin(ParticleRenderType type, CompatBufferBuilder builder, TextureManager textures) {
        Tesselator tesselator = builder.tesselator() != null ? builder.tesselator() : Tesselator.getInstance();
        BufferBuilder started = type.begin(tesselator, textures);
        if (started != null) builder.adopt(started);
    }

    /**
     * {@code ParticleRenderType.end(Tesselator)} called by a mod: draws what {@link #particleBegin} (or the type's begin) started. A mod type's own
     * {@code end} body is not run (1.21.1 has no such hook; the particle engine draws the mesh itself, APPROXIMATION: a mod's extra state reset in end is skipped).
     */
    public static void particleEnd(ParticleRenderType type, Tesselator tesselator) {
        if (CompatBufferBuilder.startedOf(tesselator) != null) CompatBufferBuilder.end(tesselator);
    }

    // ---- camera -------------------------------------------------------------------------------------------------------------------------------

    private static MethodHandle maxZoom;

    /**
     * {@code Camera.getMaxZoom(double)}: the third-person camera distance shortened so the camera does not end up inside blocks. 1.21.1 keeps the same
     * method private with a float parameter; it is reached through a private lookup.
     */
    public static synchronized double cameraMaxZoom(Camera camera, double distance) {
        try {
            if (maxZoom == null) {
                java.lang.reflect.Method m = Camera.class.getDeclaredMethod("getMaxZoom", float.class);
                m.setAccessible(true);
                maxZoom = MethodHandles.lookup().unreflect(m).asType(MethodType.methodType(float.class, Camera.class, float.class));
            }
            return (float) maxZoom.invokeExact(camera, (float) distance);
        } catch (Throwable e) {
            return distance;
        }
    }

    // ---- vertex formats -----------------------------------------------------------------------------------------------------------------------

    /**
     * {@code DefaultVertexFormat.ELEMENT_PADDING} / {@code VertexFormatElement.Usage.PADDING}: 1.21.1 has no padding element (a format gets padding through
     * {@code VertexFormat.Builder.padding(int)}). The element is a marker no format contains; {@link MiddleClientCompat#newVertexFormat} turns it into
     * one byte of padding. APPROXIMATION: the marker uses the {@code GENERIC} usage, so code that compares a usage with {@code PADDING} sees GENERIC.
     */
    public static final VertexFormatElement PADDING_ELEMENT = new VertexFormatElement(31, 31, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.GENERIC, 1);

    public static VertexFormatElement elementPadding() {
        return PADDING_ELEMENT;
    }

    public static VertexFormatElement.Usage usagePadding() {
        return VertexFormatElement.Usage.GENERIC;
    }

    /** {@code new VertexFormatElement(index, type, usage, count)} of 1.20.1: the id is taken from the next free slot of the 1.21.1 registry (or an existing equal element). */
    public static VertexFormatElement newElement(int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
        // Usage.PADDING reads as GENERIC (see usagePadding), so a byte element of that usage is the padding element (APPROXIMATION: a genuine generic byte element is padding too)
        if (usage == VertexFormatElement.Usage.GENERIC && type == VertexFormatElement.Type.BYTE) return PADDING_ELEMENT;
        for (int id = 0; id < VertexFormatElement.MAX_COUNT; id++) {
            VertexFormatElement existing = VertexFormatElement.byId(id);
            if (existing != null && existing.index() == index && existing.type() == type && existing.usage() == usage && existing.count() == count) return existing;
        }
        int id = VertexFormatElement.findNextId();
        return VertexFormatElement.register(id, index, type, usage, count);
    }

    /** True for the {@link #PADDING_ELEMENT} marker. */
    public static boolean isPadding(VertexFormatElement element) {
        return element == PADDING_ELEMENT;
    }

    static void addTo(VertexFormat.Builder builder, String name, VertexFormatElement element) {
        if (isPadding(element)) builder.padding(1);
        else builder.add(name, element);
    }

    // ---- screens ------------------------------------------------------------------------------------------------------------------------------

    /** {@code new BookViewScreen.WrittenBookAccess(stack)}: the written-book view of an item stack is {@code BookAccess.fromItem} now. */
    public static net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess bookAccess(net.minecraft.world.item.ItemStack stack) {
        return net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess.fromItem(stack);
    }

    /** {@code JoinMultiplayerScreen.setToolTip(List<Component>)}: the tooltip is drawn in the next render pass (1.21.1's {@code setTooltipForNextRenderPass}). */
    public static void setToolTip(net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen screen, java.util.List<net.minecraft.network.chat.Component> lines) {
        if (lines == null) return;
        screen.setTooltipForNextRenderPass(lines.stream().map(net.minecraft.network.chat.Component::getVisualOrderText).toList());
    }
}
