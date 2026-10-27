package legacyconnector.v1_12_2.net.minecraft.client.resources;

/** A language the game offers: its code ({@code en_us}), name, region and reading direction. */
public class Language implements Comparable<Language> {
    private final String languageCode;
    private final String region;
    private final String name;
    private final boolean bidirectional;

    public Language(String languageCodeIn, String regionIn, String nameIn, boolean bidirectionalIn) {
        this.languageCode = languageCodeIn;
        this.region = regionIn;
        this.name = nameIn;
        this.bidirectional = bidirectionalIn;
    }

    public String getLanguageCode() {
        return languageCode;
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
        return this == o || o instanceof Language l && languageCode.equals(l.languageCode);
    }

    @Override
    public int hashCode() {
        return languageCode.hashCode();
    }

    @Override
    public int compareTo(Language other) {
        return languageCode.compareTo(other.languageCode);
    }
}
