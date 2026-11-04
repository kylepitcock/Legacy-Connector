package legacyconnector.v1_12_2.net.minecraft.client.util;

import java.util.List;

/**
 * 1.12's search tree: {@link #recalculate} rebuilds it from the game's contents. The modern game keeps its own search trees (JEI and the
 * creative tab search), so this one only answers empty results; mods (CraftTweaker) call {@code recalculate()} after changing recipes.
 */
public class SearchTree<T> implements ISearchTree<T> {
    public void recalculate() {
    }

    @Override
    public List<T> search(String searchText) {
        return List.of();
    }
}
