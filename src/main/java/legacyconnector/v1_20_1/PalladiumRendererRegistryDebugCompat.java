package legacyconnector.v1_20_1;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in diagnostic snapshot for PalladiumCore's NeoForge entity-renderer registration queue.
 *
 * <p>Enable with {@code -Dlegacyconnector.debug.palladiumRendererRegistry=true}. This helper
 * reads registry suppliers and provider class names only; it never invokes a renderer provider
 * or changes the pending registry map.</p>
 */
public final class PalladiumRendererRegistryDebugCompat {
    private static final String DEBUG_PROPERTY = "legacyconnector.debug.palladiumRendererRegistry";
    private static final Logger LOGGER = LoggerFactory.getLogger("LegacyConnector/PalladiumRendererRegistry");

    private PalladiumRendererRegistryDebugCompat() {
    }

    public static void logRegistryMap(String stage, Map<?, ?> renderers) {
        if (!Boolean.getBoolean(DEBUG_PROPERTY)) {
            return;
        }
        if (renderers == null) {
            LOGGER.info("[PalladiumRendererRegistryTrace] event=RegisterRenderers stage={} pendingRenderers=<null>",
                    stage);
            return;
        }

        LOGGER.info("[PalladiumRendererRegistryTrace] event=RegisterRenderers stage={} pendingRenderers={}",
                stage, renderers.size());
        int index = 0;
        for (Map.Entry<?, ?> entry : renderers.entrySet()) {
            Object supplier = entry.getKey();
            Object provider = entry.getValue();
            LOGGER.info(
                    "[PalladiumRendererRegistryTrace] event=RegisterRenderers stage={} index={} entityType={} supplierClass={} providerClass={}",
                    stage,
                    index++,
                    resolveSupplierType(supplier),
                    className(supplier),
                    className(provider));
        }
    }

    static String resolveSupplierType(Object candidate) {
        if (!(candidate instanceof Supplier<?> supplier)) {
            return candidate == null ? "<null>" : "<not-supplier:" + className(candidate) + ">";
        }

        try {
            Object resolved = supplier.get();
            if (resolved instanceof EntityType<?> entityType) {
                ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                return key == null ? "<unregistered-entity-type>" : key.toString();
            }
            return resolved == null ? "<null>" : String.valueOf(resolved);
        } catch (RuntimeException | LinkageError error) {
            return "<supplier-error:" + error.getClass().getSimpleName() + ">";
        }
    }

    private static String className(Object value) {
        return value == null ? "<null>" : value.getClass().getName();
    }
}
