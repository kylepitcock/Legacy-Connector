package legacyconnector.v1_12_2.net.minecraftforge.common.ticket;

import java.util.function.Consumer;

/**
 * A ticket a system keeps while it is valid (a machine watering farmland): {@link #invalidate} takes it out, {@link #validate} puts it
 * back; the system asks it {@link #matches} for each position.
 */
public abstract class SimpleTicket<T> {
    private ITicketManager<T> masterManager;
    private ITicketManager<T>[] dummyManagers;
    protected boolean isValid = false;

    @SafeVarargs
    public final void setManager(ITicketManager<T> masterManager, ITicketManager<T>... dummyManagers) {
        if (this.masterManager != null) throw new IllegalStateException("Ticket is already registered to a managing system");
        this.masterManager = masterManager;
        this.dummyManagers = dummyManagers;
    }

    public boolean isValid() {
        return isValid;
    }

    public void invalidate() {
        if (isValid()) forEachManager(manager -> manager.remove(this));
        isValid = false;
    }

    /** Called by the system unloading the ticket's chunk; true when the ticket may be dropped. */
    public boolean unload(ITicketManager<T> unloadingManager) {
        if (unloadingManager == masterManager) {
            for (ITicketManager<T> manager : dummyManagers) manager.remove(this);
            isValid = false;
            return true;
        }
        return false;
    }

    public void validate() {
        if (!isValid()) forEachManager(manager -> manager.add(this));
        isValid = true;
    }

    public abstract boolean matches(T toMatch);

    protected final void forEachManager(Consumer<ITicketManager<T>> consumer) {
        if (masterManager == null) throw new IllegalStateException("Ticket is not registered to a managing system");
        consumer.accept(masterManager);
        for (ITicketManager<T> manager : dummyManagers) consumer.accept(manager);
    }

    protected final ITicketManager<T> getMasterManager() {
        return masterManager;
    }
}
