package legacyconnector.v1_12_2.net.minecraft.command;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;

/**
 * The scoreboard targets a command sender's results go to (1.12's {@code /stats}): per result type an entity name and an objective; a
 * result is written as that entity's score (on the modern server's scoreboard).
 */
public class CommandResultStats {
    private static final int NUM_RESULT_TYPES = Type.values().length;
    private static final String[] STRING_RESULT_TYPES = new String[NUM_RESULT_TYPES];
    private String[] entitiesID = STRING_RESULT_TYPES;
    private String[] objectives = STRING_RESULT_TYPES;

    public void setCommandStatForSender(MinecraftServer server, ICommandSender sender, Type typeIn, int p_184932_4_) {
        String entity = entitiesID[typeIn.getTypeID()];
        String objectiveName = objectives[typeIn.getTypeID()];
        var m = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (entity == null || objectiveName == null || m == null) return;
        if (entity.startsWith("@")) {
            // a selector: the entities it names, from the sender's position
            try {
                var source = m.createCommandSourceStack();
                var selector = new net.minecraft.commands.arguments.selector.EntitySelectorParser(
                        new com.mojang.brigadier.StringReader(entity), true).parse();
                var found = selector.findEntities(source);
                if (found.size() != 1) return;
                entity = found.get(0).getScoreboardName();
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException | RuntimeException e) {
                return;
            }
        }
        var scoreboard = m.getScoreboard();
        var objective = scoreboard.getObjective(objectiveName);
        if (objective == null) return;
        scoreboard.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly(entity), objective).set(p_184932_4_);
    }

    public void readStatsFromNBT(NBTTagCompound tagcompound) {
        if (!tagcompound.hasKey("CommandStats", 10)) return;
        NBTTagCompound stats = tagcompound.getCompoundTag("CommandStats");
        for (Type type : Type.values()) {
            String nameKey = type.getTypeName() + "Name";
            String objectiveKey = type.getTypeName() + "Objective";
            if (stats.hasKey(nameKey, 8) && stats.hasKey(objectiveKey, 8)) {
                setScoreBoardStat(this, type, stats.getString(nameKey), stats.getString(objectiveKey));
            }
        }
    }

    public void writeStatsToNBT(NBTTagCompound tagcompound) {
        NBTTagCompound stats = new NBTTagCompound();
        for (Type type : Type.values()) {
            String entity = entitiesID[type.getTypeID()];
            String objective = objectives[type.getTypeID()];
            if (entity != null && objective != null) {
                stats.setString(type.getTypeName() + "Name", entity);
                stats.setString(type.getTypeName() + "Objective", objective);
            }
        }
        if (!stats.hasNoTags()) tagcompound.setTag("CommandStats", stats);
    }

    public static void setScoreBoardStat(CommandResultStats stats, Type resultType, String entityID, String objectiveName) {
        if (entityID != null && !entityID.isEmpty() && objectiveName != null && !objectiveName.isEmpty()) {
            if (stats.entitiesID == STRING_RESULT_TYPES || stats.objectives == STRING_RESULT_TYPES) {
                stats.entitiesID = new String[NUM_RESULT_TYPES];
                stats.objectives = new String[NUM_RESULT_TYPES];
            }
            stats.entitiesID[resultType.getTypeID()] = entityID;
            stats.objectives[resultType.getTypeID()] = objectiveName;
        } else {
            removeScoreBoardStat(stats, resultType);
        }
    }

    private static void removeScoreBoardStat(CommandResultStats stats, Type resultType) {
        if (stats.entitiesID == STRING_RESULT_TYPES || stats.objectives == STRING_RESULT_TYPES) return;
        stats.entitiesID[resultType.getTypeID()] = null;
        stats.objectives[resultType.getTypeID()] = null;
        boolean empty = true;
        for (Type type : Type.values()) {
            if (stats.entitiesID[type.getTypeID()] != null && stats.objectives[type.getTypeID()] != null) {
                empty = false;
                break;
            }
        }
        if (empty) {
            stats.entitiesID = STRING_RESULT_TYPES;
            stats.objectives = STRING_RESULT_TYPES;
        }
    }

    public void addAllStats(CommandResultStats resultStatsIn) {
        for (Type type : Type.values()) {
            setScoreBoardStat(this, type, resultStatsIn.entitiesID[type.getTypeID()], resultStatsIn.objectives[type.getTypeID()]);
        }
    }

    public enum Type {
        SUCCESS_COUNT(0, "SuccessCount"),
        AFFECTED_BLOCKS(1, "AffectedBlocks"),
        AFFECTED_ENTITIES(2, "AffectedEntities"),
        AFFECTED_ITEMS(3, "AffectedItems"),
        QUERY_RESULT(4, "QueryResult");

        private final int typeID;
        private final String typeName;

        Type(int id, String name) {
            this.typeID = id;
            this.typeName = name;
        }

        public int getTypeID() {
            return typeID;
        }

        public String getTypeName() {
            return typeName;
        }

        public static String[] getTypeNames() {
            String[] names = new String[values().length];
            int i = 0;
            for (Type type : values()) names[i++] = type.getTypeName();
            return names;
        }

        public static Type getTypeByName(String name) {
            for (Type type : values()) if (type.getTypeName().equals(name)) return type;
            return null;
        }
    }
}
