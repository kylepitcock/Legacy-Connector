package legacyconnector.v1_12_2.net.minecraft.command;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;

/**
 * The base class of mods' commands: permission level 4 by default, argument parsing, player and entity lookups (names, UUIDs
 * and the selectors {@code @p @s @r @a}) on the modern server, and feedback to the sender.
 */
public abstract class CommandBase implements ICommand {
    protected static final Splitter COMMA_SPLITTER = Splitter.on(',');
    protected static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);
    private static ICommandListener commandListener;

    public CommandBase() {
    }

    public int getRequiredPermissionLevel() {
        return 4;
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(getRequiredPermissionLevel(), getName());
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(ICommand other) {
        return String.valueOf(getName()).compareTo(String.valueOf(other.getName()));
    }

    // ------------------------------------------------------------------ numbers

    public static int parseInt(String input) throws NumberInvalidException {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new NumberInvalidException("commands.generic.num.invalid", input);
        }
    }

    public static int parseInt(String input, int min) throws NumberInvalidException {
        return parseInt(input, min, Integer.MAX_VALUE);
    }

    public static int parseInt(String input, int min, int max) throws NumberInvalidException {
        int i = parseInt(input);
        if (i < min) throw new NumberInvalidException("commands.generic.num.tooSmall", i, min);
        if (i > max) throw new NumberInvalidException("commands.generic.num.tooBig", i, max);
        return i;
    }

    public static long parseLong(String input) throws NumberInvalidException {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            throw new NumberInvalidException("commands.generic.num.invalid", input);
        }
    }

    public static long parseLong(String input, long min, long max) throws NumberInvalidException {
        long l = parseLong(input);
        if (l < min) throw new NumberInvalidException("commands.generic.num.tooSmall", l, min);
        if (l > max) throw new NumberInvalidException("commands.generic.num.tooBig", l, max);
        return l;
    }

    public static double parseDouble(String input) throws NumberInvalidException {
        try {
            double d = Double.parseDouble(input);
            if (!Double.isFinite(d)) throw new NumberInvalidException("commands.generic.num.invalid", input);
            return d;
        } catch (NumberFormatException e) {
            throw new NumberInvalidException("commands.generic.num.invalid", input);
        }
    }

    public static double parseDouble(String input, double min) throws NumberInvalidException {
        return parseDouble(input, min, Double.MAX_VALUE);
    }

    public static double parseDouble(String input, double min, double max) throws NumberInvalidException {
        double d = parseDouble(input);
        if (d < min) throw new NumberInvalidException("commands.generic.double.tooSmall", String.format("%.2f", d), min);
        if (d > max) throw new NumberInvalidException("commands.generic.double.tooBig", String.format("%.2f", d), max);
        return d;
    }

    public static boolean parseBoolean(String input) throws CommandException {
        if ("true".equals(input) || "1".equals(input)) return true;
        if ("false".equals(input) || "0".equals(input)) return false;
        throw new CommandException("commands.generic.boolean.invalid", input);
    }

    public static BlockPos parseBlockPos(ICommandSender sender, String[] args, int startIndex, boolean centerBlock) throws NumberInvalidException {
        BlockPos pos = sender.getPosition();
        return new BlockPos(parseDouble(pos.getX(), args[startIndex], -30000000, 30000000, centerBlock),
                parseDouble(pos.getY(), args[startIndex + 1], 0, 256, false),
                parseDouble(pos.getZ(), args[startIndex + 2], -30000000, 30000000, centerBlock));
    }

    public static double parseDouble(double base, String input, boolean centerBlock) throws NumberInvalidException {
        return parseDouble(base, input, -30000000, 30000000, centerBlock);
    }

    public static double parseDouble(double base, String input, int min, int max, boolean centerBlock) throws NumberInvalidException {
        boolean relative = input.startsWith("~");
        if (relative && Double.isNaN(base)) throw new NumberInvalidException("commands.generic.num.invalid", base);
        double d = relative ? base : 0.0D;
        if (!relative || input.length() > 1) {
            boolean decimal = input.contains(".");
            if (relative) input = input.substring(1);
            d += parseDouble(input);
            if (!decimal && !relative && centerBlock) d += 0.5D;
        }
        if (min != 0 || max != 0) {
            if (d < min) throw new NumberInvalidException("commands.generic.double.tooSmall", String.format("%.2f", d), min);
            if (d > max) throw new NumberInvalidException("commands.generic.double.tooBig", String.format("%.2f", d), max);
        }
        return d;
    }

    public static CoordinateArg parseCoordinate(double base, String selectorArg, boolean centerBlock) throws NumberInvalidException {
        return parseCoordinate(base, selectorArg, -30000000, 30000000, centerBlock);
    }

    public static CoordinateArg parseCoordinate(double base, String selectorArg, int min, int max, boolean centerBlock) throws NumberInvalidException {
        boolean relative = selectorArg.startsWith("~");
        if (relative && Double.isNaN(base)) throw new NumberInvalidException("commands.generic.num.invalid", base);
        double d = 0.0D;
        if (!relative || selectorArg.length() > 1) {
            boolean decimal = selectorArg.contains(".");
            if (relative) selectorArg = selectorArg.substring(1);
            d += parseDouble(selectorArg);
            if (!decimal && !relative && centerBlock) d += 0.5D;
        }
        double result = d + (relative ? base : 0.0D);
        if (min != 0 || max != 0) {
            if (result < min) throw new NumberInvalidException("commands.generic.double.tooSmall", String.format("%.2f", result), min);
            if (result > max) throw new NumberInvalidException("commands.generic.double.tooBig", String.format("%.2f", result), max);
        }
        return new CoordinateArg(result, d, relative);
    }

    // ------------------------------------------------------------------ players and entities

    private static net.minecraft.server.MinecraftServer modernServer() {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    private static EntityPlayerMP playerNamed(String name) {
        var server = modernServer();
        if (server == null || name == null) return null;
        var p = server.getPlayerList().getPlayerByName(name);
        if (p == null) {
            try {
                p = server.getPlayerList().getPlayer(java.util.UUID.fromString(name));
            } catch (IllegalArgumentException e) {
                // not a UUID
            }
        }
        return p != null && EntityBridge1122.wrap(p) instanceof EntityPlayerMP mp ? mp : null;
    }

    private static List<EntityPlayerMP> allPlayers() {
        List<EntityPlayerMP> out = new ArrayList<>();
        var server = modernServer();
        if (server == null) return out;
        for (var p : server.getPlayerList().getPlayers()) if (EntityBridge1122.wrap(p) instanceof EntityPlayerMP mp) out.add(mp);
        return out;
    }

    private static EntityPlayerMP senderPlayer(ICommandSender sender) {
        if (sender instanceof EntityPlayerMP p) return p;
        return sender != null && sender.getCommandSenderEntity() instanceof EntityPlayerMP p ? p : null;
    }

    public static EntityPlayerMP getCommandSenderAsPlayer(ICommandSender sender) throws PlayerNotFoundException {
        EntityPlayerMP p = senderPlayer(sender);
        if (p == null) throw new PlayerNotFoundException("commands.generic.player.unspecified");
        return p;
    }

    /** A player named by an argument: a name, a UUID, or the selectors @p, @s and @r. */
    public static EntityPlayerMP getPlayer(MinecraftServer server, ICommandSender sender, String target) throws CommandException {
        EntityPlayerMP p = null;
        if ("@s".equals(target) || "@p".equals(target)) p = senderPlayer(sender);
        if (p == null && ("@p".equals(target) || "@r".equals(target))) {
            List<EntityPlayerMP> all = allPlayers();
            if (!all.isEmpty()) p = all.get("@r".equals(target) ? java.util.concurrent.ThreadLocalRandom.current().nextInt(all.size()) : 0);
        }
        if (p == null) p = playerNamed(target);
        if (p == null) throw new PlayerNotFoundException("commands.generic.player.notFound", target);
        return p;
    }

    public static List<EntityPlayerMP> getPlayers(MinecraftServer server, ICommandSender sender, String target) throws CommandException {
        if ("@a".equals(target)) return allPlayers();
        List<EntityPlayerMP> one = new ArrayList<>();
        one.add(getPlayer(server, sender, target));
        return one;
    }

    public static Entity getEntity(MinecraftServer server, ICommandSender sender, String target) throws CommandException {
        return getEntity(server, sender, target, Entity.class);
    }

    public static <T extends Entity> T getEntity(MinecraftServer server, ICommandSender sender, String target, Class<? extends T> targetClass)
            throws CommandException {
        Entity found = null;
        if ("@s".equals(target) && sender != null) found = sender.getCommandSenderEntity();
        if (found == null && ("@p".equals(target) || "@r".equals(target))) {
            try {
                found = getPlayer(server, sender, target);
            } catch (CommandException e) {
                // below
            }
        }
        if (found == null) found = playerNamed(target);
        if (found == null) {
            var modern = modernServer();
            java.util.UUID id = null;
            try {
                id = java.util.UUID.fromString(target);
            } catch (IllegalArgumentException | NullPointerException e) {
                // not a UUID
            }
            if (modern != null && id != null) {
                for (var level : modern.getAllLevels()) {
                    var e = level.getEntity(id);
                    if (e != null) {
                        found = EntityBridge1122.wrap(e);
                        break;
                    }
                }
            }
        }
        if (found != null && targetClass != null && !targetClass.isInstance(found)) found = null;
        if (found == null) throw new EntityNotFoundException(target);
        @SuppressWarnings("unchecked")
        T t = (T) found;
        return t;
    }

    public static List<Entity> getEntityList(MinecraftServer server, ICommandSender sender, String target) throws CommandException {
        if ("@a".equals(target)) return new ArrayList<>(allPlayers());
        if ("@e".equals(target)) {
            List<Entity> out = new ArrayList<>();
            var level = sender == null || sender.getEntityWorld() == null ? null : sender.getEntityWorld().mcc$level();
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                for (var e : sl.getAllEntities()) {
                    Entity view = EntityBridge1122.wrap(e);
                    if (view != null) out.add(view);
                }
            }
            return out;
        }
        List<Entity> one = new ArrayList<>();
        one.add(getEntity(server, sender, target));
        return one;
    }

    public static String getPlayerName(MinecraftServer server, ICommandSender sender, String query) throws CommandException {
        try {
            return getPlayer(server, sender, query).getName();
        } catch (PlayerNotFoundException e) {
            if (query == null || query.startsWith("@")) throw e;
            return query;
        }
    }

    public static String getEntityName(MinecraftServer server, ICommandSender sender, String query) throws CommandException {
        try {
            return getPlayer(server, sender, query).getName();
        } catch (PlayerNotFoundException e) {
            try {
                return getEntity(server, sender, query).getUniqueID().toString();
            } catch (EntityNotFoundException e2) {
                if (query == null || query.startsWith("@")) throw e2;
                return query;
            }
        }
    }

    /** The entity's saved data (the modern entity's, which mods read for ids and positions). */
    public static NBTTagCompound entityToNBT(Entity entity) {
        net.minecraft.world.entity.Entity modern = entity == null ? null : entity.mcc$modern();
        if (modern == null) return new NBTTagCompound();
        net.minecraft.nbt.CompoundTag tag = modern.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        return new NBTTagCompound(tag);
    }

    // ------------------------------------------------------------------ text

    public static ITextComponent getChatComponentFromNthArg(ICommandSender sender, String[] args, int index) throws CommandException {
        return getChatComponentFromNthArg(sender, args, index, false);
    }

    /** The arguments from {@code index} on as text (1.12 also expanded selectors when asked; names are kept as typed here). */
    public static ITextComponent getChatComponentFromNthArg(ICommandSender sender, String[] args, int index, boolean selectors) throws CommandException {
        return new TextComponentString(buildString(args, index));
    }

    public static String buildString(String[] args, int index) {
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(0, index); args != null && i < args.length; i++) {
            if (i > index) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    public static Item getItemByText(ICommandSender sender, String id) throws NumberInvalidException {
        Item item = Item.getByNameOrId(id);
        if (item == null) throw new NumberInvalidException("commands.give.item.notFound", id);
        return item;
    }

    public static Block getBlockByText(ICommandSender sender, String id) throws NumberInvalidException {
        Block block = Block.getBlockFromName(id);
        if (block == null) throw new NumberInvalidException("commands.give.block.notFound", id);
        return block;
    }

    /** A state written as a metadata number, {@code default}, {@code *} or {@code property=value,...}. */
    @SuppressWarnings("deprecation")
    public static IBlockState convertArgToBlockState(Block block, String blockStateArg) throws NumberInvalidException, InvalidBlockStateException {
        try {
            int meta = Integer.parseInt(blockStateArg);
            if (meta < 0) throw new NumberInvalidException("commands.generic.num.tooSmall", meta, 0);
            if (meta > 15) throw new NumberInvalidException("commands.generic.num.tooBig", meta, 15);
            return block.getStateFromMeta(meta);
        } catch (NumberFormatException e) {
            // property form
        }
        IBlockState state = block.getDefaultState();
        if ("default".equals(blockStateArg) || "*".equals(blockStateArg)) return state;
        for (String pair : COMMA_SPLITTER.split(blockStateArg)) {
            List<String> kv = EQUAL_SPLITTER.splitToList(pair);
            if (kv.size() != 2) throw new InvalidBlockStateException("commands.generic.blockstate.invalid", blockStateArg, Block.REGISTRY.getNameForObject(block));
            legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty<?> property = null;
            for (var p : state.getPropertyKeys()) if (p.getName().equals(kv.get(0))) property = p;
            if (property == null) throw new InvalidBlockStateException("commands.generic.blockstate.invalid", blockStateArg, Block.REGISTRY.getNameForObject(block));
            state = withValue(state, property, kv.get(1), blockStateArg, block);
        }
        return state;
    }

    private static <T extends Comparable<T>> IBlockState withValue(IBlockState state, legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty<T> property,
            String value, String arg, Block block) throws InvalidBlockStateException {
        com.google.common.base.Optional<T> parsed = property.parseValue(value);
        if (!parsed.isPresent()) throw new InvalidBlockStateException("commands.generic.blockstate.invalid", arg, Block.REGISTRY.getNameForObject(block));
        return state.withProperty(property, parsed.get());
    }

    public static com.google.common.base.Predicate<IBlockState> convertArgToBlockStatePredicate(Block block, String blockStateArg) throws InvalidBlockStateException {
        if ("*".equals(blockStateArg) || "-1".equals(blockStateArg)) return s -> s != null && s.getBlock() == block;
        IBlockState wanted;
        try {
            wanted = convertArgToBlockState(block, blockStateArg);
        } catch (NumberInvalidException e) {
            throw new InvalidBlockStateException("commands.generic.blockstate.invalid", blockStateArg, Block.REGISTRY.getNameForObject(block));
        }
        return s -> s != null && s.getBlock() == block && s.equals(wanted);
    }

    public static String joinNiceString(Object[] elements) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            String s = String.valueOf(elements[i]);
            if (i > 0) sb.append(i == elements.length - 1 ? " and " : ", ");
            sb.append(s);
        }
        return sb.toString();
    }

    public static ITextComponent join(List<ITextComponent> components) {
        ITextComponent out = new TextComponentString("");
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) out.appendText(i == components.size() - 1 ? " and " : ", ");
            out.appendSibling(components.get(i));
        }
        return out;
    }

    public static String joinNiceStringFromCollection(Collection<String> strings) {
        return joinNiceString(strings.toArray(new String[0]));
    }

    public static List<String> getTabCompletionCoordinate(String[] args, int index, BlockPos pos) {
        if (pos == null) return List.of("~");
        int i = args.length - 1;
        String s;
        if (i == index) s = Integer.toString(pos.getX());
        else if (i == index + 1) s = Integer.toString(pos.getY());
        else if (i == index + 2) s = Integer.toString(pos.getZ());
        else return Collections.emptyList();
        return List.of(s);
    }

    public static List<String> getTabCompletionCoordinateXZ(String[] args, int index, BlockPos pos) {
        if (pos == null) return List.of("~");
        int i = args.length - 1;
        String s;
        if (i == index) s = Integer.toString(pos.getX());
        else if (i == index + 1) s = Integer.toString(pos.getZ());
        else return Collections.emptyList();
        return List.of(s);
    }

    public static boolean doesStringStartWith(String original, String region) {
        return region.regionMatches(true, 0, original, 0, original.length());
    }

    public static List<String> getListOfStringsMatchingLastWord(String[] args, String... possibilities) {
        return getListOfStringsMatchingLastWord(args, java.util.Arrays.asList(possibilities));
    }

    /** The options that start with the last argument (or, for resource locations, whose path does). */
    public static List<String> getListOfStringsMatchingLastWord(String[] inputArgs, Collection<?> possibleCompletions) {
        String last = inputArgs == null || inputArgs.length == 0 ? "" : inputArgs[inputArgs.length - 1];
        List<String> out = new ArrayList<>();
        if (possibleCompletions == null) return out;
        for (Object o : possibleCompletions) {
            if (o instanceof legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation rl) {
                if (doesStringStartWith(last, rl.getPath())) out.add(String.valueOf(o));
            } else if (doesStringStartWith(last, String.valueOf(o))) {
                out.add(String.valueOf(o));
            }
        }
        if (out.isEmpty()) {
            for (Object o : possibleCompletions) {
                if (o instanceof legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation rl && doesStringStartWith(last, rl.getNamespace())) {
                    out.add(String.valueOf(rl));
                }
            }
        }
        return out;
    }

    public static SyntaxErrorException toSyntaxException(com.google.gson.JsonParseException e) {
        Throwable cause = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e);
        String message = cause == null ? "" : cause.getMessage();
        if (message != null && message.contains("setLenient")) message = message.substring(message.indexOf("to accept ") + 10);
        return new SyntaxErrorException("commands.tellraw.jsonException", message);
    }

    // ------------------------------------------------------------------ feedback

    public static void notifyCommandListener(ICommandSender sender, ICommand command, String translationKey, Object... translationArgs) {
        notifyCommandListener(sender, command, 0, translationKey, translationArgs);
    }

    /** The command's feedback: to the listener a mod set, else to the sender itself (flag 1 hides it from the sender). */
    public static void notifyCommandListener(ICommandSender sender, ICommand command, int flags, String translationKey, Object... translationArgs) {
        ICommandListener listener = commandListener;
        if (listener != null) {
            listener.notifyListener(sender, command, flags, translationKey, translationArgs);
            return;
        }
        if (sender != null && (flags & 1) != 1) sender.sendMessage(new TextComponentTranslation(translationKey, translationArgs));
    }

    public static void setCommandListener(ICommandListener listener) {
        commandListener = listener;
    }

    /** A parsed coordinate: the result, the typed amount and whether it was relative ({@code ~}). */
    public static class CoordinateArg {
        private final double result;
        private final double amount;
        private final boolean isRelative;

        protected CoordinateArg(double resultIn, double amountIn, boolean relative) {
            this.result = resultIn;
            this.amount = amountIn;
            this.isRelative = relative;
        }

        public double getResult() {
            return result;
        }

        public double getAmount() {
            return amount;
        }

        public boolean isRelative() {
            return isRelative;
        }
    }
}
