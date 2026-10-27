package legacyconnector.v1_12_2.net.minecraft.advancements;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** The server's advancements: a view over the modern server advancement manager. */
public class AdvancementManager {
    public static final AdvancementManager INSTANCE = new AdvancementManager();

    private static net.minecraft.server.ServerAdvancementManager mcc$modern() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getAdvancements();
    }

    public Advancement getAdvancement(ResourceLocation id) {
        var modern = mcc$modern();
        return modern == null || id == null ? null : Advancement.mcc$of(modern.get(id.mcc$modern()));
    }

    public Iterable<Advancement> getAdvancements() {
        var modern = mcc$modern();
        java.util.List<Advancement> out = new java.util.ArrayList<>();
        if (modern != null) for (var h : modern.getAllAdvancements()) out.add(Advancement.mcc$of(h));
        return out;
    }

    public void reload() {
    }

    public boolean isLoaded() {
        return mcc$modern() != null;
    }
}
