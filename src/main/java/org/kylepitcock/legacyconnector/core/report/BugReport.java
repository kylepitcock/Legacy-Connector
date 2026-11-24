package org.kylepitcock.legacyconnector.core.report;

import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.kylepitcock.legacyconnector.api.LegacyApi;
import org.kylepitcock.legacyconnector.api.LegacyMod;

/**
 * {@code /legacyconnector report}: writes {@code logs/legacyconnector-bundle-<time>.zip} (logs, Legacy Connector reports, a summary of the
 * versions and mod list) with personal data removed ({@link BugReportBundle}). In single player the local player may run it; on a server
 * only operators (permission level 2). The file system work is in {@link BugReportBundle}; this class gathers the game-side facts.
 */
public final class BugReport {
    private BugReport() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(Commands.literal("legacyconnector")
                .then(Commands.literal("report").requires(BugReport::allowed).executes(BugReport::run))));
    }

    private static boolean allowed(CommandSourceStack src) {
        if (src.hasPermission(2)) return true;
        if (src.getServer().isDedicatedServer()) return false;
        return src.getEntity() instanceof ServerPlayer p && src.getServer().isSingleplayerOwner(p.getGameProfile());
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        try {
            // Make the files as current as possible; both are rewritten again at exit.
            try {
                org.kylepitcock.legacyconnector.core.problems.LegacyProblems.writeReport();
                org.kylepitcock.legacyconnector.core.degraded.Degraded.writeFile("report command");
            } catch (RuntimeException ignored) {
                // the bundle still carries what is on disk
            }
            Path game = FMLPaths.GAMEDIR.get();
            Path logs = game.resolve("logs");
            List<Path> files = new ArrayList<>();
            files.add(logs.resolve("latest.log"));
            files.add(logs.resolve("debug.log"));
            files.add(logs.resolve("legacyconnector-report.txt"));
            files.add(logs.resolve("legacyconnector-degraded.txt"));
            Path crash = newestCrashReport(game.resolve("crash-reports"), ManagementFactory.getRuntimeMXBean().getStartTime());
            if (crash != null) files.add(crash);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path zip = BugReportBundle.write(logs, stamp, summary(game), files, BugReportBundle.Redactor.ofSystem(), BugReportBundle.DEFAULT_CAP);
            Component open = Component.translatable("legacyconnector.report.open_folder").withStyle(
                    Style.EMPTY.withUnderlined(true).withColor(net.minecraft.ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, logs.toAbsolutePath().toString())));
            src.sendSuccess(() -> Component.translatable("legacyconnector.report.written", zip.toAbsolutePath().toString()).append(" ").append(open), false);
            src.sendSuccess(() -> Component.translatable("legacyconnector.report.attach", zip.getFileName().toString()), false);
            return 1;
        } catch (IOException | RuntimeException ex) {
            src.sendFailure(Component.translatable("legacyconnector.report.failed", String.valueOf(ex)));
            return 0;
        }
    }

    /** The newest file in {@code crash-reports/} written since this JVM started, or null. */
    static Path newestCrashReport(Path dir, long sessionStartMillis) {
        if (!Files.isDirectory(dir)) return null;
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().endsWith(".txt")).filter(p -> {
                try {
                    return Files.getLastModifiedTime(p).toMillis() >= sessionStartMillis;
                } catch (IOException e) {
                    return false;
                }
            }).max((a, b) -> {
                try {
                    return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
                } catch (IOException e) {
                    return 0;
                }
            }).orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static String summary(Path game) {
        StringBuilder b = new StringBuilder();
        String nl = System.lineSeparator();
        b.append("Legacy Connector bug report bundle").append(nl);
        b.append("Legacy Connector: ").append(version("legacyconnector")).append(nl);
        b.append("Minecraft: ").append(SharedConstants.getCurrentVersion().getName()).append(nl);
        b.append("NeoForge: ").append(version("neoforge")).append(nl);
        b.append("Java: ").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append(')').append(nl);
        b.append("OS: ").append(System.getProperty("os.name")).append(' ').append(System.getProperty("os.version")).append(' ')
                .append(System.getProperty("os.arch")).append(nl);
        Runtime rt = Runtime.getRuntime();
        b.append("Memory: max ").append(rt.maxMemory() >> 20).append(" MB, committed ").append(rt.totalMemory() >> 20)
                .append(" MB, free in committed ").append(rt.freeMemory() >> 20).append(" MB").append(nl);
        b.append("JVM arguments: ").append(String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments())).append(nl);
        b.append(nl).append("Settings (config/legacyconnector.properties: versions.*, experimental.*, legacy.*, mods.*)").append(nl);
        TreeMap<String, String> toggles = new TreeMap<>();
        Path cfg = FMLPaths.CONFIGDIR.get().resolve("legacyconnector.properties");
        if (Files.isRegularFile(cfg)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(cfg)) {
                p.load(in);
            } catch (IOException | RuntimeException ignored) {
                // unreadable config: the list stays empty
            }
            for (String k : p.stringPropertyNames()) {
                if (k.startsWith("versions.") || k.startsWith("experimental.") || k.startsWith("legacy.") || k.startsWith("mods.")) {
                    toggles.put(k, p.getProperty(k).trim());
                }
            }
        }
        if (toggles.isEmpty()) b.append("(none set; defaults)").append(nl);
        toggles.forEach((k, v) -> b.append(k).append(" = ").append(v).append(nl));

        Set<String> legacyIds = new HashSet<>();
        List<LegacyMod> legacy = new ArrayList<>();
        try {
            legacy.addAll(LegacyApi.mods());
        } catch (RuntimeException ignored) {
            // the mod list is not available yet
        }
        for (LegacyMod m : legacy) legacyIds.add(m.id());
        b.append(nl).append("Legacy mods (").append(legacy.size()).append("): name | legacy id | version | Minecraft version | status").append(nl);
        for (LegacyMod m : legacy) {
            b.append(m.name()).append(" | ").append(m.legacyId()).append(" | ").append(m.version()).append(" | ")
                    .append(m.minecraftVersion() == null ? "?" : m.minecraftVersion().id()).append(" | ")
                    .append(m.failed() ? "FAILED " + m.failureMessage().orElse("") : "ok").append(nl);
        }
        List<String> modern = new ArrayList<>();
        for (var mod : ModList.get().getMods()) {
            if (legacyIds.contains(mod.getModId())) continue;
            modern.add(mod.getModId() + " " + mod.getVersion());
        }
        modern.sort(String.CASE_INSENSITIVE_ORDER);
        b.append(nl).append("Modern mods (").append(modern.size()).append(')').append(nl);
        for (String m : modern) b.append(m).append(nl);
        return b.toString();
    }

    private static String version(String modId) {
        return ModList.get().getModContainerById(modId).map(c -> c.getModInfo().getVersion().toString()).orElse("unknown");
    }
}
