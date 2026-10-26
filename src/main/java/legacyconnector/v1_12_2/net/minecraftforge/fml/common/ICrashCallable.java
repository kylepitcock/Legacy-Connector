package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import java.util.concurrent.Callable;

/** A section mods add to crash reports: its label and, when called, its text. */
public interface ICrashCallable extends Callable<String> {
    String getLabel();
}
