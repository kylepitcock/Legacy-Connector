package legacyconnector.v1_12_2.net.minecraft.client.util;

import java.util.List;

/** 1.12's creative and recipe book search tree. */
public interface ISearchTree<T> {
    List<T> search(String searchText);
}
