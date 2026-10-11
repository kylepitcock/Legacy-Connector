package mccompat.v1_7_10.cpw.mods.fml.client.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mccompat.v1_7_10.net.minecraft.client.renderer.entity.Render;
import mccompat.v1_7_10.net.minecraft.client.renderer.entity.RenderManager;

/** Legacy renderer registration: entity renderers (drawn by the entity bridge) and block render handlers. */
public class RenderingRegistry {
    private static final RenderingRegistry INSTANCE = new RenderingRegistry();
    private static final Map<Integer, ISimpleBlockRenderingHandler> BLOCK_HANDLERS = new ConcurrentHashMap<>();
    private static int nextRenderId = 42;

    public static RenderingRegistry instance() {
        return INSTANCE;
    }

    public static void registerEntityRenderingHandler(Class<?> entityClass, Render renderer) {
        if (entityClass == null || renderer == null) return;
        renderer.setRenderManager(RenderManager.instance);
        RenderManager.instance.entityRenderMap.put(entityClass, renderer);
    }

    public static synchronized int getNextAvailableRenderId() {
        return nextRenderId++;
    }

    public static void registerBlockHandler(ISimpleBlockRenderingHandler handler) {
        if (handler != null) BLOCK_HANDLERS.put(handler.getRenderId(), handler);
    }

    public static void registerBlockHandler(int renderId, ISimpleBlockRenderingHandler handler) {
        if (handler != null) BLOCK_HANDLERS.put(renderId, handler);
    }

    public static ISimpleBlockRenderingHandler mcc$blockHandler(int renderId) {
        return BLOCK_HANDLERS.get(renderId);
    }

    public static int addNewArmourRendererPrefix(String prefix) {
        return 5 + prefix.hashCode() % 1000;
    }
}
