package legacyconnector.v1_12_2.net.minecraft.util;

public class ActionResult<T> {
    private final EnumActionResult type;
    private final T result;

    public ActionResult(EnumActionResult type, T result) {
        this.type = type;
        this.result = result;
    }

    public EnumActionResult getType() {
        return type;
    }

    public T getResult() {
        return result;
    }
}
