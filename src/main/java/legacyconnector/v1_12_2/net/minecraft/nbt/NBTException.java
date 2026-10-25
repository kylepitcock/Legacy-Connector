package legacyconnector.v1_12_2.net.minecraft.nbt;

public class NBTException extends Exception {
    public NBTException(String message) {
        super(message);
    }

    public NBTException(String message, String text, int index) {
        super(message + " at: " + text.substring(0, Math.min(Math.max(index, 0), text.length())) + "<--[HERE]");
    }
}
