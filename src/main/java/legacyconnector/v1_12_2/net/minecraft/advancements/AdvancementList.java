package legacyconnector.v1_12_2.net.minecraft.advancements;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * A tree of advancements: on the client, a view of the game's client advancement tree ({@code connection.getAdvancementManager()}).
 * (Created by area R2b for the client advancement manager.)
 */
public class AdvancementList {
    private final net.minecraft.advancements.AdvancementTree tree;

    public AdvancementList() {
        this(new net.minecraft.advancements.AdvancementTree());
    }

    public AdvancementList(net.minecraft.advancements.AdvancementTree tree) {
        this.tree = tree;
    }

    public net.minecraft.advancements.AdvancementTree mcc$modern() {
        return tree;
    }

    public Iterable<Advancement> getRoots() {
        List<Advancement> out = new ArrayList<>();
        for (var node : tree.roots()) out.add(Advancement.mcc$of(node.holder()));
        return out;
    }

    public Iterable<Advancement> getAdvancements() {
        List<Advancement> out = new ArrayList<>();
        for (var node : tree.nodes()) out.add(Advancement.mcc$of(node.holder()));
        return out;
    }

    public Advancement getAdvancement(ResourceLocation id) {
        var node = id == null ? null : tree.get(id.mcc$modern());
        return node == null ? null : Advancement.mcc$of(node.holder());
    }

    public void clear() {
        tree.clear();
    }

    public void setListener(Listener listener) {
        tree.setListener(listener == null ? null : mcc$listener(listener));
    }

    /** A legacy listener as the game's tree listener (not a 1.12 method). */
    public static net.minecraft.advancements.AdvancementTree.Listener mcc$listener(Listener l) {
        return new net.minecraft.advancements.AdvancementTree.Listener() {
            @Override
            public void onAddAdvancementRoot(net.minecraft.advancements.AdvancementNode node) {
                l.rootAdvancementAdded(Advancement.mcc$of(node.holder()));
            }

            @Override
            public void onRemoveAdvancementRoot(net.minecraft.advancements.AdvancementNode node) {
                l.rootAdvancementRemoved(Advancement.mcc$of(node.holder()));
            }

            @Override
            public void onAddAdvancementTask(net.minecraft.advancements.AdvancementNode node) {
                l.nonRootAdvancementAdded(Advancement.mcc$of(node.holder()));
            }

            @Override
            public void onRemoveAdvancementTask(net.minecraft.advancements.AdvancementNode node) {
                l.nonRootAdvancementRemoved(Advancement.mcc$of(node.holder()));
            }

            @Override
            public void onAdvancementsCleared() {
                l.advancementsCleared();
            }
        };
    }

    public interface Listener {
        void rootAdvancementAdded(Advancement advancement);

        void rootAdvancementRemoved(Advancement advancement);

        void nonRootAdvancementAdded(Advancement advancement);

        void nonRootAdvancementRemoved(Advancement advancement);

        void advancementsCleared();
    }
}
