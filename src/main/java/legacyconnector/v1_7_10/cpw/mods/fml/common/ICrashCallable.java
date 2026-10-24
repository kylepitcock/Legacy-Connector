package legacyconnector.v1_7_10.cpw.mods.fml.common;

/** Adds a labelled line to crash reports; the modern crash report has its own sections, so callables are accepted and unused. */
public interface ICrashCallable extends java.util.concurrent.Callable<String> {
    String getLabel();
}
