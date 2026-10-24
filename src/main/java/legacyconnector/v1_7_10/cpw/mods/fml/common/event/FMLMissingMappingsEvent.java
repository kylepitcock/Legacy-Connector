package legacyconnector.v1_7_10.cpw.mods.fml.common.event;

import java.util.ArrayList;
import java.util.List;

/**
 * A world saved with ids that no longer exist. Ids are not persisted by number here, so nothing is ever missing and the
 * event carries no mappings; mods that handle it (Railcraft) just see an empty list.
 */
public class FMLMissingMappingsEvent extends FMLEvent {
    private final List<MissingMapping> mappings = new ArrayList<>();

    public List<MissingMapping> getAll() {
        return mappings;
    }

    public List<MissingMapping> get(String modId) {
        List<MissingMapping> out = new ArrayList<>();
        for (MissingMapping m : mappings) if (m.name.startsWith(modId + ":")) out.add(m);
        return out;
    }

    public static class MissingMapping {
        public final String name;
        public final int id;

        public MissingMapping(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public void ignore() {
        }

        public void warn() {
        }

        public void fail() {
        }
    }
}
