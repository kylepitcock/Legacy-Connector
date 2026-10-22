package org.kylepitcock.mccompat.v1_7_10.bridge;

import mccompat.v1_7_10.net.minecraft.item.crafting.FurnaceRecipes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some libraries patch vanilla constructors with a coremod (the patch cannot run on the shim classes) and expect to
 * be told when the vanilla object exists. This delivers the same call once the library's classes are loadable.
 */
public final class CoremodHooks {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/Hooks");
    private static boolean smeltingDone;

    private CoremodHooks() {}

    /** DragonAPI hooks {@code FurnaceRecipes.<init>} with {@code ReplacementSmeltingHandler.onSmeltingInit(this)}. */
    public static synchronized void afterConstruct(ClassLoader loader) {
        if (smeltingDone) return;
        try {
            Class<?> handler = Class.forName("Reika.DragonAPI.Extras.ReplacementSmeltingHandler", true, loader);
            smeltingDone = true;
            handler.getMethod("onSmeltingInit", FurnaceRecipes.class).invoke(null, FurnaceRecipes.smelting());
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // library not installed
        } catch (ReflectiveOperationException | RuntimeException e) {
            smeltingDone = true;
            LOG.warn("DragonAPI's smelting hook failed", e);
        }
    }
}
