package legacyconnector.v1_12_2.net.minecraft.server.management;

/** An entry of a user list (ops, bans, the whitelist): the user it is about. */
public class UserListEntry<T> {
    private final T value;

    public UserListEntry(T valueIn) {
        this.value = valueIn;
    }

    public T getValue() {
        return value;
    }

    boolean hasBanExpired() {
        return false;
    }
}
