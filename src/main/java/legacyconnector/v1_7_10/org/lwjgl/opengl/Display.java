package legacyconnector.v1_7_10.org.lwjgl.opengl;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

/**
 * LWJGL 2's window facade over the modern client's GLFW window. The window is created and owned by the game, so
 * lifecycle calls (create, destroy, update, sync) are accepted and do nothing; every query reads the real window.
 */
public class Display {
    private Display() {
    }

    private static Window window() {
        try {
            return Minecraft.getInstance().getWindow();
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    public static void create() {
    }

    /** The pixel format only matters to the window the host already created. */
    public static void create(PixelFormat format) {
    }

    public static void destroy() {
    }

    /**
     * LWJGL 2's {@code Display.setIcon}: the window icon from RGBA images (square, 4 bytes a pixel). Returns the number of icons
     * used. The game's own icon stays when there is no window or no usable image.
     */
    public static int setIcon(java.nio.ByteBuffer[] icons) {
        Window w = window();
        if (w == null || icons == null) return 0;
        // the pixels are copied (the mod may reuse its buffers) and GLFW is called on the render thread
        java.util.List<byte[]> pixelsOf = new java.util.ArrayList<>();
        java.util.List<Integer> sizes = new java.util.ArrayList<>();
        for (java.nio.ByteBuffer icon : icons) {
            if (icon == null) continue;
            int pixels = icon.remaining() / 4;
            int size = (int) Math.round(Math.sqrt(pixels));
            if (size <= 0 || size * size != pixels) continue;
            byte[] copy = new byte[pixels * 4];
            icon.duplicate().get(copy);
            pixelsOf.add(copy);
            sizes.add(size);
        }
        if (pixelsOf.isEmpty()) return 0;
        onMainThread(() -> {
            try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
                org.lwjgl.glfw.GLFWImage.Buffer images = org.lwjgl.glfw.GLFWImage.malloc(pixelsOf.size(), stack);
                for (int i = 0; i < pixelsOf.size(); i++) {
                    java.nio.ByteBuffer pixels = stack.malloc(pixelsOf.get(i).length);
                    pixels.put(pixelsOf.get(i)).flip();
                    images.position(i);
                    images.width(sizes.get(i)).height(sizes.get(i)).pixels(pixels);
                }
                images.position(0);
                GLFW.glfwSetWindowIcon(w.getWindow(), images);
            } catch (RuntimeException | LinkageError e) {
                // the game's own icon stays
            }
        });
        return pixelsOf.size();
    }

    /**
     * GLFW window calls must run on the thread that owns the window (macOS aborts the whole process with "NSWindow geometry should only be
     * modified on the main thread" otherwise): legacy mods call {@code Display.setTitle} and friends from the mod loading threads, so they are
     * queued onto the game's render thread unless the caller already is that thread.
     */
    private static void onMainThread(Runnable action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.isSameThread()) action.run();
        else mc.execute(action);
    }

    public static void update() {
    }

    public static void update(boolean processMessages) {
    }

    public static void processMessages() {
    }

    public static void sync(int fps) {
    }

    public static void swapBuffers() {
    }

    public static boolean isCreated() {
        return window() != null;
    }

    public static boolean isActive() {
        Window w = window();
        return w != null && GLFW.glfwGetWindowAttrib(w.getWindow(), GLFW.GLFW_FOCUSED) != 0;
    }

    public static boolean isVisible() {
        Window w = window();
        return w != null && GLFW.glfwGetWindowAttrib(w.getWindow(), GLFW.GLFW_ICONIFIED) == 0;
    }

    public static boolean isDirty() {
        return false;
    }

    public static boolean isCurrent() {
        return com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread();
    }

    public static void makeCurrent() {
    }

    public static void releaseContext() {
    }

    public static boolean isCloseRequested() {
        Window w = window();
        return w != null && GLFW.glfwWindowShouldClose(w.getWindow());
    }

    public static int getWidth() {
        Window w = window();
        return w == null ? 854 : w.getScreenWidth();
    }

    public static int getHeight() {
        Window w = window();
        return w == null ? 480 : w.getScreenHeight();
    }

    public static int getX() {
        Window w = window();
        if (w == null) return 0;
        int[] x = new int[1];
        GLFW.glfwGetWindowPos(w.getWindow(), x, new int[1]);
        return x[0];
    }

    public static int getY() {
        Window w = window();
        if (w == null) return 0;
        int[] y = new int[1];
        GLFW.glfwGetWindowPos(w.getWindow(), new int[1], y);
        return y[0];
    }

    public static float getPixelScaleFactor() {
        Window w = window();
        return w == null || w.getScreenWidth() == 0 ? 1.0F : (float) w.getWidth() / w.getScreenWidth();
    }

    public static boolean isFullscreen() {
        Window w = window();
        return w != null && w.isFullscreen();
    }

    public static void setFullscreen(boolean fullscreen) {
        Window w = window();
        if (w != null) onMainThread(() -> {
            if (w.isFullscreen() != fullscreen) w.toggleFullScreen();
        });
    }

    public static String getTitle() {
        return "Minecraft 1.7.10";
    }

    public static void setTitle(String title) {
        Window w = window();
        if (w != null && title != null) onMainThread(() -> GLFW.glfwSetWindowTitle(w.getWindow(), title));
    }

    public static void setVSyncEnabled(boolean enabled) {
        Window w = window();
        if (w != null) w.updateVsync(enabled);
    }

    public static boolean isResizable() {
        return true;
    }

    public static void setResizable(boolean resizable) {
    }

    public static boolean wasResized() {
        return false;
    }

    public static void setLocation(int x, int y) {
        Window w = window();
        if (w != null) onMainThread(() -> GLFW.glfwSetWindowPos(w.getWindow(), x, y));
    }

    public static DisplayMode getDisplayMode() {
        return new DisplayMode(getWidth(), getHeight(), 32, 60);
    }

    public static void setDisplayMode(DisplayMode mode) {
    }

    public static DisplayMode getDesktopDisplayMode() {
        try {
            GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
            if (mode != null) return new DisplayMode(mode.width(), mode.height(), mode.redBits() + mode.greenBits() + mode.blueBits(), mode.refreshRate());
        } catch (RuntimeException | LinkageError e) {
            // fall through to the window size
        }
        return getDisplayMode();
    }

    public static DisplayMode[] getAvailableDisplayModes() {
        return new DisplayMode[] {getDesktopDisplayMode()};
    }

    public static String getAdapter() {
        return "Generic";
    }

    public static String getVersion() {
        return "1.21.1";
    }
}
