package mccompat.v1_7_10.net.minecraft.client.resources;

/** A language of the game (1.7.10: code like "en_US", display name). */
public class Language implements Comparable<Language> {
    private final String code;
    private final String region;
    private final String name;
    private final boolean bidirectional;

    public Language(String code, String region, String name, boolean bidirectional) {
        this.code = code;
        this.region = region;
        this.name = name;
        this.bidirectional = bidirectional;
    }

    public String getLanguageCode() {
        return code;
    }

    public boolean isBidirectional() {
        return bidirectional;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, region);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Language l && code.equals(l.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public int compareTo(Language other) {
        return code.compareTo(other.code);
    }
}
