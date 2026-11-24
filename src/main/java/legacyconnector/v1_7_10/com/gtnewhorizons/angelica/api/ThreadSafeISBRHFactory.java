package legacyconnector.v1_7_10.com.gtnewhorizons.angelica.api;

import legacyconnector.v1_7_10.cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

/** Angelica's per-thread handler factory (see {@link ThreadSafeISBRH}): a handler that creates its own copies. */
public interface ThreadSafeISBRHFactory extends ISimpleBlockRenderingHandler {
    ThreadSafeISBRHFactory newInstance();
}
