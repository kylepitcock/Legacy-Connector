package legacyconnector.v1_12_2.net.minecraft.client;

/**
 * 1.12's loading screen ({@code Minecraft.loadingScreen}): the title, working message and progress of the task the client is busy with.
 * 1.12 drew it straight to the window between frames; the modern game shows its own loading screens, so this keeps the state (mods
 * subclass it and read it back) and draws nothing.
 */
public class LoadingScreenRenderer {
    protected String message = "";
    protected final Minecraft mc;
    protected String currentlyDisplayedText = "";
    protected long systemTime = Minecraft.getSystemTime();
    protected boolean loadingSuccess;
    /** The last percentage set (not a 1.12 field). */
    public int mcc$progress = -1;

    public LoadingScreenRenderer(Minecraft mc) {
        this.mc = mc;
    }

    public void resetProgressAndMessage(String message) {
        loadingSuccess = false;
        displayString(message);
    }

    public void displaySavingString(String message) {
        loadingSuccess = true;
        displayString(message);
    }

    private void displayString(String message) {
        currentlyDisplayedText = message;
        this.message = "";
        mcc$progress = -1;
    }

    public void displayLoadingString(String message) {
        systemTime = 0L;
        this.message = message;
        setLoadingProgress(-1);
        systemTime = 0L;
    }

    public void setLoadingProgress(int percent) {
        systemTime = Minecraft.getSystemTime();
        mcc$progress = percent;
    }

    public void setDoneWorking() {
        mcc$progress = -1;
    }
}
