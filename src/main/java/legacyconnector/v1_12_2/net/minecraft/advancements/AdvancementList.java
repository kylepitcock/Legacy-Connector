package legacyconnector.v1_12_2.net.minecraft.advancements;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** The loaded advancements (the client's or the server's): a view over the server's advancements. */
public class AdvancementList {
    public Advancement getAdvancement(ResourceLocation id) {
        return AdvancementManager.INSTANCE.getAdvancement(id);
    }

    public Iterable<Advancement> getAdvancements() {
        return AdvancementManager.INSTANCE.getAdvancements();
    }

    public Iterable<Advancement> getRoots() {
        List<Advancement> out = new ArrayList<>();
        for (Advancement a : getAdvancements()) if (a.getParent() == null) out.add(a);
        return out;
    }
}
