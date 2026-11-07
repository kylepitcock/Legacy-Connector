package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.src.BaseMod;
import net.neoforged.fml.loading.FMLPaths;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyPlatform;
import org.kylepitcock.legacyconnector.core.synth.Synth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minecraft Beta 1.7.3 (Risugami's ModLoader / Forge 1.0.x), run as ModLoader did: the vanilla blocks and items are in place first
 * ({@link VanillaB173}), each {@code mod_*} class gets its {@code @MLProp} config ({@link MLPropsB173}) and is constructed (a ModLoader
 * mod registers its blocks, items, names, recipes and textures from its constructor), then every mod gets {@code ModsLoaded()}. Their
 * blocks and items become modern registry entries afterwards ({@link BridgeB173}); recipes join the generated data pack
 * ({@link RecipesB173}). Not run yet: tick hooks, world generation, rendering, GUIs, tile entities and entities (recorded only).
 */
public final class PlatformB173 implements LegacyPlatform {
    public static final String VERSION = "b1.7.3";
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    private static boolean started;

    @Override
    public String version() {
        return VERSION;
    }

    /** Mods whose class could not be loaded or initialised in {@link #start}: their construction rethrows it. */
    private static final java.util.Map<LegacyModRecord, Throwable> PREPARE_FAILURES = new java.util.IdentityHashMap<>();

    /**
     * Once, before the first mod: the vanilla views, then every b1.7.3 mod class is loaded and initialised and its {@code @MLProp}
     * config read (ModLoader did both before constructing), and block ids two mods share are moved apart ({@link MLPropsB173#resolveClashes}).
     */
    private static synchronized void start() {
        if (started) return;
        started = true;
        Synth.addStaticResolver(VanillaB173::resolveStatic);
        VanillaB173.init();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        java.util.List<Class<?>> classes = new java.util.ArrayList<>();
        java.util.List<Object> owners = new java.util.ArrayList<>();
        for (LegacyModRecord mod : org.kylepitcock.legacyconnector.core.LegacyMods.ordered()) {
            if (!VERSION.equals(mod.version)) continue;
            int[] mark = ContentB173.mark();
            ContentB173.enter(mod);
            try {
                Class<?> cls = Class.forName(mod.mainClassName, true, loader);
                mod.mainClass = cls;
                MLPropsB173.load(cls, FMLPaths.CONFIGDIR.get());
                classes.add(cls);
                owners.add(mod);
            } catch (Throwable t) {
                ContentB173.release(mark);
                PREPARE_FAILURES.put(mod, t instanceof ExceptionInInitializerError e && e.getCause() != null ? e.getCause() : t);
            } finally {
                ContentB173.exit();
            }
        }
        MLPropsB173.resolveClashes(classes, owners, FMLPaths.CONFIGDIR.get());
    }

    @Override
    public void construct(LegacyModRecord mod) throws Exception {
        start();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Throwable prepared = PREPARE_FAILURES.get(mod);
        if (prepared instanceof Exception e) throw e;
        if (prepared instanceof Error e) throw e;
        ContentB173.enter(mod);
        int[] mark = ContentB173.mark();
        try {
            Class<?> cls = mod.mainClass != null ? mod.mainClass : Class.forName(mod.mainClassName, true, loader);
            mod.mainClass = cls;
            var ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object instance = null;
            for (int attempt = 0; instance == null; attempt++) {
                int[] attemptMark = ContentB173.mark();
                legacyconnector.vb1_7_3.net.minecraft.src.forge.Configuration.mcc$beginAttempt();
                try {
                    instance = ctor.newInstance();
                } catch (java.lang.reflect.InvocationTargetException e) {
                    // Two mods with the same block id: players edited the config file. The mod's @MLProp or Forge config property
                    // holding that id moves to a free one (and is saved) and the mod is constructed again.
                    int taken = occupiedSlot(e.getCause());
                    if (taken < 0 || attempt >= 32) throw e;
                    ContentB173.release(attemptMark);
                    if (!MLPropsB173.reassignBlockId(cls, taken, FMLPaths.CONFIGDIR.get())
                            && !legacyconnector.vb1_7_3.net.minecraft.src.forge.Configuration.mcc$reassign(taken)) throw e;
                    LOG.info("b1.7.3 mod {}: block id {} is taken by {}; its config now uses another id, constructing it again", mod.legacyId,
                            taken, legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS[taken]);
                }
            }
            mod.instance = instance;
            if (instance instanceof BaseMod base) {
                ContentB173.addMod(base);
                String version;
                try {
                    version = base.Version();
                } catch (RuntimeException | LinkageError e) {
                    version = "?";
                }
                LOG.info("b1.7.3 mod {} {} constructed", mod.legacyId, version);
            } else {
                LOG.warn("b1.7.3 mod class {} is not a BaseMod ({}); it was constructed but gets no ModLoader calls", mod.mainClassName,
                        instance.getClass().getSuperclass().getName());
            }
        } catch (Exception | LinkageError e) {
            // a mod that fails gives its block and item ids back, so the mods after it are not blocked by half its content
            int freed = ContentB173.release(mark);
            if (freed > 0) LOG.info("b1.7.3 mod {} failed; freed the {} block/item id(s) it had taken", mod.legacyId, freed);
            if (e instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() instanceof Exception cause) throw cause;
            if (e instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() instanceof LinkageError cause) throw cause;
            throw e;
        } finally {
            ContentB173.exit();
        }
    }

    /** The block id of b1.7.3's "Slot N is already occupied by ..." (Block's constructor), or -1. */
    private static int occupiedSlot(Throwable t) {
        if (!(t instanceof IllegalArgumentException) || t.getMessage() == null) return -1;
        var m = java.util.regex.Pattern.compile("^Slot (\\d+) is already occupied").matcher(t.getMessage());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    @Override
    public void preInit(LegacyModRecord mod) {}

    @Override
    public void init(LegacyModRecord mod) {}

    /** {@code BaseMod.ModsLoaded()}: every mod is constructed by now. */
    @Override
    public void postInit(LegacyModRecord mod) {
        if (!(mod.instance instanceof BaseMod base)) return;
        ContentB173.enter(mod);
        try {
            base.ModsLoaded();
        } finally {
            ContentB173.exit();
        }
    }

    @Override
    public void afterPostInit() {
        if (!started) return;
        BridgeB173.registerAll();
        CreativeTabsB173.register();
    }
}
