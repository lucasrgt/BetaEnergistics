package betaenergistics.harness;

import betaenergistics.storage.BE_ItemKey;
import betaenergistics.storage.index.BE_CatalogTransaction;
import betaenergistics.storage.index.BE_IncrementalCatalog;
import betaenergistics.storage.index.BE_ProviderQuality;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class CatalogSoakTest {
    private static final int PROVIDERS = 1024, STEPS = 50_000;
    private CatalogSoakTest() {}
    public static void main(String[] arguments) {
        BE_IncrementalCatalog catalog = new BE_IncrementalCatalog();
        Map<BE_ItemKey, Long> reference = new LinkedHashMap<BE_ItemKey, Long>();
        long[][] providerCounts = new long[PROVIDERS][64];
        for (int id = 0; id < PROVIDERS; id++)
            catalog.attach(id, BE_ProviderQuality.PUSH, Collections.<BE_ItemKey, Integer>emptyMap());
        Random random = new Random(173);
        for (int step = 0; step < STEPS; step++) {
            int keyIndex = random.nextInt(64), provider = random.nextInt(PROVIDERS);
            BE_ItemKey key = new BE_ItemKey(200 + keyIndex);
            long providerBefore = providerCounts[provider][keyIndex];
            long delta = providerBefore == 0 || random.nextBoolean() ? 1 + random.nextInt(16)
                    : -Math.min(providerBefore, 1 + random.nextInt(16));
            catalog.delta(provider, key, delta);
            providerCounts[provider][keyIndex] += delta;
            put(reference, key, value(reference, key) + delta);
            if (step % 997 == 0) compare(catalog, reference);
        }
        compare(catalog, reference);
        BE_ItemKey absent = new BE_ItemKey(9999);
        long generation = catalog.snapshot().generation();
        try {
            new BE_CatalogTransaction(catalog).add(0, absent, -1).add(1, absent, 1).commit();
            throw new AssertionError("invalid transaction accepted");
        } catch (IllegalStateException expected) {
            require(catalog.count(absent) == 0 && catalog.snapshot().generation() == generation,
                    "failed transaction was visible");
        }
        require(catalog.work().fullScans == 0, "soak performed full scan");
        System.out.println("CatalogSoakTest passed mutations=" + STEPS + " fullScans=0");
    }
    private static void compare(BE_IncrementalCatalog catalog, Map<BE_ItemKey, Long> expected) {
        require(catalog.snapshot().totals().equals(expected), "reference/catalog divergence");
    }
    private static long value(Map<BE_ItemKey, Long> map, BE_ItemKey key) {
        Long value = map.get(key); return value == null ? 0 : value.longValue();
    }
    private static void put(Map<BE_ItemKey, Long> map, BE_ItemKey key, long value) {
        if (value == 0) map.remove(key); else map.put(key, Long.valueOf(value));
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
