package legacyconnector.v1_12_2.net.minecraftforge.common.ticket;

/** A system tickets register with (the farmland water manager's chunks). */
public interface ITicketManager<T> {
    void add(SimpleTicket<T> ticket);

    void remove(SimpleTicket<T> ticket);
}
