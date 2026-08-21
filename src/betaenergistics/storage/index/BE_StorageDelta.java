package betaenergistics.storage.index;

import betaenergistics.storage.BE_ItemKey;

/** One logical item-count change from one provider. */
public final class BE_StorageDelta {
    private final long providerId;
    private final BE_ItemKey key;
    private final long amount;

    public BE_StorageDelta(long providerId, BE_ItemKey key, long amount) {
        if (providerId < 0) throw new IllegalArgumentException("negative provider id");
        if (key == null) throw new NullPointerException("key");
        if (amount == 0) throw new IllegalArgumentException("zero delta");
        this.providerId = providerId;
        this.key = key;
        this.amount = amount;
    }

    public long providerId() { return providerId; }
    public BE_ItemKey key() { return key; }
    public long amount() { return amount; }
}
