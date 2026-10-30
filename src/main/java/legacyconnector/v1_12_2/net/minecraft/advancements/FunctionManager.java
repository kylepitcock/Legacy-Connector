package legacyconnector.v1_12_2.net.minecraft.advancements;

import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.command.FunctionObject;
import legacyconnector.v1_12_2.net.minecraft.command.ICommandManager;
import legacyconnector.v1_12_2.net.minecraft.command.ICommandSender;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** The server's functions: a view of the modern function manager (data pack functions); mods' own functions run their commands. */
public class FunctionManager {
    private final MinecraftServer server;

    public FunctionManager(java.io.File functionDirIn, MinecraftServer serverIn) {
        this.server = serverIn;
    }

    private static net.minecraft.server.MinecraftServer modern() {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    public FunctionObject getFunction(ResourceLocation id) {
        var m = modern();
        if (m == null || id == null) return null;
        var modernId = net.minecraft.resources.ResourceLocation.tryBuild(id.getNamespace(), id.getPath());
        if (modernId == null || m.getFunctions().get(modernId).isEmpty()) return null;
        return mcc$modernFunction(id);
    }

    private static FunctionObject mcc$modernFunction(ResourceLocation id) {
        return FunctionObject.mcc$ofModern(id);
    }

    public ICommandManager getCommandManager() {
        return server != null ? server.getCommandManager() : MinecraftServer.mcc$instance().getCommandManager();
    }

    public int getMaxCommandChainLength() {
        var m = modern();
        return m == null ? 65536 : m.getGameRules().getInt(net.minecraft.world.level.GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH);
    }

    public Map<ResourceLocation, FunctionObject> getFunctions() {
        Map<ResourceLocation, FunctionObject> out = new LinkedHashMap<>();
        var m = modern();
        if (m == null) return out;
        for (var id : m.getFunctions().getFunctionNames()) {
            ResourceLocation legacy = new ResourceLocation(id.getNamespace(), id.getPath());
            out.put(legacy, mcc$modernFunction(legacy));
        }
        return out;
    }

    /** Ticks are run by the modern function manager. */
    public void update() {
    }

    /** Runs the function as the sender; the number of commands run (1 for a data pack function, which runs as a whole). */
    public int execute(FunctionObject function, ICommandSender sender) {
        var m = modern();
        if (m == null || function == null) return 0;
        ResourceLocation id = function.mcc$id();
        if (id != null) {
            var f = m.getFunctions().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath()));
            if (f.isEmpty()) return 0;
            m.getFunctions().execute(f.get(), mcc$source(sender));
            return 1;
        }
        int n = 0;
        int max = getMaxCommandChainLength();
        for (FunctionObject.Entry e : function.getEntries()) {
            if (n++ >= max) break;
            e.execute(this, sender, new java.util.ArrayDeque<>(), max);
        }
        return n;
    }

    /** Runs one command line as the sender through the modern dispatcher. */
    public void mcc$runCommand(ICommandSender sender, String command) {
        var m = modern();
        if (m == null || command == null) return;
        m.getCommands().performPrefixedCommand(mcc$source(sender), command);
    }

    /** The modern command source for a sender: its world, position, entity and permission (functions run at level 2). */
    public static net.minecraft.commands.CommandSourceStack mcc$source(ICommandSender sender) {
        var m = modern();
        var source = m.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        if (sender == null) return source;
        var world = sender.getEntityWorld();
        if (world != null && world.mcc$level() instanceof net.minecraft.server.level.ServerLevel level) source = source.withLevel(level);
        var pos = sender.getPositionVector();
        if (pos != null) source = source.withPosition(new net.minecraft.world.phys.Vec3(pos.x, pos.y, pos.z));
        var entity = sender.getCommandSenderEntity();
        if (entity != null && entity.mcc$modern() != null) source = source.withEntity(entity.mcc$modern());
        return source;
    }

    /** Reloading is the modern server's ({@code /reload}). */
    public void reload() {
    }
}
