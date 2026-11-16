package legacyconnector.v1_6_4.net.minecraft.src.forge;

/** One entry of a Forge 3.x configuration file. */
public class Property {
    public String name;
    public String value;
    public String comment;

    public Property() {}

    public int getInt() {
        return getInt(-1);
    }

    public int getInt(int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    public boolean isIntValue() {
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean getBoolean(boolean fallback) {
        if (isBooleanValue()) return Boolean.parseBoolean(value.trim());
        return fallback;
    }

    public boolean isBooleanValue() {
        return value != null && ("true".equalsIgnoreCase(value.trim()) || "false".equalsIgnoreCase(value.trim()));
    }
}
