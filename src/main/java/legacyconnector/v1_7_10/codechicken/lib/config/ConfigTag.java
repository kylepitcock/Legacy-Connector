package legacyconnector.v1_7_10.codechicken.lib.config;

/** A tag of a CodeChickenLib config file: an optional value, comment and child tags. */
public class ConfigTag extends ConfigTagParent {
    public final ConfigTagParent parent;
    public final String name;
    private String value;

    public ConfigTag(ConfigTagParent parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    public String getNameQualifier() {
        String q = parent == null ? "" : parent.getNameQualifier();
        return q.isEmpty() ? name : q + "." + name;
    }

    public String getValue() {
        return value;
    }

    public String getValue(String defaultValue) {
        if (value == null) value = defaultValue;
        return value;
    }

    public ConfigTag setValue(String s) {
        value = s;
        return this;
    }

    public ConfigTag setDefaultValue(String s) {
        if (value == null) value = s;
        return this;
    }

    public int getIntValue() {
        return Integer.parseInt(value.trim());
    }

    public int getIntValue(int defaultValue) {
        try {
            if (value != null) return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            // fall through to the default, as the original does
        }
        value = Integer.toString(defaultValue);
        return defaultValue;
    }

    public ConfigTag setIntValue(int i) {
        value = Integer.toString(i);
        return this;
    }

    public int getHexValue() {
        return Integer.parseUnsignedInt(value.trim().replace("0x", ""), 16);
    }

    public int getHexValue(int defaultValue) {
        try {
            if (value != null) return Integer.parseUnsignedInt(value.trim().replace("0x", ""), 16);
        } catch (NumberFormatException ignored) {
            // default
        }
        value = "0x" + Integer.toHexString(defaultValue).toUpperCase();
        return defaultValue;
    }

    public ConfigTag setHexValue(int i) {
        value = "0x" + Integer.toHexString(i).toUpperCase();
        return this;
    }

    public boolean getBooleanValue() {
        return Boolean.parseBoolean(value.trim());
    }

    public boolean getBooleanValue(boolean defaultValue) {
        if (value != null && (value.trim().equalsIgnoreCase("true") || value.trim().equalsIgnoreCase("false"))) return Boolean.parseBoolean(value.trim());
        value = Boolean.toString(defaultValue);
        return defaultValue;
    }

    public ConfigTag setBooleanValue(boolean b) {
        value = Boolean.toString(b);
        return this;
    }

    public ConfigTag setComment(String comment) {
        this.comment = comment;
        return this;
    }

    void writeTag(StringBuilder out, String indent) {
        if (comment != null) for (String line : comment.split("\n")) out.append(indent).append("# ").append(line).append('\n');
        out.append(indent).append(name);
        if (value != null) out.append('=').append(value);
        if (!children.isEmpty()) {
            out.append(" {\n");
            write(out, indent + "    ");
            out.append(indent).append("}\n");
        } else {
            out.append('\n');
        }
    }
}
