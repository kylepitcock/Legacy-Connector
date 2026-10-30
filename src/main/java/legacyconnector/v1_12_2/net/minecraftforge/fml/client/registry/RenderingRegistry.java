package legacyconnector.v1_12_2.net.minecraftforge.fml.client.registry;

import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.Render;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ClientRegistrations1122;

/**
 * Forge 1.12's entity renderer registry. Registrations are kept in order ({@link ClientRegistrations1122}); the shim's
 * {@link RenderManager} makes the renderers from them on first use and the layer draws mod entities with them.
 */
public class RenderingRegistry {
    private RenderingRegistry() {}

    public static <T extends Entity> void registerEntityRenderingHandler(Class<T> entityClass, IRenderFactory<? super T> renderFactory) {
        if (entityClass != null && renderFactory != null) ClientRegistrations1122.mcc$registerEntityRenderer(entityClass, renderFactory);
    }

    /** The pre-factory overload: the renderer itself (made before the render manager existed). */
    @Deprecated
    public static void registerEntityRenderingHandler(Class<? extends Entity> entityClass, Render<? extends Entity> renderer) {
        if (entityClass != null && renderer != null) ClientRegistrations1122.mcc$registerEntityRenderer(entityClass, renderer);
    }

    /** Puts the registered renderers into a renderer map (Forge's hook into the render manager). */
    @Deprecated
    public static void loadEntityRenderers(Map<Class<? extends Entity>, Render<? extends Entity>> renderMap) {
        loadEntityRenderers(RenderManager.mcc$instance(), renderMap);
    }

    @SuppressWarnings("unchecked")
    public static void loadEntityRenderers(RenderManager manager, Map<Class<? extends Entity>, Render<? extends Entity>> renderMap) {
        if (renderMap == null) return;
        for (var e : ClientRegistrations1122.entityRenderers().entrySet()) {
            Object value = e.getValue();
            Render<? extends Entity> render = null;
            try {
                if (value instanceof Render<?> r) render = (Render<? extends Entity>) r;
                else if (value instanceof IRenderFactory<?> f) render = (Render<? extends Entity>) f.createRenderFor(manager);
            } catch (RuntimeException | LinkageError ex) {
                org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("creating the entity renderer of " + e.getKey().getName(), ex);
            }
            if (render != null) renderMap.put((Class<? extends Entity>) e.getKey(), render);
        }
    }
}
