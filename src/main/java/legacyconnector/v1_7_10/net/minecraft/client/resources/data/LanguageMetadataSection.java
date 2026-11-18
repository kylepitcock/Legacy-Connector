package legacyconnector.v1_7_10.net.minecraft.client.resources.data;

import java.util.Collection;

/** The "language" section of a pack.mcmeta: the languages the pack adds. */
public class LanguageMetadataSection implements IMetadataSection {
    private final Collection languages;

    public LanguageMetadataSection(Collection languages) {
        this.languages = languages;
    }

    public Collection getLanguages() {
        return languages;
    }
}
