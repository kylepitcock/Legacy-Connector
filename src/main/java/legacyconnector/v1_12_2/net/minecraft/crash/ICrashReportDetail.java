package legacyconnector.v1_12_2.net.minecraft.crash;

public interface ICrashReportDetail<V> {
    V call() throws Exception;
}
