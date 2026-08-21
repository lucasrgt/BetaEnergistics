package betaenergistics.storage.index;

import betaenergistics.storage.BE_ItemKey;
import java.util.LinkedHashMap;
import java.util.Map;

/** Package-private mutable state for one attached provider. */
final class BE_ProviderState {
    final long id;
    final BE_ProviderQuality quality;
    final Map<BE_ItemKey, Long> items = new LinkedHashMap<BE_ItemKey, Long>();
    long generation;
    boolean dirty;

    BE_ProviderState(long id, BE_ProviderQuality quality) {
        this.id = id; this.quality = quality;
    }
    long count(BE_ItemKey key) {
        Long value = items.get(key); return value == null ? 0L : value.longValue();
    }
    void set(BE_ItemKey key, long value) {
        if (value == 0) items.remove(key); else items.put(key, Long.valueOf(value));
    }
}
