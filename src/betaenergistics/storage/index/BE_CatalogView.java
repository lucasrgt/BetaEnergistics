package betaenergistics.storage.index;

import betaenergistics.storage.BE_ItemKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Generation-keyed search/sort cache that never touches physical providers. */
public final class BE_CatalogView {
    public enum Sort { NAME, COUNT }
    private static final int MAX_CACHE = 128;
    private final Map<String, List<BE_ItemKey>> cache =
            new LinkedHashMap<String, List<BE_ItemKey>>();
    private long evaluations;

    public synchronized List<BE_ItemKey> query(BE_CatalogSnapshot snapshot,
            Map<BE_ItemKey, String> names, String text, Sort sort, int limit) {
        if (snapshot == null || names == null || text == null || sort == null)
            throw new NullPointerException();
        if (limit <= 0) throw new IllegalArgumentException("limit");
        String normalized = normalize(text);
        String cacheKey = snapshot.generation() + "|" + sort + "|" + limit + "|" + normalized;
        List<BE_ItemKey> existing = cache.get(cacheKey); if (existing != null) return existing;
        List<BE_ItemKey> result = new ArrayList<BE_ItemKey>();
        for (BE_ItemKey key : snapshot.totals().keySet()) {
            String name = names.get(key); if (name != null && normalize(name).contains(normalized)) result.add(key);
        }
        Collections.sort(result, comparator(snapshot, names, sort));
        if (result.size() > limit) result = new ArrayList<BE_ItemKey>(result.subList(0, limit));
        result = Collections.unmodifiableList(result); evaluations++;
        if (cache.size() >= MAX_CACHE) cache.clear(); cache.put(cacheKey, result); return result;
    }
    public synchronized long evaluations() { return evaluations; }
    public synchronized int cachedQueries() { return cache.size(); }

    private static Comparator<BE_ItemKey> comparator(final BE_CatalogSnapshot snapshot,
            final Map<BE_ItemKey, String> names, final Sort sort) {
        return new Comparator<BE_ItemKey>() {
            public int compare(BE_ItemKey left, BE_ItemKey right) {
                if (sort == Sort.COUNT) {
                    int count = Long.compare(snapshot.count(right), snapshot.count(left));
                    if (count != 0) return count;
                }
                String a = names.get(left), b = names.get(right);
                int name = String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
                if (name != 0) return name;
                int id = Integer.compare(left.itemId, right.itemId);
                return id != 0 ? id : Integer.compare(left.damageValue, right.damageValue);
            }
        };
    }
    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
