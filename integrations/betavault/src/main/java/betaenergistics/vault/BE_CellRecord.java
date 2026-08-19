package betaenergistics.vault;

import betaenergistics.storage.BE_ItemKey;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/** Immutable logical contents of one item storage cell. */
public final class BE_CellRecord {
    private static final Comparator<BE_ItemKey> KEY_ORDER = new Comparator<BE_ItemKey>() {
        @Override public int compare(BE_ItemKey left, BE_ItemKey right) {
            int item = Integer.compare(left.itemId, right.itemId);
            return item != 0 ? item : Integer.compare(left.damageValue, right.damageValue);
        }
    };
    private final int tier;
    private final int capacity;
    private final Map<BE_ItemKey, Integer> contents;

    public BE_CellRecord(int tier, int capacity, Map<BE_ItemKey, Integer> contents) {
        if (tier < 0 || tier > 5 || capacity < 1 || contents == null) throw new IllegalArgumentException("cell shape");
        TreeMap<BE_ItemKey, Integer> copy = new TreeMap<BE_ItemKey, Integer>(KEY_ORDER);
        long stored = 0;
        for (Map.Entry<BE_ItemKey, Integer> entry : contents.entrySet()) {
            BE_ItemKey key = entry.getKey();
            Integer amount = entry.getValue();
            if (key == null || key.itemId < 0 || key.damageValue < 0 || amount == null || amount.intValue() < 1) {
                throw new IllegalArgumentException("invalid cell entry");
            }
            if (copy.put(key, amount) != null) throw new IllegalArgumentException("duplicate cell entry");
            stored += amount.intValue();
        }
        if (copy.size() > 63 || stored > capacity) throw new IllegalArgumentException("cell bound");
        this.tier = tier;
        this.capacity = capacity;
        this.contents = Collections.unmodifiableMap(copy);
    }

    public static BE_CellRecord empty(int tier, int capacity) {
        return new BE_CellRecord(tier, capacity, Collections.<BE_ItemKey, Integer>emptyMap());
    }

    public int tier() { return tier; }

    public int capacity() { return capacity; }

    public Map<BE_ItemKey, Integer> contents() { return contents; }

    public int amount(BE_ItemKey key) {
        Integer amount = contents.get(key);
        return amount == null ? 0 : amount.intValue();
    }

    public int stored() {
        int result = 0;
        for (Integer amount : contents.values()) result += amount.intValue();
        return result;
    }

    public BE_CellRecord withAmount(BE_ItemKey key, int amount) {
        if (key == null || amount < 0) throw new IllegalArgumentException("amount");
        Map<BE_ItemKey, Integer> next = new TreeMap<BE_ItemKey, Integer>(KEY_ORDER);
        next.putAll(contents);
        if (amount == 0) next.remove(key); else next.put(key, Integer.valueOf(amount));
        return new BE_CellRecord(tier, capacity, next);
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof BE_CellRecord)) return false;
        BE_CellRecord that = (BE_CellRecord) other;
        return tier == that.tier && capacity == that.capacity && contents.equals(that.contents);
    }

    @Override public int hashCode() { return contents.hashCode() * 31 + capacity * 7 + tier; }
}
