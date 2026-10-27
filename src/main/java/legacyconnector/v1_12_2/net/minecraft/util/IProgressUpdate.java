package legacyconnector.v1_12_2.net.minecraft.util;

/** Something that shows the progress of a long task (a world conversion or download): a title, a stage and a percentage. */
public interface IProgressUpdate {
    void displaySavingString(String message);

    void resetProgressAndMessage(String message);

    void displayLoadingString(String message);

    void setLoadingProgress(int progress);

    void setDoneWorking();
}
