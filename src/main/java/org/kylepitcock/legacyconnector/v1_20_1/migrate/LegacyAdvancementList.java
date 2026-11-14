package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.ResourceLocation;

/**
 * Stands in for 1.20.1's {@code AdvancementList}: the set of advancements a client or server knows, with their roots and a listener. 1.21.1 calls it
 * {@code AdvancementTree} and keeps nodes instead of advancements; this class wraps a tree and answers in 1.20.1's terms (an advancement is the
 * {@code AdvancementHolder} here, see the class rename). The tree behind {@code ClientAdvancements.getAdvancements()} is the game's own, so what a mod reads
 * and what it adds through {@link #add} is what the advancement screen shows.
 */
public class LegacyAdvancementList {
    private final AdvancementTree tree;

    public LegacyAdvancementList() {
        this(new AdvancementTree());
    }

    public LegacyAdvancementList(AdvancementTree tree) {
        this.tree = tree;
    }

    /** {@code add(Map<ResourceLocation, Advancement.Builder>)}: builds each advancement with its id and inserts them (a child after its parent, as before). */
    public void add(Map<ResourceLocation, Advancement.Builder> builders) {
        List<AdvancementHolder> holders = new ArrayList<>(builders.size());
        builders.forEach((id, builder) -> holders.add(builder.build(id)));
        tree.addAll(holders);
    }

    /** The 1.21.1 way in: ready holders. */
    public void addAll(Collection<AdvancementHolder> advancements) {
        tree.addAll(advancements);
    }

    public void remove(Set<ResourceLocation> ids) {
        tree.remove(ids);
    }

    public void clear() {
        tree.clear();
    }

    public Iterable<AdvancementHolder> getRoots() {
        List<AdvancementHolder> out = new ArrayList<>();
        for (AdvancementNode n : tree.roots()) out.add(n.holder());
        return out;
    }

    public Iterable<AdvancementHolder> getAllAdvancements() {
        List<AdvancementHolder> out = new ArrayList<>();
        for (AdvancementNode n : tree.nodes()) out.add(n.holder());
        return out;
    }

    @Nullable
    public AdvancementHolder get(ResourceLocation id) {
        AdvancementNode node = tree.get(id);
        return node == null ? null : node.holder();
    }

    /** The node view of the same data (1.21.1's own type, for code that already speaks it). */
    public AdvancementTree mcc$tree() {
        return tree;
    }

    private final Map<Listener, AdvancementTree.Listener> adapters = new LinkedHashMap<>();

    public void setListener(@Nullable Listener listener) {
        if (listener == null) {
            tree.setListener(null);
            return;
        }
        tree.setListener(new AdvancementTree.Listener() {
            @Override
            public void onAddAdvancementRoot(AdvancementNode advancement) {
                listener.onAddAdvancementRoot(advancement.holder());
            }

            @Override
            public void onRemoveAdvancementRoot(AdvancementNode advancement) {
                listener.onRemoveAdvancementRoot(advancement.holder());
            }

            @Override
            public void onAddAdvancementTask(AdvancementNode advancement) {
                listener.onAddAdvancementTask(advancement.holder());
            }

            @Override
            public void onRemoveAdvancementTask(AdvancementNode advancement) {
                listener.onRemoveAdvancementTask(advancement.holder());
            }

            @Override
            public void onAdvancementsCleared() {
                listener.onAdvancementsCleared();
            }
        });
    }

    /** {@code AdvancementList.Listener}. */
    public interface Listener {
        void onAddAdvancementRoot(AdvancementHolder advancement);

        void onRemoveAdvancementRoot(AdvancementHolder advancement);

        void onAddAdvancementTask(AdvancementHolder advancement);

        void onRemoveAdvancementTask(AdvancementHolder advancement);

        void onAdvancementsCleared();
    }
}
