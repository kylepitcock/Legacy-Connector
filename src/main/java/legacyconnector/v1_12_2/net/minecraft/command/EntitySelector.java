package legacyconnector.v1_12_2.net.minecraft.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;

/**
 * Target selectors ({@code @p}, {@code @a}, {@code @r}, {@code @e}, {@code @s} with arguments), resolved by the modern selector parser from
 * the sender's world, position and entity. 1.12's arguments ({@code r}, {@code rm}, {@code c}, {@code m}, {@code l}, {@code lm},
 * {@code type=Zombie}, ...) are translated to the modern ones first.
 */
public class EntitySelector {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^@([pares])(?:\\[([^ ]*)\\])?$");

    private EntitySelector() {
    }

    public static boolean isSelector(String selectorStr) {
        return selectorStr != null && TOKEN_PATTERN.matcher(selectorStr).matches();
    }

    /** Whether the selector can name more than one player ({@code @a}, {@code @e}, or a count other than 1). */
    public static boolean matchesMultiplePlayers(String selectorStr) {
        Matcher m = TOKEN_PATTERN.matcher(selectorStr == null ? "" : selectorStr);
        if (!m.matches()) return false;
        Map<String, String> args = arguments(m.group(2));
        String type = m.group(1);
        int count = !"a".equals(type) && !"e".equals(type) ? 1 : 0;
        try {
            if (args.containsKey("c")) count = Integer.parseInt(args.get("c"));
        } catch (NumberFormatException e) {
            // the default
        }
        return count != 1;
    }

    public static boolean hasArguments(String selectorStr) {
        Matcher m = TOKEN_PATTERN.matcher(selectorStr == null ? "" : selectorStr);
        return m.matches() && m.group(2) != null;
    }

    public static EntityPlayerMP matchOnePlayer(ICommandSender sender, String token) throws CommandException {
        return matchOneEntity(sender, token, EntityPlayerMP.class);
    }

    public static List<EntityPlayerMP> getPlayers(ICommandSender sender, String token) throws CommandException {
        return matchEntities(sender, token, EntityPlayerMP.class);
    }

    public static <T extends Entity> T matchOneEntity(ICommandSender sender, String token, Class<? extends T> targetClass) throws CommandException {
        List<T> list = matchEntities(sender, token, targetClass);
        return list.size() == 1 ? list.get(0) : null;
    }

    public static ITextComponent matchEntitiesToTextComponent(ICommandSender sender, String token) throws CommandException {
        List<Entity> list = matchEntities(sender, token, Entity.class);
        if (list.isEmpty()) return null;
        List<ITextComponent> names = new ArrayList<>();
        for (Entity e : list) names.add(e.getDisplayName());
        return CommandBase.join(names);
    }

    /** The entities of that class the selector names, as the sender sees them (none when the sender may not use selectors). */
    @SuppressWarnings("unchecked")
    public static <T extends Entity> List<T> matchEntities(ICommandSender sender, String token, Class<? extends T> targetClass) throws CommandException {
        Matcher m = TOKEN_PATTERN.matcher(token == null ? "" : token);
        if (!m.matches() || sender == null || !sender.canUseCommand(1, "@")) return Collections.emptyList();
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return Collections.emptyList();
        var source = server.createCommandSourceStack().withPermission(2);
        var world = sender.getEntityWorld();
        if (world != null && world.mcc$level() instanceof net.minecraft.server.level.ServerLevel level) source = source.withLevel(level);
        var pos = sender.getPositionVector();
        if (pos != null) source = source.withPosition(new net.minecraft.world.phys.Vec3(pos.x, pos.y, pos.z));
        var self = sender.getCommandSenderEntity();
        if (self != null && self.mcc$modern() != null) source = source.withEntity(self.mcc$modern());
        List<? extends net.minecraft.world.entity.Entity> found;
        try {
            var selector = new net.minecraft.commands.arguments.selector.EntitySelectorParser(
                    new com.mojang.brigadier.StringReader(modernSelector(m.group(1), arguments(m.group(2)))), true).parse();
            found = selector.findEntities(source);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new CommandException("commands.generic.syntax");
        }
        List<T> out = new ArrayList<>();
        for (var e : found) {
            Entity view = EntityBridge1122.wrap(e);
            if (view != null && (targetClass == null || targetClass.isInstance(view))) out.add((T) view);
        }
        return out;
    }

    private static Map<String, String> arguments(String args) {
        Map<String, String> out = new java.util.LinkedHashMap<>();
        if (args == null || args.isEmpty()) return out;
        String[] positional = {"x", "y", "z", "r"};
        int index = 0;
        for (String part : args.split(",")) {
            int eq = part.indexOf('=');
            if (eq < 0) {
                if (index < positional.length && !part.isEmpty()) out.put(positional[index], part);
                index++;
            } else {
                out.put(part.substring(0, eq), part.substring(eq + 1));
            }
        }
        return out;
    }

    /** The modern selector for a 1.12 selector type and arguments. */
    private static String modernSelector(String type, Map<String, String> args) {
        List<String> out = new ArrayList<>();
        String r = args.get("r");
        String rm = args.get("rm");
        if (r != null || rm != null) out.add("distance=" + (rm == null ? "" : rm) + ".." + (r == null ? "" : r));
        String l = args.get("l");
        String lm = args.get("lm");
        if (l != null || lm != null) out.add("level=" + (lm == null ? "" : lm) + ".." + (l == null ? "" : l));
        for (String k : new String[] {"x", "y", "z", "dx", "dy", "dz", "name", "tag", "team"}) {
            if (args.containsKey(k)) out.add(k + "=" + args.get(k));
        }
        String mode = args.get("m");
        if (mode != null) {
            boolean not = mode.startsWith("!");
            String v = not ? mode.substring(1) : mode;
            String name = switch (v) {
                case "0", "s", "survival" -> "survival";
                case "1", "c", "creative" -> "creative";
                case "2", "a", "adventure" -> "adventure";
                case "3", "sp", "spectator" -> "spectator";
                default -> null;
            };
            if (name != null) out.add("gamemode=" + (not ? "!" : "") + name);
        }
        String entityType = args.get("type");
        if (entityType != null) {
            boolean not = entityType.startsWith("!");
            String v = not ? entityType.substring(1) : entityType;
            String fixed = legacyconnector.v1_12_2.net.minecraft.util.datafix.fixes.EntityId.OLD_TO_NEW_ID_MAP.get(v);
            if (fixed != null) v = fixed;
            v = v.toLowerCase(java.util.Locale.ROOT);
            if (!v.contains(":")) v = "minecraft:" + v;
            out.add("type=" + (not ? "!" : "") + v);
        }
        String c = args.get("c");
        String t = "r".equals(type) ? "r" : type;
        if (c != null) {
            try {
                int count = Integer.parseInt(c);
                if (count != 0) {
                    out.add("limit=" + Math.abs(count));
                    if (!"r".equals(type)) out.add("sort=" + (count < 0 ? "furthest" : "nearest"));
                }
            } catch (NumberFormatException e) {
                // ignored, as 1.12 did
            }
        }
        if ("r".equals(type) && entityType != null) {
            // 1.12's @r with a type picks any entity of that type; modern @r is players only
            t = "e";
            out.add("sort=random");
            if (c == null) out.add("limit=1");
        }
        return "@" + t + (out.isEmpty() ? "" : "[" + String.join(",", out) + "]");
    }
}
