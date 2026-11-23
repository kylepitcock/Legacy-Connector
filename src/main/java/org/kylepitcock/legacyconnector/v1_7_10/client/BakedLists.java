package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.kylepitcock.legacyconnector.core.degraded.Degraded;
import org.kylepitcock.legacyconnector.core.flags.Flags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Display lists as GPU geometry. A legacy mod compiles a list once and calls it every frame; replaying its recorded draws means
 * writing every vertex into the frame's buffers again, per call (a wireframe of a few hundred thousand lines, a model of hundreds of
 * boxes). The second call of a list in the world instead runs its actions once against a recorder: geometry lands, in the list's own
 * space, in one {@link VertexBuffer} per render type; the call after that sets the caller's matrix and draws the buffers.
 *
 * <p>What a list may do to be baked: draw geometry and change the matrix, colour and state as it likes inside, as long as it leaves
 * the caller's state, matrix and attribute stack as it found them and makes no real GL change (state at its end equals the state at
 * its call: its calls then have no effect beyond their geometry). The geometry depends on the emulated state at the call (texture,
 * blending, colour, light, polygon mode ...: {@link RenderContext.BakeState}); a call under another state bakes again, and a list
 * whose calls rarely agree with a bake goes back to replay for good. Everything else replays as before: calls in screens, captures,
 * the shadow pass, under a legacy projection or texture matrix, nested calls, lists with translucent (sorted) render types, and lists
 * over the memory cap.</p>
 *
 * <p>Lit render types take their normals in the space the caller's pose leaves them (the CPU applied the normal matrix), the shaders'
 * light directions are fixed: the directions are moved into the list's space for the draw, which gives the same lighting.</p>
 */
final class BakedLists {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/DisplayLists");
    /** Bakes of one list before it is given up when its calls keep arriving under another state. */
    private static final int MAX_BAKES = 6;
    private static final int MAX_LISTS = 20000;
    private static final long CAP_BYTES = Math.max(1, Flags.DISPLAY_LIST_BAKE_MB.getInt(256)) * 1024L * 1024L;

    private static final Set<DisplayLists.Entry> LIVE = ConcurrentHashMap.newKeySet();
    private static final AtomicLong BYTES = new AtomicLong();
    private static final AtomicLong BAKED = new AtomicLong(), REFUSED = new AtomicLong(), DRAWS = new AtomicLong();
    private static volatile boolean capNoted;
    private static final Field LIGHTS = lightsField();

    private BakedLists() {}

    /** One render type's geometry of a list. */
    private record Seg(RenderType type, VertexBuffer buffer, boolean lit) {}

    /** The baked meshes of a list, valid for the state they were made under. */
    static final class Baked {
        final RenderContext.BakeState key;
        final Seg[] segs;
        final long bytes;

        Baked(RenderContext.BakeState key, Seg[] segs, long bytes) {
            this.key = key;
            this.segs = segs;
            this.bytes = bytes;
        }
    }

