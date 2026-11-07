package org.kylepitcock.legacyconnector.compat.waila;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_7_10.cpw.mods.fml.common.event.FMLInterModComms;
import legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaConfigHandler;
import legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaDataProvider;
import legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaEntityProvider;
import legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Waila -> Jade bridge, Jade-independent half: collects the providers legacy mods register through Waila's IMC
 * callback ({@code sendMessage("Waila", "register", "pkg.Class.method")}). The Jade plugin reads them.
 */
public final class WailaBridge implements IWailaRegistrar, IWailaConfigHandler {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Waila");
    public static final WailaBridge INSTANCE = new WailaBridge();

    /** {@code only}: a registration for one block instance (numeric ids), else every block or tile of the class. */
    public record Registration<P>(P provider, Class<?> target, Object only) {
        public Registration(P provider, Class<?> target) {
            this(provider, target, null);
        }
    }

    public final List<Registration<IWailaDataProvider>> stack = new ArrayList<>();
    public final List<Registration<IWailaDataProvider>> head = new ArrayList<>();
    public final List<Registration<IWailaDataProvider>> body = new ArrayList<>();
    public final List<Registration<IWailaDataProvider>> tail = new ArrayList<>();
    public final List<Registration<IWailaDataProvider>> nbt = new ArrayList<>();
    public final List<Registration<IWailaEntityProvider>> entityNbt = new ArrayList<>();
    public final List<Registration<IWailaEntityProvider>> entityHead = new ArrayList<>();
    public final List<Registration<IWailaEntityProvider>> entityBody = new ArrayList<>();
    public final List<Registration<IWailaEntityProvider>> entityTail = new ArrayList<>();
    private final Map<String, Boolean> config = new HashMap<>();
    private final Map<String, HashMap<String, String>> modules = new HashMap<>();

    private WailaBridge() {}

    /** Runs the registration callbacks queued for Waila; called once after legacy postInit. */
    public static void processRegistrations() {
        int n = 0;
        for (FMLInterModComms.IMCMessage msg : FMLInterModComms.mcc$drain("Waila")) {
            if (!"register".equals(msg.key) || !msg.isStringMessage()) continue;
            String target = msg.getStringValue();
            int dot = target.lastIndexOf('.');
            try {
                Class<?> cls = Class.forName(target.substring(0, dot), true, Thread.currentThread().getContextClassLoader());
                Method m = cls.getMethod(target.substring(dot + 1), IWailaRegistrar.class);
                m.invoke(null, INSTANCE);
                n++;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                LOG.warn("Waila registration {} failed: {}", target, e.toString());
            }
        }
        if (n > 0) LOG.info("Ran {} legacy Waila registration(s); {} block and {} entity providers", n,
                INSTANCE.head.size() + INSTANCE.body.size() + INSTANCE.tail.size(), INSTANCE.entityHead.size() + INSTANCE.entityBody.size() + INSTANCE.entityTail.size());
    }

    public boolean isEmpty() {
        return head.isEmpty() && body.isEmpty() && tail.isEmpty() && stack.isEmpty()
                && entityHead.isEmpty() && entityBody.isEmpty() && entityTail.isEmpty();
    }

    // ------------------------------------------------------------------ IWailaRegistrar

    @Override
    public void addConfig(String modName, String key, String text) {
        addConfig(modName, key, text, true);
    }

    @Override
    public void addConfig(String modName, String key, String text, boolean defaultValue) {
        config.putIfAbsent(key, defaultValue);
        modules.computeIfAbsent(modName, k -> new HashMap<>()).put(key, text);
    }

    @Override
    public void addConfigRemote(String modName, String key, String text) {
        addConfig(modName, key, text, true);
    }

    @Override
    public void addConfigRemote(String modName, String key, String text, boolean defaultValue) {
        addConfig(modName, key, text, defaultValue);
    }

    @Override
    public void addConfig(String modName, String key) {
        addConfig(modName, key, key, true);
    }

    @Override
    public void addConfig(String modName, String key, boolean defaultValue) {
        addConfig(modName, key, key, defaultValue);
    }

    @Override
    public void addConfigRemote(String modName, String key) {
        addConfig(modName, key, key, true);
    }

    @Override
    public void addConfigRemote(String modName, String key, boolean defaultValue) {
        addConfig(modName, key, key, defaultValue);
    }

    @Override
    public void registerStackProvider(IWailaDataProvider provider, Class<?> block) {
        stack.add(new Registration<>(provider, block));
    }

    @Override
    public void registerHeadProvider(IWailaDataProvider provider, Class<?> block) {
        head.add(new Registration<>(provider, block));
    }

    @Override
    public void registerBodyProvider(IWailaDataProvider provider, Class<?> block) {
        body.add(new Registration<>(provider, block));
    }

    @Override
    public void registerTailProvider(IWailaDataProvider provider, Class<?> block) {
        tail.add(new Registration<>(provider, block));
    }

    @Override
    public void registerNBTProvider(IWailaDataProvider provider, Class<?> block) {
        nbt.add(new Registration<>(provider, block));
    }

    /** 1.6.4 and older registered by numeric block id: the provider applies to that one block. */
    public void registerForBlock(int kind, IWailaDataProvider provider, legacyconnector.v1_7_10.net.minecraft.block.Block block) {
        if (block == null) return;
        Registration<IWailaDataProvider> r = new Registration<>(provider, block.getClass(), block);
        switch (kind) {
            case 0 -> stack.add(r);
            case 1 -> head.add(r);
            case 2 -> body.add(r);
            case 3 -> tail.add(r);
            default -> nbt.add(r);
        }
    }

    @Override
    public void registerHeadProvider(IWailaEntityProvider provider, Class<?> entity) {
        entityHead.add(new Registration<>(provider, entity));
    }

    @Override
    public void registerBodyProvider(IWailaEntityProvider provider, Class<?> entity) {
        entityBody.add(new Registration<>(provider, entity));
    }

    @Override
    public void registerTailProvider(IWailaEntityProvider provider, Class<?> entity) {
        entityTail.add(new Registration<>(provider, entity));
    }

    @Override
    public void registerOverrideEntityProvider(IWailaEntityProvider provider, Class<?> entity) {
    }

    @Override
    public void registerNBTProvider(IWailaEntityProvider provider, Class<?> entity) {
        entityNbt.add(new Registration<>(provider, entity));
    }

    @Override
    public void registerSyncedNBTKey(String key, Class<?> target) {
    }

    // ------------------------------------------------------------------ IWailaConfigHandler

    @Override
    public Set<String> getModuleNames() {
        return modules.keySet();
    }

    @Override
    public HashMap<String, String> getConfigKeys(String module) {
        return modules.getOrDefault(module, new HashMap<>());
    }

    @Override
    public boolean getConfig(String key, boolean defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean getConfig(String key) {
        return getConfig(key, true);
    }

    /** Waila's {@code ConfigHandler.setConfig}: switches one option. */
    public void mcc$setConfig(String key, boolean value) {
        config.put(key, value);
    }
}
