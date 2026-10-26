package legacyconnector.v1_7_10.net.minecraft.util;

/** Hears the progress of a long task (world saving, terrain loading): a title, a working message and a percentage. */
public interface IProgressUpdate {
    void displayProgressMessage(String message);

    void resetProgressAndMessage(String message);

    void resetProgresAndWorkingMessage(String message);

    void setLoadingProgress(int percent);

    void setDoneWorking();
}
