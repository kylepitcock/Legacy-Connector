package legacyconnector.v1_12_2.net.minecraft.client.util;

import java.util.HashMap;
import java.util.Map;

/** 1.12's holder of the creative-tab and recipe-book search trees. */
public class SearchTreeManager {
    public static final Key<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> ITEMS = new Key<>();
    public static final Key<Object> RECIPES = new Key<>();

    private final Map<Key<?>, SearchTree<?>> trees = new HashMap<>();

    public SearchTreeManager() {
        trees.put(ITEMS, new SearchTree<>());
        trees.put(RECIPES, new SearchTree<>());
    }

    @SuppressWarnings("unchecked")
    public <T> ISearchTree<T> get(Key<T> key) {
        return (ISearchTree<T>) trees.computeIfAbsent(key, k -> new SearchTree<>());
    }

    public <T> void register(Key<T> key, SearchTree<T> tree) {
        trees.put(key, tree);
    }

    public static class Key<T> {
    }
}
