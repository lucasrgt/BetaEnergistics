package betaenergistics.storage.index;

import betaenergistics.storage.BE_ItemKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Sharded delta-driven item catalog; normal idle work never scans providers. */
public final class BE_IncrementalCatalog {
    public static final int PROVIDERS_PER_SHARD = 128;
    private final Map<Long, BE_ProviderState> providers =
            new LinkedHashMap<Long, BE_ProviderState>();
    private final Map<Long, BE_CatalogShard> shards = new LinkedHashMap<Long, BE_CatalogShard>();
    private final Map<BE_ItemKey, Long> totals = new LinkedHashMap<BE_ItemKey, Long>();
    private final Map<BE_ItemKey, Set<Long>> keyShards =
            new LinkedHashMap<BE_ItemKey, Set<Long>>();
    private final Set<Long> dirtyProviders = new LinkedHashSet<Long>();
    private final BE_WorkCounters work = new BE_WorkCounters();
    private long generation;

    public synchronized void attach(long id, BE_ProviderQuality quality,
            Map<BE_ItemKey, Integer> snapshot) {
        if (providers.containsKey(Long.valueOf(id))) throw new IllegalArgumentException("duplicate provider " + id);
        validate(id, quality, snapshot); BE_ProviderState state = new BE_ProviderState(id, quality);
        providers.put(Long.valueOf(id), state); work.providerSnapshot(); work.providerEntries(snapshot.size());
        for (Map.Entry<BE_ItemKey, Integer> entry : snapshot.entrySet()) {
            int amount = amount(entry.getValue()); if (amount > 0) apply(state, entry.getKey(), amount);
        }
        state.generation = 1; generation++;
    }
    public synchronized void detach(long id) {
        BE_ProviderState state = require(id);
        for (Map.Entry<BE_ItemKey, Long> entry : new ArrayList<Map.Entry<BE_ItemKey, Long>>(state.items.entrySet()))
            apply(state, entry.getKey(), -entry.getValue().longValue());
        providers.remove(Long.valueOf(id)); generation++;
        dirtyProviders.remove(Long.valueOf(id));
    }
    public synchronized void delta(long id, BE_ItemKey key, long amount) {
        if (key == null || amount == 0) throw new IllegalArgumentException("invalid delta");
        BE_ProviderState state = require(id); apply(state, key, amount);
        state.generation++; generation++; work.delta();
    }
    public synchronized void applyTransaction(Collection<BE_StorageDelta> deltas) {
        if (deltas == null || deltas.isEmpty()) throw new IllegalArgumentException("empty transaction");
        Map<Long, Map<BE_ItemKey, Long>> projected = new LinkedHashMap<Long, Map<BE_ItemKey, Long>>();
        for (BE_StorageDelta delta : deltas) {
            BE_ProviderState state = require(delta.providerId());
            Map<BE_ItemKey, Long> values = projected.get(Long.valueOf(state.id));
            if (values == null) { values = new LinkedHashMap<BE_ItemKey, Long>(state.items);
                projected.put(Long.valueOf(state.id), values); }
            long next = value(values, delta.key()) + delta.amount(); requireNonnegative(next);
            put(values, delta.key(), next);
        }
        Set<Long> changed = new LinkedHashSet<Long>();
        for (BE_StorageDelta delta : deltas) { apply(require(delta.providerId()), delta.key(), delta.amount());
            changed.add(Long.valueOf(delta.providerId())); work.delta(); }
        for (Long id : changed) require(id.longValue()).generation++;
        generation++; work.transaction();
    }
    public synchronized void markDirty(long id) {
        require(id).dirty = true; dirtyProviders.add(Long.valueOf(id));
    }
    public synchronized void reconcile(long id, Map<BE_ItemKey, Integer> snapshot) {
        BE_ProviderState state = require(id); validate(id, state.quality, snapshot);
        Set<BE_ItemKey> keys = new LinkedHashSet<BE_ItemKey>(state.items.keySet()); keys.addAll(snapshot.keySet());
        work.providerSnapshot(); work.providerEntries(snapshot.size()); work.reconcile();
        for (BE_ItemKey key : keys) {
            long next = snapshot.containsKey(key) ? amount(snapshot.get(key)) : 0;
            long delta = next - state.count(key); if (delta != 0) { apply(state, key, delta); work.delta(); }
        }
        state.dirty = false; dirtyProviders.remove(Long.valueOf(id)); state.generation++; generation++;
    }
    public synchronized int reconcileDirty(BE_ProviderSnapshotSource source, int budget) {
        if (source == null) throw new NullPointerException("source");
        if (budget <= 0) throw new IllegalArgumentException("budget");
        int completed = 0;
        for (Long id : new ArrayList<Long>(dirtyProviders)) {
            if (completed == budget) break;
            reconcile(id.longValue(), source.snapshot(id.longValue())); completed++;
        }
        return completed;
    }
    public synchronized void fullReconcile(Map<Long, Map<BE_ItemKey, Integer>> snapshots) {
        if (snapshots == null || snapshots.size() != providers.size()
                || !snapshots.keySet().equals(providers.keySet()))
            throw new IllegalArgumentException("full provider snapshot mismatch");
        work.fullScan();
        for (Map.Entry<Long, Map<BE_ItemKey, Integer>> entry : snapshots.entrySet())
            reconcile(entry.getKey().longValue(), entry.getValue());
    }
    public synchronized long count(BE_ItemKey key) { work.countQuery(); return value(totals, key); }
    public synchronized Set<Long> providers(BE_ItemKey key) {
        work.directoryQuery(); Set<Long> result = new LinkedHashSet<Long>();
        Set<Long> ids = keyShards.get(key); if (ids != null) for (Long id : ids) {
            work.shardLookup(); result.addAll(shards.get(id).providers(key));
        }
        return Collections.unmodifiableSet(result);
    }
    public synchronized void idleTick() { work.idleTick(); }
    public synchronized BE_CatalogSnapshot snapshot() {
        work.snapshot(); return new BE_CatalogSnapshot(generation, totals);
    }
    public synchronized BE_WorkSnapshot work() { return work.snapshotValue(); }
    public synchronized int providerCount() { return providers.size(); }
    public synchronized int shardCount() { return shards.size(); }
    public synchronized long providerGeneration(long id) { return require(id).generation; }
    public synchronized boolean dirty(long id) { return require(id).dirty; }
    public synchronized int dirtyCount() { return dirtyProviders.size(); }

