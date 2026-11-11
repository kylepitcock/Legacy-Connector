package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.kylepitcock.legacyconnector.vb1_7_3.AssetsB173;
import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;

/**
 * Draws b1.7.3 GUIs into the modern screen being rendered ({@link #begin}/{@link #end} around the legacy drawing). Texture paths become
 * numbered textures as {@code RenderEngine.getTexture} made them: the image from the mods' jars, else the b1.7.3 layout of a vanilla GUI
 * sheet built from the modern game's textures (furnace with its flame and arrow, chest, crafting table, dispenser, button sheet). Images are
 * uploaded on first use on the render thread, so a GUI built on the server thread (single player) can ask for ids too.
 */
public final class ClientGuiB173 implements GuiDrawB173.Sink {
    private static final AtomicInteger NEXT = new AtomicInteger(1);
    private static final Map<String, Integer> IDS = new ConcurrentHashMap<>();
    private static final Map<Integer, Texture> TEXTURES = new ConcurrentHashMap<>();

    private static GuiGraphics graphics;
    private static Texture bound;
    private static float red = 1.0F, green = 1.0F, blue = 1.0F, alpha = 1.0F;
    public record Background(ResourceLocation texture,int u,int v,int width,int height) {}
    private record BackgroundBounds(int x,int y,int width,int height) {}
    private static BackgroundBounds backgroundBounds;
    private static Background background;
    /** Immediate-mode state of the legacy GUI being drawn: GL_TEXTURE_2D, the matrix mode, the orthographic mapping and the base pose. */
    private static boolean texture2d = true, overlay;
    private static java.util.Set<String> overlayStandard = java.util.Set.of();
    private static int matrixMode = 5888;
    private static double orthoSx = 1, orthoSy = 1, orthoTx, orthoTy;
    private static org.joml.Matrix4f basePose;

    private static final class Texture {
        final int id;
        final String path;
        final BufferedImage image;
        ResourceLocation location;

        Texture(int id, String path, BufferedImage image) {
            this.id = id;
            this.path = path;
            this.image = image;
        }
    }

    /** Legacy drawing goes into {@code g} until {@link #end}. */
    public static void begin(GuiGraphics g) {
        graphics = g;
        red = green = blue = alpha = 1.0F;
        backgroundBounds=null;background=null;
        texture2d = true;matrixMode = 5888;orthoSx = orthoSy = 1;orthoTx = orthoTy = 0;overlay = false;overlayStandard = java.util.Set.of();
        basePose = new org.joml.Matrix4f(g.pose().last().pose());
    }

    /**
     * Draws the additions of a legacy GUI over a finished native screen (a mod's loading-screen text over the native loading screen): the
     * legacy drawing's own backdrop and bars (quads) are skipped, and so are the standard {@code standardText} strings the native screen
     * already shows. The legacy GL matrix's depth translation is ignored, so text lands at the screen's own depth.
     */
    public static void beginOverlay(GuiGraphics g, java.util.Set<String> standardText) {
        begin(g);
        overlay = true;
        overlayStandard = standardText;
    }

    /** Observe the original full background blit so attached native controls share its artwork. */
    public static void begin(GuiGraphics g,int x,int y,int width,int height) {
        begin(g);backgroundBounds=new BackgroundBounds(x,y,width,height);
    }
    public static Background background(){return background;}

    public static void end() {
        if (graphics != null) graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics = null;
        backgroundBounds=null;
    }

    @Override
    public void blit(int x, int y, int u, int v, int width, int height) {
        GuiGraphics g = graphics;
        Texture t = bound;
        if (g == null || t == null) return;
        ResourceLocation rl = upload(t);
        if (rl != null) {
            var bounds=backgroundBounds;
            if(bounds!=null&&background==null&&x==bounds.x()&&y==bounds.y()&&width==bounds.width()&&height==bounds.height())
                background=new Background(rl,u,v,width,height);
            g.blit(rl, x, y, u, v, width, height);
        }
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        GuiGraphics g = graphics;
        if (g != null) g.fill(x1, y1, x2, y2, color);
    }

