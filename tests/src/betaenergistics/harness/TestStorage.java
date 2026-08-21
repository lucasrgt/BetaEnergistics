package betaenergistics.harness;

import betaenergistics.storage.BE_IStorage;
import betaenergistics.storage.BE_ItemKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class TestStorage implements BE_IStorage {
    private final Map<BE_ItemKey, Integer> items = new LinkedHashMap<BE_ItemKey, Integer>();
    private final int capacity, priority;
    TestStorage(int capacity, int priority) { this.capacity = capacity; this.priority = priority; }
    public int insert(BE_ItemKey key, int amount, boolean simulate) {
        int inserted = Math.min(amount, capacity - getStored());
        if (!simulate && inserted > 0) put(key, getCount(key) + inserted); return inserted;
    }
    public int extract(BE_ItemKey key, int amount, boolean simulate) {
        int extracted = Math.min(amount, getCount(key));
        if (!simulate && extracted > 0) put(key, getCount(key) - extracted); return extracted;
    }
    public int getCount(BE_ItemKey key) { Integer value = items.get(key); return value == null ? 0 : value; }
    public Map<BE_ItemKey, Integer> getAll() { return Collections.unmodifiableMap(items); }
    public int getStored() { int total = 0; for (Integer value : items.values()) total += value; return total; }
    public int getCapacity() { return capacity; }
    public int getPriority() { return priority; }
    void externalSet(BE_ItemKey key, int amount) { put(key, amount); }
    private void put(BE_ItemKey key, int amount) {
        if (amount == 0) items.remove(key); else items.put(key, Integer.valueOf(amount));
    }
}