    private static Field lightsField() {
        try {
            Field f = RenderSystem.class.getDeclaredField("shaderLightDirections");
            f.setAccessible(true);
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    /** What a recorded render type's buffer is, until it is built. */
    private static final class Part {
        final RenderType type;
        final ByteBufferBuilder bytes;
        final BufferBuilder builder;

        Part(RenderType type) {
            this.type = type;
            this.bytes = new ByteBufferBuilder(Math.max(1536, type.bufferSize()));
            this.builder = new BufferBuilder(bytes, type.mode(), type.format());
        }
    }

    /** Takes the geometry of a list the way a buffer source does, a buffer per render type, and keeps it. */
    private static final class Recorder implements MultiBufferSource {
        final Map<RenderType, Part> parts = new LinkedHashMap<>();
        /** A render type whose draws must be sorted by distance on every upload, which a fixed mesh cannot do. */
        RenderType sorted;

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            Part p = parts.get(type);
            if (p == null) {
                if (type.sortOnUpload() && sorted == null) sorted = type;
                parts.put(type, p = new Part(type));
            }
            return p.builder;
        }

        void close() {
            for (Part p : parts.values()) p.bytes.close();
            parts.clear();
        }
    }

    /**
     * Draws the list from its baked meshes, baking it first when it has none for the state of this call.
     *
     * @return false when the caller must replay the list's actions (nothing has been drawn then, and the emulated state is as it was)
     */
    static boolean tryCall(DisplayLists.Entry e) {
        RenderContext.BakeState now = RenderContext.bakeState();
        if (now == null) return false;
        int calls = ++e.calls;
        Baked b = (Baked) e.baked;
        if (b == null || !b.key.equals(now)) {
            if (calls < 2) return false; // a list called once is not worth the second run
            if (e.bakes >= MAX_BAKES && (long) e.bakes * MAX_BAKES > calls) {
                giveUp(e, "its calls keep arriving under other states");
                return false;
            }
            if (LIVE.size() >= MAX_LISTS && b == null) {
                capNote("more than " + MAX_LISTS + " baked lists");
                e.off = true;
                return false;
            }
            e.bakes++;
            b = bake(e, now);
            if (b == null) return false;
        }
        return draw(b);
    }

    private static void giveUp(DisplayLists.Entry e, String why) {
        e.off = true;
        REFUSED.incrementAndGet();
        release(e);
    }

    private static Baked bake(DisplayLists.Entry e, RenderContext.BakeState now) {
        Recorder rec = new Recorder();
        RenderContext.BakeRun run = RenderContext.beginBake(rec);
        String why = null;
        try {
            DisplayLists.runActions(e);
            if (rec.sorted != null) why = "translucent render type " + rec.sorted;
            else if (!run.clean()) why = "changes real GL state or leaves attributes pushed";
            else if (!RenderContext.bakeStateIs(now)) why = "leaves the emulated state changed";
            else if (!identity(run.pose)) why = "leaves its matrix changed";
        } catch (Throwable t) {
            why = "threw " + t;
        } finally {
            RenderContext.endBake(run, now);
        }
        if (why != null) {
            rec.close();
            e.off = true;
            REFUSED.incrementAndGet();
            if (REFUSED.get() <= 20) LOG.debug("display list not baked: {}", why);
            return null;
        }
        List<Seg> segs = new ArrayList<>();
        long bytes = 0;
        try {
            for (Part p : rec.parts.values()) {
                MeshData mesh = p.builder.build();
                if (mesh == null) continue;
                long size = (long) mesh.drawState().vertexCount() * p.type.format().getVertexSize();
                VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
                vb.bind();
                vb.upload(mesh);
                VertexBuffer.unbind();
                boolean lit = p.type.format().contains(VertexFormatElement.NORMAL) && p.type.format().contains(VertexFormatElement.UV2);
                segs.add(new Seg(p.type, vb, lit));
                bytes += size;
            }
        } catch (Throwable t) {
            for (Seg s : segs) s.buffer.close();
            LOG.debug("display list not baked: upload failed", t);
            e.off = true;
            return null;
        } finally {
            rec.close();
        }
        if (BYTES.get() + bytes > CAP_BYTES) {
            for (Seg s : segs) s.buffer.close();
            capNote("more than " + (CAP_BYTES >> 20) + " MB of baked geometry");
            e.off = true;
            return null;
        }
        Baked nb = new Baked(now, segs.toArray(new Seg[0]), bytes);
        Baked old = (Baked) e.baked;
        e.baked = nb;
        BYTES.addAndGet(bytes);
        LIVE.add(e);
        BAKED.incrementAndGet();
        if (old != null) free(old);
        return nb;
    }

    private static boolean identity(PoseStack pose) {
        if (!pose.clear()) return false;
        PoseStack.Pose p = pose.last();
        return p.pose().equals(new Matrix4f(), 1.0E-5F) && p.normal().equals(new Matrix3f(), 1.0E-5F);
    }

    private static void capNote(String what) {
        if (capNoted) return;
        capNoted = true;
        Degraded.note(Degraded.Category.OTHER, "display lists", "bake cap", "Baked display lists reached the cap (" + what + "); further lists are replayed every call");
    }

    private static boolean draw(Baked b) {
        PoseStack.Pose caller = RenderContext.callerPose();
        if (caller == null) return false;
        if (b.segs.length == 0) return true;
        boolean anyLit = false;
        for (Seg s : b.segs) anyLit |= s.lit;
        Vector3f[] lights = null;
        if (anyLit) {
            lights = lights();
            if (lights == null) return false; // the shaders' light directions cannot be read: replay
        }
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(caller.pose());
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        Vector3f[] saved = anyLit ? new Vector3f[] {new Vector3f(lights[0]), new Vector3f(lights[1])} : null;
        try {
            for (Seg s : b.segs) {
                s.type.setupRenderState();
                try {
                    ShaderInstance shader = RenderSystem.getShader();
                    if (shader == null) continue;
                    if (s.lit) {
                        // the buffer's normals are in list space; vanilla's are rotated by the pose's normal matrix, against light directions fixed in the world
                        RenderSystem.setShaderLights(caller.normal().transformTranspose(new Vector3f(saved[0])),
                                caller.normal().transformTranspose(new Vector3f(saved[1])));
                    }
                    s.buffer.bind();
                    s.buffer.drawWithShader(modelView, projection, shader);
                    VertexBuffer.unbind();
                } finally {
                    s.type.clearRenderState();
                }
            }
        } finally {
            if (anyLit) RenderSystem.setShaderLights(saved[0], saved[1]);
        }
        DRAWS.incrementAndGet();
        return true;
    }

    private static Vector3f[] lights() {
        if (LIGHTS == null) return null;
        try {
            Vector3f[] l = (Vector3f[]) LIGHTS.get(null);
            return l != null && l.length >= 2 && l[0] != null && l[1] != null ? l : null;
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------ freeing

    static void release(DisplayLists.Entry e) {
        Object old = e.baked;
        e.baked = null;
        LIVE.remove(e);
        if (old instanceof Baked b) free(b);
    }

    private static void free(Baked b) {
        BYTES.addAndGet(-b.bytes);
        Runnable close = () -> {
            for (Seg s : b.segs) s.buffer.close();
        };
        if (RenderSystem.isOnRenderThread()) close.run();
        else RenderSystem.recordRenderCall(close::run);
    }

    /** Resource reload: the baked texture coordinates are stale. */
    static void clearAll() {
        for (DisplayLists.Entry e : LIVE) {
            release(e);
            e.off = false;
            e.bakes = 0;
        }
    }

    /** For the log: lists baked, bytes held, draws from buffers. */
    static String stats() {
        return LIVE.size() + " lists, " + (BYTES.get() >> 10) + " KB, " + BAKED.get() + " bakes, " + DRAWS.get() + " draws, " + REFUSED.get() + " not bakeable";
    }
}