    @Override
    public void fillGradient(int x1, int y1, int x2, int y2, int top, int bottom) {
        GuiGraphics g = graphics;
        if (g != null) g.fillGradient(x1, y1, x2, y2, top, bottom);
    }

    /** b1.7.3 text took its own colour, not the GL colour textures were tinted with. */
    @Override
    public void text(String text, int x, int y, int color, boolean shadow) {
        GuiGraphics g = graphics;
        if (g == null || overlay && overlayStandard.contains(text)) return;
        boolean tinted = red != 1.0F || green != 1.0F || blue != 1.0F || alpha != 1.0F;
        if (tinted) g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        // An orthographic mapping a legacy GUI set (its own scaled coordinates) applies to text positions as it does to quads.
        g.drawString(Minecraft.getInstance().font, text, (int) Math.round(x * orthoSx + orthoTx), (int) Math.round(y * orthoSy + orthoTy), color, shadow);
        if (tinted) {
            g.flush();
            g.setColor(red, green, blue, alpha);
        }
    }

    @Override
    public int width(String text) {
        // In the legacy GUI's own scaled units when it set an orthographic mapping, so its centring arithmetic still centres.
        return (int) Math.round(Minecraft.getInstance().font.width(text) / orthoSx);
    }

    @Override
    public int textureId(String path) {
        return IDS.computeIfAbsent(path, p -> {
            int id = NEXT.getAndIncrement();
            TEXTURES.put(id, new Texture(id, p, null));
            return id;
        });
    }

    @Override
    public int load(BufferedImage image) {
        int id = NEXT.getAndIncrement();
        TEXTURES.put(id, new Texture(id, null, image));
        return id;
    }

    @Override
    public void bind(int id) {
        bound = TEXTURES.get(id);
    }

    @Override
    public void color(float r, float g, float b, float a) {
        red = r;
        green = g;
        blue = b;
        alpha = a;
        GuiGraphics gg = graphics;
        if (gg != null) gg.setColor(r, g, b, a);
    }

    @Override
    public void push() {
        GuiGraphics g = graphics;
        if (g != null) g.pose().pushPose();
    }

    @Override
    public void pop() {
        GuiGraphics g = graphics;
        if (g != null) g.pose().popPose();
    }

    @Override
    public void translate(float x, float y, float z) {
        GuiGraphics g = graphics;
        if (g != null) g.pose().translate(x, y, overlay ? 0 : z);
    }

    @Override public boolean drawing() {return graphics != null;}
    @Override public void texture2d(boolean enabled) {texture2d = enabled;}
    @Override public void matrixMode(int mode) {matrixMode = mode;}

    @Override public void loadIdentity() {
        GuiGraphics g = graphics;
        if (g == null) return;
        if (matrixMode == 5889) {orthoSx = orthoSy = 1;orthoTx = orthoTy = 0;}
        else if (matrixMode == 5888 && basePose != null) g.pose().last().pose().set(basePose);
    }

    /** The legacy coordinates (left to right, top to bottom) span the whole screen. */
    @Override public void ortho(double left, double right, double bottom, double top) {
        GuiGraphics g = graphics;
        if (g == null || matrixMode != 5889) return;
        double width = right - left, height = bottom - top;
        if (width == 0 || height == 0) return;
        orthoSx = g.guiWidth() / width;orthoSy = g.guiHeight() / height;
        orthoTx = -left * orthoSx;orthoTy = -top * orthoSy;
    }

