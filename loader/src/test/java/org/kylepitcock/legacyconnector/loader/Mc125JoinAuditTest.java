package org.kylepitcock.legacyconnector.loader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The 1.2.5 to 1.4.7 join must not bind two members that have different Feather names unless somebody reviewed that pair (a Feather rename between builds): a shared Calamus intermediary id
 * was reused for other members (PlayerManager.unban joined to removeFromWhitelist). Needs the generated table ({@code -Pmappings125}); {@code genMappings125} writes the joins it accepted
 * with different names to {@code join-name-changes.tsv}.
 */
class Mc125JoinAuditTest {
    private static Set<String> lines(String resource) throws IOException {
        Set<String> out = new HashSet<>();
        try (InputStream in = Mc125JoinAuditTest.class.getResourceAsStream(resource)) {
            if (in == null) return null;
            for (String l : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) if (!l.isBlank() && !l.startsWith("#")) out.add(l.replace("\r", ""));
        }
        return out;
    }

    @Test
    void everyJoinWithDifferentFeatherNamesWasReviewed() throws IOException {
        Set<String> accepted = lines("/join-name-changes.tsv");
        assumeTrue(accepted != null, "built without -Pmappings125");
        Set<String> reviewed = lines("/legacyconnector/mappings/1.2.5/reviewed-name-changes.tsv");
        assertTrue(reviewed != null && !reviewed.isEmpty(), "reviewed-name-changes.tsv");
        for (String line : accepted) {
            String[] p = line.split("\t");
            String key = p[1] + "\t" + p[2] + "\t" + p[3]; // side, owner, name 1.2.5, name 1.4.7, descriptor
            assertTrue(reviewed.contains(key), "joined with different Feather names and not reviewed: " + line);
        }
    }
}
