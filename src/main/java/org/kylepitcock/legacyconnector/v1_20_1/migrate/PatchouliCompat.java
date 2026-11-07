package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonElement;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;
import vazkii.patchouli.api.VariableHelper;

/**
 * Patchouli 1.20.1 (build 85) against the installed Patchouli 1.21.1 (build 93): the variable API gained a {@code HolderLookup.Provider} parameter
 * (item stacks in variables are components now). The provider is the running level's registries.
 */
public final class PatchouliCompat {
    private PatchouliCompat() {
    }

    /** The registries of the running server or client level (the built-in ones before a level exists). */
    public static HolderLookup.Provider registries() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess();
        if (FMLEnvironment.dist.isClient()) {
            HolderLookup.Provider p = ClientRegistries.get();
            if (p != null) return p;
        }
        return RegistryAccess.EMPTY;
    }

    /** The client class stays out of the common method so a dedicated server never loads it. */
    private static final class ClientRegistries {
        static HolderLookup.Provider get() {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            return level != null ? level.registryAccess() : null;
        }
    }

    public static <T> IVariable from(T value) {
        return IVariable.from(value, registries());
    }

    public static IVariable wrap(JsonElement json) {
        return IVariable.wrap(json, registries());
    }

    public static IVariable wrapList(Iterable<IVariable> list) {
        return IVariable.wrapList(list, registries());
    }

    public static IVariable get(IVariableProvider provider, String key) {
        return provider.get(key, registries());
    }

    public static Stream<IVariable> asStream(IVariable v) {
        return v.asStream(registries());
    }

    public static Stream<IVariable> asStreamOrSingleton(IVariable v) {
        return v.asStreamOrSingleton(registries());
    }

    public static List<IVariable> asList(IVariable v) {
        return v.asList(registries());
    }

    public static List<IVariable> asListOrSingleton(IVariable v) {
        return v.asListOrSingleton(registries());
    }

    public static <T> IVariable createFromObject(VariableHelper h, T value) {
        return h.createFromObject(value, registries());
    }

    public static IVariable createFromJson(VariableHelper h, JsonElement json) {
        return h.createFromJson(json, registries());
    }
}