    /** One Tessellator quad: transformed by the current pose and orthographic mapping, textured with the bound image unless GL_TEXTURE_2D is off. */
    @Override public void quad(double[] x, double[] y, double[] u, double[] v, int[] argb) {
        GuiGraphics g = graphics;
        if (g == null || x.length != 4 || overlay) return;
        var pose = g.pose().last().pose();
        float[] px = new float[4], py = new float[4];
        var point = new org.joml.Vector3f();
        for (int i = 0; i < 4; i++) {
            pose.transformPosition((float) x[i], (float) y[i], 0, point);
            px[i] = (float) (point.x * orthoSx + orthoTx);py[i] = (float) (point.y * orthoSy + orthoTy);
        }
        ResourceLocation rl = texture2d && bound != null ? upload(bound) : null;
        g.flush();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.disableCull();
        com.mojang.blaze3d.vertex.BufferBuilder buffer;
        if (rl != null) {
            com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader);
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, rl);
            buffer = com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR);
        } else {
            com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
            buffer = com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
        }
        for (int i = 0; i < 4; i++) {
            int c = argb[i];
            int a = Math.round(((c >>> 24) & 255) * alpha), r = Math.round(((c >> 16) & 255) * red);
            int gr = Math.round(((c >> 8) & 255) * green), b = Math.round((c & 255) * blue);
            int tinted = (a << 24) | (r << 16) | (gr << 8) | b;
            var vertex = buffer.addVertex(px[i], py[i], 0);
            if (rl != null) vertex.setUv((float) u[i], (float) v[i]);
            vertex.setColor(tinted);
        }
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    @Override
    public void scale(float x, float y, float z) {
        GuiGraphics g = graphics;
        if (g != null) g.pose().scale(x, y, z);
    }

    @Override public void rotate(float angle,float x,float y,float z) {
        if(graphics!=null&&x*x+y*y+z*z>0)graphics.pose().mulPose(new org.joml.Quaternionf().rotationAxis(
                (float)Math.toRadians(angle),new org.joml.Vector3f(x,y,z).normalize()));
    }
    @Override public void lighting(boolean enabled) {
        if(graphics==null)return;
        graphics.flush();
        if(enabled)com.mojang.blaze3d.platform.Lighting.setupForEntityInInventory();
        else com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
    }

    /** Render the client's entity through the native dispatcher under the original GUI's matrix and temporary pose. */
    @Override public void entity(Object raw,double x,double y,double z,float yaw,float partial) {
        GuiGraphics g=graphics;
        if(g==null||!(raw instanceof legacyconnector.vb1_7_3.net.minecraft.entity.Entity original))return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null)return;
        net.minecraft.world.entity.Entity entity=original.mcc$modern;
        if(entity==null)return;
        // Integrated-server GUI handoff objects retain a server actor; rendering never touches that actor's native state.
        if(!entity.level().isClientSide)entity=mc.player!=null&&entity.getUUID().equals(mc.player.getUUID())?mc.player:mc.level.getEntity(entity.getId());
        if(entity==null||entity.level()!=mc.level)return;
        var dispatcher=mc.getEntityRenderDispatcher();
        var camera=new org.joml.Quaternionf(dispatcher.cameraOrientation());
        float oldYaw=entity.getYRot(),oldPitch=entity.getXRot(),oldYawO=entity.yRotO,oldPitchO=entity.xRotO;
        var living=entity instanceof net.minecraft.world.entity.LivingEntity value?value:null;
        float body=living==null?0:living.yBodyRot,bodyO=living==null?0:living.yBodyRotO;
        float head=living==null?0:living.yHeadRot,headO=living==null?0:living.yHeadRotO;
        boolean shadow;
        try {shadow=ShadowAccess.FIELD.getBoolean(dispatcher);}
        catch(IllegalAccessException error){throw new IllegalStateException("Cannot preserve native GUI entity shadow state",error);}
        g.flush();g.pose().pushPose();
        try {
            entity.setYRot(original.yaw);entity.yRotO=original.yaw;
            entity.setXRot(original.pitch);entity.xRotO=original.pitch;
            if(living!=null){
                living.yHeadRot=living.yHeadRotO=original.yaw;
                if(original instanceof legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity legacy)living.yBodyRot=living.yBodyRotO=legacy.bodyYaw;
            }
            // Preserve the caller's model orientation: its mirrored scale and the native renderer's
            // body-yaw rotation already produce the legacy preview's facing direction.
            com.mojang.blaze3d.platform.Lighting.setupForEntityInInventory();
            dispatcher.overrideCameraOrientation(new org.joml.Quaternionf());
            dispatcher.setRenderShadow(false);
            net.minecraft.world.entity.Entity target=entity;
            // Beta RenderPlayer subtracts yOffset; native players already use a feet-relative render origin.
            double renderY=original instanceof legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity?y-original.standingEyeHeight:y;
            com.mojang.blaze3d.systems.RenderSystem.runAsFancy(()->dispatcher.render(target,x,renderY,z,yaw,partial,g.pose(),g.bufferSource(),15728880));
            g.flush();
        } finally {
            dispatcher.setRenderShadow(shadow);dispatcher.overrideCameraOrientation(camera);
            entity.setYRot(oldYaw);entity.yRotO=oldYawO;entity.setXRot(oldPitch);entity.xRotO=oldPitchO;
            if(living!=null){living.yBodyRot=body;living.yBodyRotO=bodyO;living.yHeadRot=head;living.yHeadRotO=headO;}
            g.pose().popPose();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        }
    }
    private static final class ShadowAccess {
        static final java.lang.reflect.Field FIELD=net.neoforged.fml.util.ObfuscationReflectionHelper.findField(
                net.minecraft.client.renderer.entity.EntityRenderDispatcher.class,"shouldRenderShadow");
    }

    @Override
    public void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    // ------------------------------------------------------------------ textures

    private static ResourceLocation upload(Texture t) {
        if (t.location != null) return t.location;
        BufferedImage img = t.image != null ? t.image : read(t.path);
        if (img == null) img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        NativeImage ni = new NativeImage(img.getWidth(), img.getHeight(), true);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                ni.setPixelRGBA(x, y, (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16));
            }
        }
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("legacyconnector", "b173_gui/" + t.id);
        Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(ni));
        t.location = rl;
        return rl;
    }

    /** A GUI image: from the mods' jars, else a vanilla sheet in its b1.7.3 layout. */
    static BufferedImage read(String path) {
        if (path == null) return null;
        String p = path.startsWith("/") ? path : "/" + path;
        BufferedImage img = AssetsB173.modImage(p);
        return img != null ? img : vanillaSheet(p);
    }

    private static BufferedImage vanillaSheet(String path) {
        return switch (path) {
            case "/gui/furnace.png" -> {
                BufferedImage sheet = sheet(vanilla("textures/gui/container/furnace.png"));
                paste(sheet, vanilla("textures/gui/sprites/container/furnace/lit_progress.png"), 176, 0);
                paste(sheet, vanilla("textures/gui/sprites/container/furnace/burn_progress.png"), 176, 14);
                yield sheet;
            }
            case "/gui/gui.png" -> {
                BufferedImage sheet = sheet(null);
                paste(sheet, vanilla("textures/gui/sprites/widget/button_disabled.png"), 0, 46);
                paste(sheet, vanilla("textures/gui/sprites/widget/button.png"), 0, 66);
                paste(sheet, vanilla("textures/gui/sprites/widget/button_highlighted.png"), 0, 86);
                yield sheet;
            }
            case "/gui/container.png" -> vanilla("textures/gui/container/generic_54.png");
            case "/gui/crafting.png" -> vanilla("textures/gui/container/crafting_table.png");
            case "/gui/trap.png" -> vanilla("textures/gui/container/dispenser.png");
            case "/gui/inventory.png" -> vanilla("textures/gui/container/inventory.png");
            case "/font/default.png" -> vanilla("textures/font/ascii.png");
            case "/gui/background.png" -> vanilla("textures/block/dirt.png");
            default -> null;
        };
    }

    private static BufferedImage vanilla(String location) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(ResourceLocation.withDefaultNamespace(location))) {
            return ImageIO.read(in);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** A 256x256 sheet holding {@code base} at its top left corner. */
    private static BufferedImage sheet(BufferedImage base) {
        BufferedImage sheet = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        paste(sheet, base, 0, 0);
        return sheet;
    }

    private static void paste(BufferedImage sheet, BufferedImage part, int x, int y) {
        if (part == null) return;
        var g = sheet.createGraphics();
        try {
            g.drawImage(part, x, y, null);
        } finally {
            g.dispose();
        }
    }
}
