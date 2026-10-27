package legacyconnector.v1_12_2.net.minecraft.client.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The creative search's index: objects found by any substring of their texts (a plain scan instead of 1.12's suffix array). */
public class SuffixArray<T> {
    private final List<T> objects = new ArrayList<>();
    private final List<String> texts = new ArrayList<>();

    public SuffixArray() {
    }

    public void add(T object, String text) {
        objects.add(object);
        texts.add(text == null ? "" : text);
    }

    /** 1.12 built its index here; the scan needs none. */
    public void generate() {
    }

    /** The objects with a text containing the query, in the order they were added. */
    public List<T> search(String query) {
        Set<T> out = new LinkedHashSet<>();
        for (int i = 0; i < texts.size(); i++) if (texts.get(i).contains(query)) out.add(objects.get(i));
        return new ArrayList<>(out);
    }
}