    private void apply(BE_ProviderState state, BE_ItemKey key, long delta) {
        long before = state.count(key), after = before + delta; requireNonnegative(after);
        long shardId = state.id / PROVIDERS_PER_SHARD;
        BE_CatalogShard shard = shards.get(Long.valueOf(shardId));
        if (shard == null) { shard = new BE_CatalogShard(); shards.put(Long.valueOf(shardId), shard); }
        shard.apply(state.id, key, before, after, delta); state.set(key, after);
        long total = value(totals, key) + delta; requireNonnegative(total); put(totals, key, total);
        Set<Long> ids = keyShards.get(key);
        if (shard.contains(key)) { if (ids == null) { ids = new LinkedHashSet<Long>(); keyShards.put(key, ids); }
            ids.add(Long.valueOf(shardId)); }
        else if (ids != null) { ids.remove(Long.valueOf(shardId)); if (ids.isEmpty()) keyShards.remove(key); }
        if (shard.empty()) shards.remove(Long.valueOf(shardId));
    }
    private BE_ProviderState require(long id) {
        BE_ProviderState value = providers.get(Long.valueOf(id));
        if (value == null) throw new IllegalArgumentException("unknown provider " + id); return value;
    }
    private static void validate(long id, BE_ProviderQuality quality, Map<BE_ItemKey, Integer> values) {
        if (id < 0 || quality == null || values == null) throw new IllegalArgumentException("invalid provider");
        for (Map.Entry<BE_ItemKey, Integer> entry : values.entrySet()) {
            if (entry.getKey() == null) throw new IllegalArgumentException("null item key"); amount(entry.getValue());
        }
    }
    private static int amount(Integer value) {
        if (value == null || value.intValue() < 0) throw new IllegalArgumentException("negative item count");
        return value.intValue();
    }
    private static long value(Map<BE_ItemKey, ? extends Number> map, BE_ItemKey key) {
        Number value = map.get(key); return value == null ? 0 : value.longValue();
    }
    private static void put(Map<BE_ItemKey, Long> map, BE_ItemKey key, long value) {
        if (value == 0) map.remove(key); else map.put(key, Long.valueOf(value));
    }
    private static void requireNonnegative(long value) {
        if (value < 0) throw new IllegalStateException("negative catalog count");
    }
}
