package legacyconnector.v1_12_2.net.minecraft.command;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.advancements.FunctionManager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * A function: commands run in order. A data pack function of the modern server is run by the modern function manager; one a mod made
 * from lines ({@link #create}) runs its commands through the modern command dispatcher.
 */
public class FunctionObject {
    public static final FunctionObject EMPTY = new FunctionObject(new Entry[0]);
    private final Entry[] entries;
    /** The modern function this one is, or null. */
    private final ResourceLocation mcc$id;

    public FunctionObject(Entry[] entriesIn) {
        this(entriesIn, null);
    }

    private FunctionObject(Entry[] entriesIn, ResourceLocation modernId) {
        this.entries = entriesIn;
        this.mcc$id = modernId;
    }

    /** The legacy object of a data pack function of the modern server. */
    public static FunctionObject mcc$ofModern(ResourceLocation id) {
        return new FunctionObject(new Entry[0], id);
    }

    /** The modern function this one is, or null for one a mod made. */
    public ResourceLocation mcc$id() {
        return mcc$id;
    }

    public Entry[] getEntries() {
        return entries;
    }

    /** A function from the lines of a function file: comments and blank lines skipped, {@code function x} lines call other functions. */
    public static FunctionObject create(FunctionManager functionManagerIn, List<String> commands) {
        List<Entry> list = new ArrayList<>(commands.size());
        for (String raw : commands) {
            String s = raw.trim();
            if (s.startsWith("#") || s.isEmpty()) continue;
            String[] parts = s.split(" ", 2);
            if ("function".equals(parts[0]) && parts.length > 1) list.add(new FunctionEntry(new CacheableFunction(new ResourceLocation(parts[1].trim()))));
            else list.add(new CommandEntry(s));
        }
        return new FunctionObject(list.toArray(new Entry[0]));
    }

    public interface Entry {
        void execute(FunctionManager functionManagerIn, ICommandSender sender, java.util.ArrayDeque<?> commandQueue, int maxCommandChainLength);
    }

    /** One command line. */
    public static class CommandEntry implements Entry {
        private final String command;

        public CommandEntry(String commandIn) {
            this.command = commandIn;
        }

        @Override
        public void execute(FunctionManager functionManagerIn, ICommandSender sender, java.util.ArrayDeque<?> commandQueue, int maxCommandChainLength) {
            functionManagerIn.mcc$runCommand(sender, command);
        }

        @Override
        public String toString() {
            return "/" + command;
        }
    }

    /** A call of another function. */
    public static class FunctionEntry implements Entry {
        private final CacheableFunction function;

        public FunctionEntry(FunctionObject functionIn) {
            this.function = new CacheableFunction(functionIn);
        }

        FunctionEntry(CacheableFunction functionIn) {
            this.function = functionIn;
        }

        @Override
        public void execute(FunctionManager functionManagerIn, ICommandSender sender, java.util.ArrayDeque<?> commandQueue, int maxCommandChainLength) {
            FunctionObject f = function.get(functionManagerIn);
            if (f != null) functionManagerIn.execute(f, sender);
        }

        @Override
        public String toString() {
            return "/function " + function.getId();
        }
    }

    /** A function named by id, looked up when first run. */
    public static class CacheableFunction {
        public static final CacheableFunction EMPTY = new CacheableFunction((ResourceLocation) null);
        private final ResourceLocation id;
        private boolean isValid;
        private FunctionObject function;

        public CacheableFunction(ResourceLocation idIn) {
            this.id = idIn;
        }

        public CacheableFunction(FunctionObject functionIn) {
            this.id = null;
            this.function = functionIn;
        }

        public FunctionObject get(FunctionManager functionManagerIn) {
            if (!isValid) {
                if (id != null) function = functionManagerIn.getFunction(id);
                isValid = true;
            }
            return function;
        }

        public String toString() {
            return String.valueOf(id);
        }

        public ResourceLocation getId() {
            return id;
        }
    }
}
