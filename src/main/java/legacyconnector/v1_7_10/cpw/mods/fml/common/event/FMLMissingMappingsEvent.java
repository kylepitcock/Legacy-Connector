package legacyconnector.v1_7_10.cpw.mods.fml.common.event;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer;
import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.GameRegistry;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;

/**
 * A world saved with ids that no longer exist. Ids are not persisted by number here, so nothing is ever missing and the
 * event normally carries no mappings; mods that handle it (Railcraft, BuildCraft, Thermal Expansion) see an empty list.
 * The structure is the legacy one (mappings by mod id, each with an action and a remap target) so mods that build the
 * event themselves (BuildCraft's blueprint mapping registry) work.
 */
public class FMLMissingMappingsEvent extends FMLEvent {
    private final ListMultimap<String, MissingMapping> missing;
    private ModContainer activeContainer;

    public FMLMissingMappingsEvent(ListMultimap<String, MissingMapping> missingMappings) {
        this.missing = missingMappings;
    }

    public FMLMissingMappingsEvent() {
        this(ArrayListMultimap.create());
    }

    /** FML sets the mod whose handler runs next; {@link #get()} returns that mod's mappings. */
    public void applyModContainer(ModContainer activeContainer) {
        this.activeContainer = activeContainer;
    }

    /** The missing mappings of the mod whose handler is running. */
    public List<MissingMapping> get() {
        if (activeContainer == null) return ImmutableList.of();
        return ImmutableList.copyOf(missing.get(activeContainer.getModId()));
    }

    public List<MissingMapping> getAll() {
        return ImmutableList.copyOf(missing.values());
    }

    public List<MissingMapping> get(String modId) {
        List<MissingMapping> out = new ArrayList<>(missing.get(modId));
        if (out.isEmpty()) for (MissingMapping m : missing.values()) if (m.name.startsWith(modId + ":")) out.add(m);
        return out;
    }

    public enum Action {
        DEFAULT, IGNORE, WARN, FAIL, REMAP, BLOCKONLY
    }

    public static class MissingMapping {
        public final GameRegistry.Type type;
        public final String name;
        public final int id;
        private Action action = Action.DEFAULT;
        private Object target;

        public MissingMapping(GameRegistry.Type type, String name, int id) {
            this.type = type;
            this.name = name;
            this.id = id;
        }

        public MissingMapping(String name, int id) {
            this(GameRegistry.Type.ITEM, name, id);
        }

        public void ignore() {
            action = Action.IGNORE;
        }

        public void warn() {
            action = Action.WARN;
        }

        public void fail() {
            action = Action.FAIL;
        }

        public void skipItemBlock() {
            if (type != GameRegistry.Type.ITEM) throw new IllegalArgumentException("Cannot skip an item that is a block");
            action = Action.BLOCKONLY;
        }

        public void remap(Block target) {
            if (type != GameRegistry.Type.BLOCK) throw new IllegalArgumentException("Can't remap an item to a block");
            if (target == null) throw new NullPointerException("remap target is null");
            setAction(Action.REMAP, target);
        }

        public void remap(Item target) {
            if (type != GameRegistry.Type.ITEM) throw new IllegalArgumentException("Can't remap a block to an item");
            if (target == null) throw new NullPointerException("remap target is null");
            setAction(Action.REMAP, target);
        }

        private void setAction(Action action, Object target) {
            this.action = action;
            this.target = target;
        }

        public Action getAction() {
            return action;
        }

        public Object getTarget() {
            return target;
        }
    }
}
