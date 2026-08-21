package betaenergistics.storage.index;

import betaenergistics.storage.BE_ItemKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable aggregate view published to terminals and search indexes. */
public final class BE_CatalogSnapshot {
    private final long generation;
    private final Map<BE_ItemKey, Long> totals;

    BE_CatalogSnapshot(long generation, Map<BE_ItemKey, Long> totals) {
        this.generation = generation;
        this.totals = Collections.unmodifiableMap(new LinkedHashMap<BE_ItemKey, Long>(totals));
    }

    public long generation() { return generation; }
    public long count(BE_ItemKey key) {
        Long value = totals.get(key); return value == null ? 0L : value.longValue();
    }
    public Map<BE_ItemKey, Long> totals() { return totals; }
    public int typeCount() { return totals.size(); }
}
