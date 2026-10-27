package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import java.awt.image.BufferedImage;
import java.io.File;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** A screenshot is about to be saved: handlers change the file, the chat message, or cancel it (with the cancel message). */
@legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
public class ScreenshotEvent extends legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event {
    public static final ITextComponent DEFAULT_CANCEL_REASON = new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString("Screenshot canceled");

    private final BufferedImage image;
    private File screenshotFile;
    private ITextComponent resultMessage;

    public ScreenshotEvent(BufferedImage image, File screenshotFile) {
        this.image = image;
        this.screenshotFile = screenshotFile;
    }

    public BufferedImage getImage() {
        return image;
    }

    public File getScreenshotFile() {
        return screenshotFile;
    }

    public void setScreenshotFile(File file) {
        this.screenshotFile = file;
    }

    public ITextComponent getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(ITextComponent message) {
        this.resultMessage = message;
    }

    public ITextComponent getCancelMessage() {
        return getResultMessage() != null ? getResultMessage() : DEFAULT_CANCEL_REASON;
    }
}
