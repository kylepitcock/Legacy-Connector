package org.kylepitcock.legacyconnector.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.AbstractFilter;

/**
 * Drops vanilla warnings that legacy blocks cause by design. A legacy block whose class hosts tile entities for some metadata
 * only (Immersive Engineering's ores share a class with its machines) is one modern entity block; world generation queues a
 * placeholder block entity for every placement and the chunk warns, once per block, when the metadata has none. That is
 * hundreds of lines per generated chunk; for any other block the warning still shows.
 */
public final class LegacyLogFilter extends AbstractFilter {
    private static final String FAILED = "Tried to load a block entity for block Block{";

    private LegacyLogFilter() {
        super(Filter.Result.NEUTRAL, Filter.Result.NEUTRAL);
    }

    public static void install() {
        try {
            if (!(LogManager.getContext(false) instanceof LoggerContext ctx)) return;
            ctx.getConfiguration().getRootLogger().addFilter(new LegacyLogFilter());
            ctx.updateLoggers();
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector").debug("Log filter not installed", e);
        }
    }

    @Override
    public Filter.Result filter(LogEvent event) {
        if (!"net.minecraft.world.level.chunk.LevelChunk".equals(event.getLoggerName())) return Filter.Result.NEUTRAL;
        String text = event.getMessage().getFormattedMessage();
        if (!text.startsWith(FAILED)) return Filter.Result.NEUTRAL;
        int end = text.indexOf('}', FAILED.length());
        if (end < 0) return Filter.Result.NEUTRAL;
        ResourceLocation id = ResourceLocation.tryParse(text.substring(FAILED.length(), end));
        if (id == null) return Filter.Result.NEUTRAL;
        var block = BuiltInRegistries.BLOCK.get(id);
        return block instanceof org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyEntityBlockAdapter
                || block instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122EntityBlock ? Filter.Result.DENY : Filter.Result.NEUTRAL;
    }
}
