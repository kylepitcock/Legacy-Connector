package org.kylepitcock.legacyconnector.compat.jdk;

import java.io.Serializable;
import java.nio.charset.Charset;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.RandomAccessFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.pattern.RegexReplacement;

/**
 * log4j 2.0-beta9 (1.7.10) factories that later log4j versions replaced by builders, as mods call them to add a log
 * file of their own (JourneyMap: the NoSuchMethodError made it tell the player that Forge was incompatible). The string
 * arguments are parsed as beta9 did.
 */
public final class Log4jCompat {
    private Log4jCompat() {}

    public static PatternLayout createLayout(String pattern, Configuration config, RegexReplacement replace, String charsetName,
            String always) {
        PatternLayout.Builder b = PatternLayout.newBuilder().withConfiguration(config).withRegexReplacement(replace)
                .withAlwaysWriteExceptions(always == null || Boolean.parseBoolean(always));
        if (pattern != null) b.withPattern(pattern);
        if (charsetName != null && Charset.isSupported(charsetName)) b.withCharset(Charset.forName(charsetName));
        return b.build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static RandomAccessFileAppender createAppender(String fileName, String append, String name, String immediateFlush,
            String ignore, Layout layout, Filter filter, String advertise, String advertiseURI, Configuration config) {
        if (name == null || fileName == null) return null;
        RandomAccessFileAppender.Builder b = RandomAccessFileAppender.newBuilder();
        b.setFileName(fileName).setAppend(append == null || Boolean.parseBoolean(append)).setAdvertise(Boolean.parseBoolean(advertise))
                .setAdvertiseURI(advertiseURI);
        b.setImmediateFlush(immediateFlush == null || Boolean.parseBoolean(immediateFlush));
        b.setName(name).setIgnoreExceptions(ignore == null || Boolean.parseBoolean(ignore)).setConfiguration(config);
        if (layout != null) b.setLayout((Layout<? extends Serializable>) layout);
        b.setFilter(filter);
        return b.build();
    }
}
