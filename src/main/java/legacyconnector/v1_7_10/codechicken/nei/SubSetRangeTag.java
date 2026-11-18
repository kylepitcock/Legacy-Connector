package legacyconnector.v1_7_10.codechicken.nei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NEI 1.4.7 / 1.5.2's tree of item panel subsets ({@code API.getRangeTag("Mod").getTag("Group").setRange(range)}). JEI has no subset menu, so
 * the tree is only kept: plugins build it while configuring NEI and read it back, none of it is drawn.
 */
public class SubSetRangeTag {
    public final String name;
    public final SubSetRangeTag parent;
    public final ArrayList<SubSetRangeTag> sortedtags = new ArrayList<>();
    private final Map<String, SubSetRangeTag> children = new LinkedHashMap<>();
    private MultiItemRange range;
    private String comment;
    public boolean saveTag = true;
    public boolean expanded;
    public boolean hidden;
    public int position = Integer.MAX_VALUE;

    public SubSetRangeTag(String name) {
        this(null, name);
    }

    public SubSetRangeTag(SubSetRangeTag parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    public SubSetRangeTag getTag(String tagname) {
        return getTag(tagname, true);
    }

    public SubSetRangeTag getTag(String tagname, boolean create) {
        SubSetRangeTag tag = children.get(tagname);
        if (tag == null && create) {
            tag = new SubSetRangeTag(this, tagname);
            children.put(tagname, tag);
            sortedtags.add(tag);
        }
        return tag;
    }

    public SubSetRangeTag getNewTag(String tagname) {
        return getTag(tagname, true);
    }

    public SubSetRangeTag setPosition(int position) {
        this.position = position;
        return this;
    }

    public SubSetRangeTag setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public SubSetRangeTag useBraces() {
        return this;
    }

    public void setSave(boolean save) {
        this.saveTag = save;
    }

    public void setDefaultValue(MultiItemRange range) {
        if (this.range == null) this.range = range;
    }

    public void setRange(MultiItemRange range) {
        this.range = range;
    }

    public void setValue(String value) {
    }

    public MultiItemRange getRange() {
        return range;
    }

    @Override
    public String toString() {
        return name + (comment == null ? "" : " (" + comment + ")");
    }
}
