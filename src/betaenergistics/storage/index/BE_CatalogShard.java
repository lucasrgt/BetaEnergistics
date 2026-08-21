package betaenergistics.storage.index;

import betaenergistics.storage.BE_ItemKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Aggregate counts and provider directory for one stable provider shard. */
final class BE_CatalogShard {
    private final Map<BE_ItemKey, Long> totals = new LinkedHashMap<BE_ItemKey, Long>();
    private final Map<BE_ItemKey, Set<Long>> directory =
            new LinkedHashMap<BE_ItemKey, Set<Long>>();

    void apply(long providerId, BE_ItemKey key, long providerBefore,
            long providerAfter, long delta) {
        long total = count(key) + delta;
        if (total < 0) throw new IllegalStateException("negative shard total");
        if (total == 0) totals.remove(key); else totals.put(key, Long.valueOf(total));
        Set<Long> providers = directory.get(key);
        if (providerBefore == 0 && providerAfter > 0) {
            if (providers == null) {
                providers = new LinkedHashSet<Long>(); directory.put(key, providers);
            }
            providers.add(Long.valueOf(providerId));
        } else if (providerBefore > 0 && providerAfter == 0 && providers != null) {
            providers.remove(Long.valueOf(providerId));
            if (providers.isEmpty()) directory.remove(key);
        }
    }
    long count(BE_ItemKey key) {
        Long value = totals.get(key); return value == null ? 0L : value.longValue();
    }
    Set<Long> providers(BE_ItemKey key) {
        Set<Long> value = directory.get(key);
        return value == null ? Collections.<Long>emptySet() : value;
    }
    boolean contains(BE_ItemKey key) { return totals.containsKey(key); }
    boolean empty() { return totals.isEmpty(); }
}
