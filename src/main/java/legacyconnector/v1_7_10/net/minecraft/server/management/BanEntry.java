package legacyconnector.v1_7_10.net.minecraft.server.management;

import com.google.gson.JsonObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** A ban: when it started, who issued it, when it ends (null: never) and why. */
public abstract class BanEntry<T> extends UserListEntry<T> {
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    protected final Date banStartDate;
    protected final String bannedBy;
    protected final Date banEndDate;
    protected final String reason;

    public BanEntry(T value, Date start, String bannedBy, Date end, String reason) {
        super(value);
        this.banStartDate = start == null ? new Date() : start;
        this.bannedBy = bannedBy == null ? "(Unknown)" : bannedBy;
        this.banEndDate = end;
        this.reason = reason == null ? "Banned by an operator." : reason;
    }

    protected BanEntry(T value, JsonObject json) {
        super(value, json);
        this.banStartDate = parse(json, "created", new Date());
        this.bannedBy = json.has("source") ? json.get("source").getAsString() : "(Unknown)";
        this.banEndDate = parse(json, "expires", null);
        this.reason = json.has("reason") ? json.get("reason").getAsString() : "Banned by an operator.";
    }

    private static Date parse(JsonObject json, String key, Date fallback) {
        if (!json.has(key)) return fallback;
        try {
            synchronized (dateFormat) {
                return dateFormat.parse(json.get(key).getAsString());
            }
        } catch (ParseException e) {
            return fallback;
        }
    }

    public Date getBanEndDate() {
        return banEndDate;
    }

    public String getBanReason() {
        return reason;
    }

    /** When the ban started (modern lists need it). */
    public Date mcc$start() {
        return banStartDate;
    }

    /** Who issued the ban. */
    public String mcc$source() {
        return bannedBy;
    }

    @Override
    boolean hasBanExpired() {
        return banEndDate != null && banEndDate.before(new Date());
    }

    @Override
    protected void onSerialization(JsonObject json) {
        synchronized (dateFormat) {
            json.addProperty("created", dateFormat.format(banStartDate));
            json.addProperty("source", bannedBy);
            json.addProperty("expires", banEndDate == null ? "forever" : dateFormat.format(banEndDate));
        }
        json.addProperty("reason", reason);
    }
}
