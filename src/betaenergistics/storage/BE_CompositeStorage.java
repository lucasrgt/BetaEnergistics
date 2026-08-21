package betaenergistics.storage;

import betaenergistics.storage.index.BE_CatalogSnapshot;
import betaenergistics.storage.index.BE_IncrementalCatalog;
import betaenergistics.storage.index.BE_ProviderQuality;
import betaenergistics.storage.index.BE_WorkSnapshot;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregates multiple item storages (BE_IStorage) into a unified view.
 * Insert goes to highest-priority storage with space.
 * Extract pulls from first storage that has the item.
 */
public class BE_CompositeStorage extends BE_CompositeStorageBase {
    private final BE_IncrementalCatalog catalog = new BE_IncrementalCatalog();
    private final Map<BE_IStorage, Long> providerIds =
            new IdentityHashMap<BE_IStorage, Long>();
    private final boolean indexed;
    private long nextProviderId;

    public BE_CompositeStorage() {
        this(Boolean.getBoolean("betaenergistics.storage.incrementalCatalog"));
    }

    public BE_CompositeStorage(boolean indexed) { this.indexed = indexed; }

    public void addStorage(BE_IStorage storage) {
        if (storage == null) throw new NullPointerException("storage");
        if (providerIds.containsKey(storage)) throw new IllegalArgumentException("duplicate storage");
        addStorageImpl(storage);
        long id = nextProviderId++;
        providerIds.put(storage, Long.valueOf(id));
        if (indexed) catalog.attach(id, BE_ProviderQuality.DIRTY_NOTIFY, storage.getAll());
    }

    public void removeStorage(BE_IStorage storage) {
        removeStorageImpl(storage);
        Long id = providerIds.remove(storage);
        if (indexed && id != null) catalog.detach(id.longValue());
    }

    @Override
    public void clear() {
        if (indexed) for (Long id : providerIds.values()) catalog.detach(id.longValue());
        providerIds.clear(); super.clear();
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (!indexed) return;
        Map<Long, Map<BE_ItemKey, Integer>> snapshots =
                new LinkedHashMap<Long, Map<BE_ItemKey, Integer>>();
        for (Map.Entry<BE_IStorage, Long> entry : providerIds.entrySet())
            snapshots.put(entry.getValue(), entry.getKey().getAll());
        catalog.fullReconcile(snapshots);
    }

    public void markDirty(BE_IStorage storage) {
        if (!indexed) { super.markDirty(); return; }
        Long id = providerIds.get(storage);
        if (id == null) throw new IllegalArgumentException("unknown storage");
        catalog.markDirty(id.longValue());
        catalog.reconcile(id.longValue(), storage.getAll());
    }

    @Override
    protected int getStoragePriority(Object storage) {
        return ((BE_IStorage) storage).getPriority();
    }

    @Override
    protected int doInsert(Object storage, Object key, int amount, boolean simulate) {
        return ((BE_IStorage) storage).insert((BE_ItemKey) key, amount, simulate);
    }

    @Override
    protected int doExtract(Object storage, Object key, int amount, boolean simulate) {
        return ((BE_IStorage) storage).extract((BE_ItemKey) key, amount, simulate);
    }

    @Override
    protected int doGetCount(Object storage, Object key) {
        return ((BE_IStorage) storage).getCount((BE_ItemKey) key);
    }

    @Override
    protected Map<?, Integer> doGetAll(Object storage) {
        return ((BE_IStorage) storage).getAll();
    }

    @Override
    protected int doGetStored(Object storage) {
        return ((BE_IStorage) storage).getStored();
    }

    @Override
    protected int doGetCapacity(Object storage) {
        return ((BE_IStorage) storage).getCapacity();
    }

    @Override
    protected void onMutation(Object storage, Object key, int delta) {
        Long id = providerIds.get((BE_IStorage) storage);
        if (!indexed) return;
        if (id == null) throw new IllegalStateException("unindexed storage mutation");
        catalog.delta(id.longValue(), (BE_ItemKey) key, delta);
    }

    // Public typed API

    public int insert(BE_ItemKey key, int amount, boolean simulate) {
        return insertAll(key, amount, simulate);
    }

    public int extract(BE_ItemKey key, int amount, boolean simulate) {
        return extractAll(key, amount, simulate);
    }

    public int getCount(BE_ItemKey key) {
        if (!indexed) return getCountAll(key);
        long count = catalog.count(key);
        if (count > Integer.MAX_VALUE) throw new IllegalStateException("item count exceeds legacy API");
        return (int) count;
    }

    public Map<BE_ItemKey, Integer> getAll() {
        if (!indexed) {
            Map<BE_ItemKey, Integer> legacy = new LinkedHashMap<BE_ItemKey, Integer>();
            for (Map.Entry<Object, Integer> entry : getAllMerged().entrySet())
                legacy.put((BE_ItemKey) entry.getKey(), entry.getValue());
            return java.util.Collections.unmodifiableMap(legacy);
        }
        Map<BE_ItemKey, Integer> result = new LinkedHashMap<BE_ItemKey, Integer>();
        for (Map.Entry<BE_ItemKey, Long> entry : catalog.snapshot().totals().entrySet()) {
            long count = entry.getValue().longValue();
            if (count > Integer.MAX_VALUE) throw new IllegalStateException("item count exceeds legacy API");
            result.put(entry.getKey(), Integer.valueOf((int) count));
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    public BE_CatalogSnapshot catalogSnapshot() {
        if (!indexed) throw new IllegalStateException("incremental catalog disabled");
        return catalog.snapshot();
    }
    public BE_WorkSnapshot catalogWork() {
        if (!indexed) throw new IllegalStateException("incremental catalog disabled");
        return catalog.work();
    }
}
