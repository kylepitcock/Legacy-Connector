package legacyconnector.v1_7_10.net.minecraft.client;

/**
 * 1.7.10's loading screen ({@code Minecraft.loadingScreen}): the title, working message and progress of the task the
 * client is busy with. 1.7.10 drew it straight to the window between frames; the modern game shows its own loading
 * screens, so this keeps the state (mods subclass it and read it back) and draws nothing.
 */
public class LoadingScreenRenderer implements legacyconnector.v1_7_10.net.minecraft.util.IProgressUpdate {
    protected String field_73727_a = "";
    protected Minecraft mc;
    protected String currentlyDisplayedText = "";
    protected long field_73723_d = Minecraft.getSystemTime();
    protected boolean field_73724_e;
    /** The last percentage set (not a 1.7.10 field). */
    public int mcc$progress = -1;

    public LoadingScreenRenderer(Minecraft mc) {
        this.mc = mc;
    }

    @Override
    public void resetProgressAndMessage(String message) {
        field_73724_e = false;
        func_73722_d(message);
    }

    @Override
    public void displayProgressMessage(String message) {
        field_73724_e = true;
        func_73722_d(message);
    }

    /** Sets the title (and clears the working message). */
    public void func_73722_d(String message) {
        currentlyDisplayedText = message;
        field_73727_a = "";
        mcc$progress = -1;
    }

    @Override
    public void resetProgresAndWorkingMessage(String message) {
        field_73723_d = 0L;
        field_73727_a = message;
        setLoadingProgress(-1);
        field_73723_d = 0L;
    }

    @Override
    public void setLoadingProgress(int percent) {
        field_73723_d = Minecraft.getSystemTime();
        mcc$progress = percent;
    }

    @Override
    public void setDoneWorking() {
        mcc$progress = -1;
    }
}
