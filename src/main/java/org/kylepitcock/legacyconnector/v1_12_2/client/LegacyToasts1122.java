package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.client.gui.toasts.GuiToast;
import legacyconnector.v1_12_2.net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** 1.12.2 toasts as modern toasts: each frame runs the legacy draw (160x32 at the toast's origin) and keeps its visibility. */
public final class LegacyToasts1122 {
    private LegacyToasts1122() {}

    public static void add(GuiToast gui, IToast legacy) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        mc.execute(() -> mc.getToasts().addToast(new Adapter(gui, legacy)));
    }

    @SuppressWarnings("unchecked")
    public static <T extends IToast> T find(Class<? extends T> cls, Object type) {
        Adapter a = net.minecraft.client.Minecraft.getInstance().getToasts().getToast(Adapter.class, type);
        return a != null && cls.isInstance(a.legacy) ? (T) a.legacy : null;
    }

    private static final class Adapter implements Toast {
        private final GuiToast gui;
        private final IToast legacy;

        Adapter(GuiToast gui, IToast legacy) {
            this.gui = gui;
            this.legacy = legacy;
        }

        @Override
        public Visibility render(GuiGraphics graphics, ToastComponent toastComponent, long timeSinceLastVisible) {
            RenderContext.beginGui(graphics);
            try {
                IToast.Visibility v = legacy.draw(gui, timeSinceLastVisible);
                return v == null ? Visibility.HIDE : v.mcc$modern();
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Gui").warn("A legacy toast {} failed: {}", legacy.getClass().getName(), e.toString());
                return Visibility.HIDE;
            } finally {
                RenderContext.end();
            }
        }

        @Override
        public Object getToken() {
            Object t = legacy.getType();
            return t == null ? IToast.NO_TOKEN : t;
        }
    }
}
