package legacyconnector.v1_12_2.net.minecraft.advancements;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** An advancement: a view over the modern one the server loaded. */
public class Advancement {
    private final net.minecraft.advancements.AdvancementHolder holder;

    public Advancement(net.minecraft.advancements.AdvancementHolder holder) {
        this.holder = holder;
    }

    public static Advancement mcc$of(net.minecraft.advancements.AdvancementHolder holder) {
        return holder == null ? null : new Advancement(holder);
    }

    public net.minecraft.advancements.AdvancementHolder mcc$modern() {
        return holder;
    }

    public ResourceLocation getId() {
        return new ResourceLocation(holder.id().getNamespace(), holder.id().getPath());
    }

    public Advancement getParent() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        var parent = holder.value().parent().orElse(null);
        return parent == null || server == null ? null : mcc$of(server.getAdvancements().get(parent));
    }

    /** The criteria by name (values are the modern criteria; mods only read the names). */
    public java.util.Map<String, Object> getCriteria() {
        return new java.util.LinkedHashMap<>(holder.value().criteria());
    }

    public int getRequirementCount() {
        return holder.value().requirements().size();
    }

    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayText() {
        return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(holder.value().name().map(c -> c.getString()).orElse(getId().toString()));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Advancement a && a.holder.id().equals(holder.id());
    }

    @Override
    public int hashCode() {
        return holder.id().hashCode();
    }

    @Override
    public String toString() {
        return "SimpleAdvancement{id=" + holder.id() + '}';
    }
}
