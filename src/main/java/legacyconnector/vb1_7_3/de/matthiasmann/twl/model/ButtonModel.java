package legacyconnector.vb1_7_3.de.matthiasmann.twl.model;

public interface ButtonModel {
    boolean isSelected();boolean isPressed();boolean isArmed();boolean isHover();boolean isEnabled();
    void setSelected(boolean v);void setPressed(boolean v);void setArmed(boolean v);void setHover(boolean v);void setEnabled(boolean v);
    void addActionCallback(Runnable r);void removeActionCallback(Runnable r);void fireActionCallback();boolean hasActionCallbacks();
    void addStateCallback(Runnable r);void removeStateCallback(Runnable r);void connect();void disconnect();
}
