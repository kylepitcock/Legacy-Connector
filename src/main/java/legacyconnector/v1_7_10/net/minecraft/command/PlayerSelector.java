package legacyconnector.v1_7_10.net.minecraft.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The 1.7.10 player selectors {@code @p}, {@code @a} and {@code @r} with their arguments: positional {@code x,y,z,r},
 * or {@code x= y= z= r= rm= m= c= l= lm= team= name= score_<objective>= score_<objective>_min=}. Selectors use the old
 * syntax mods pass (not the modern {@code distance=..10}), so they are evaluated here over the server's players.
 */
public class PlayerSelector {
    private static final Pattern tokenPattern = Pattern.compile("^@([parf])(?:\\[([\\w=,!-]*)\\])?$");
    private static final Pattern intListPattern = Pattern.compile("\\G([-!]?[\\w-]*)(?:$|,)");
    private static final Pattern keyValueListPattern = Pattern.compile("\\G(\\w+)=([-!]?[\\w-]*)(?:$|,)");

    /** The one player the selector names, or null (no match, several, or not a selector). */
    public static EntityPlayerMP matchOnePlayer(ICommandSender sender, String token) {
        EntityPlayerMP[] players = matchPlayers(sender, token);
        return players != null && players.length == 1 ? players[0] : null;
    }

    /** The matched players' names joined as a chat component, or null when nothing matches. */
    public static IChatComponent func_150869_b(ICommandSender sender, String token) {
        EntityPlayerMP[] players = matchPlayers(sender, token);
        if (players == null || players.length == 0) return null;
        IChatComponent[] names = new IChatComponent[players.length];
        for (int i = 0; i < players.length; i++) names[i] = players[i].getFormattedCommandSenderName();
        return CommandBase.joinNiceString(names);
    }

    /** The players the selector matches, or null when the token is not a player selector. */
    public static EntityPlayerMP[] matchPlayers(ICommandSender sender, String token) {
        Matcher matcher = tokenPattern.matcher(token);
        if (!matcher.matches()) return null;
        Map<String, String> args = getArgumentMap(matcher.group(2));
        String type = matcher.group(1);
        int minRange = 0;
        int maxRange = 0;
        int minLevel = 0;
        int maxLevel = Integer.MAX_VALUE;
        int count = "a".equals(type) ? 0 : 1;
        int gameMode = -1;
        ChunkCoordinates sc = sender.getPlayerCoordinates();
        ChunkCoordinates coords = sc == null ? null : new ChunkCoordinates(sc);
        Map scores = func_96560_a(args);
        String name = null;
        String team = null;
        boolean sameWorld = false;
        if (args.containsKey("rm")) {
            minRange = parse(args.get("rm"), minRange);
            sameWorld = true;
        }
        if (args.containsKey("r")) {
            maxRange = parse(args.get("r"), maxRange);
            sameWorld = true;
        }
        if (args.containsKey("lm")) minLevel = parse(args.get("lm"), minLevel);
        if (args.containsKey("l")) maxLevel = parse(args.get("l"), maxLevel);
        if (coords != null) {
            if (args.containsKey("x")) {
                coords.posX = parse(args.get("x"), coords.posX);
                sameWorld = true;
            }
            if (args.containsKey("y")) {
                coords.posY = parse(args.get("y"), coords.posY);
                sameWorld = true;
            }
            if (args.containsKey("z")) {
                coords.posZ = parse(args.get("z"), coords.posZ);
                sameWorld = true;
            }
        }
        if (args.containsKey("m")) gameMode = parse(args.get("m"), gameMode);
        if (args.containsKey("c")) count = parse(args.get("c"), count);
        if (args.containsKey("team")) team = args.get("team");
        if (args.containsKey("name")) name = args.get("name");
        World world = sameWorld ? sender.getEntityWorld() : null;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return new EntityPlayerMP[0];
        List list;
        if ("p".equals(type) || "a".equals(type)) {
            list = server.getConfigurationManager().findPlayers(coords, minRange, maxRange, count, gameMode, minLevel, maxLevel, scores, name, team, world);
        } else if ("r".equals(type)) {
            list = new ArrayList(server.getConfigurationManager().findPlayers(coords, minRange, maxRange, 0, gameMode, minLevel, maxLevel, scores, name, team, world));
            Collections.shuffle(list);
            list = list.subList(0, Math.min(count, list.size()));
        } else {
            return null;
        }
        return (EntityPlayerMP[]) list.toArray(new EntityPlayerMP[0]);
    }

    /** The {@code score_} arguments as objective (with an optional {@code _min} suffix) -> value. */
    public static Map func_96560_a(Map args) {
        Map<String, Integer> scores = new HashMap<>();
        for (Object key : args.keySet()) {
            String s = (String) key;
            if (s.startsWith("score_") && s.length() > "score_".length()) {
                scores.put(s.substring("score_".length()), parse((String) args.get(s), 1));
            }
        }
        return scores;
    }

    /** Whether the token is a selector that can match several players ({@code @a}, or a count other than one). */
    public static boolean matchesMultiplePlayers(String token) {
        Matcher matcher = tokenPattern.matcher(token);
        if (!matcher.matches()) return false;
        Map<String, String> args = getArgumentMap(matcher.group(2));
        String type = matcher.group(1);
        int count = "a".equals(type) ? 0 : 1;
        if (args.containsKey("c")) count = parse(args.get("c"), count);
        return count != 1;
    }

    /** Whether the token is a selector of the given type ({@code "@p"}, {@code "@a"}, ...). */
    public static boolean hasTheseArguments(String token, String type) {
        Matcher matcher = tokenPattern.matcher(token);
        if (!matcher.matches()) return false;
        String found = matcher.group(1);
        return type == null || type.equals(found);
    }

    /** Whether the token is any selector. */
    public static boolean hasArguments(String token) {
        return hasTheseArguments(token, null);
    }

    private static int parse(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Map<String, String> getArgumentMap(String args) {
        Map<String, String> map = new HashMap<>();
        if (args == null) return map;
        Matcher matcher = intListPattern.matcher(args);
        int index = 0;
        int end = -1;
        while (index < 4 && matcher.find()) {
            String key = switch (index++) {
                case 0 -> "x";
                case 1 -> "y";
                case 2 -> "z";
                case 3 -> "r";
                default -> null;
            };
            map.put(key, matcher.group(1));
            end = matcher.end();
        }
        if (end < args.length()) {
            Matcher kv = keyValueListPattern.matcher(end == -1 ? args : args.substring(end));
            while (kv.find()) map.put(kv.group(1), kv.group(2));
        }
        return map;
    }
}
