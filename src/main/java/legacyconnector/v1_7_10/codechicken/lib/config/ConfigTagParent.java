package legacyconnector.v1_7_10.codechicken.lib.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A node of a CodeChickenLib config file that has named child tags. */
public abstract class ConfigTagParent {
    public static final String[] EMPTY = new String[0];
    public final Map<String, ConfigTag> children = new LinkedHashMap<>();
    public String comment;
    public boolean brace = true;
    public boolean newlinemode = true;

    /** The child tag with the name (created, also through dots in the name, when missing). */
    public ConfigTag getTag(String tagname) {
        return getTag(tagname, true);
    }

    public ConfigTag getTag(String tagname, boolean create) {
        int dot = tagname.indexOf('.');
        String head = dot < 0 ? tagname : tagname.substring(0, dot);
        ConfigTag tag = children.get(head);
        if (tag == null) {
            if (!create) return null;
            tag = new ConfigTag(this, head);
            children.put(head, tag);
        }
        return dot < 0 ? tag : tag.getTag(tagname.substring(dot + 1), create);
    }

    public boolean containsTag(String tagname) {
        return getTag(tagname, false) != null;
    }

    public void removeTag(String tagname) {
        int dot = tagname.indexOf('.');
        if (dot < 0) {
            children.remove(tagname);
        } else {
            ConfigTag sub = children.get(tagname.substring(0, dot));
            if (sub != null) sub.removeTag(tagname.substring(dot + 1));
        }
    }

    public List<ConfigTag> getSortedTagList() {
        List<ConfigTag> list = new ArrayList<>(children.values());
        list.sort((a, b) -> a.name.compareTo(b.name));
        return Collections.unmodifiableList(list);
    }

    public boolean hasChildTags() {
        return !children.isEmpty();
    }

    public ConfigTagParent setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public ConfigTagParent useBraces() {
        brace = true;
        return this;
    }

    public ConfigTagParent setNewLineMode(boolean mode) {
        newlinemode = mode;
        return this;
    }

    public abstract String getNameQualifier();

    void write(StringBuilder out, String indent) {
        for (ConfigTag t : children.values()) t.writeTag(out, indent);
    }
}
