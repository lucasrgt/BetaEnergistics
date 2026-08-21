package betaenergistics.harness;

import betaenergistics.storage.BE_ItemKey;
import betaenergistics.storage.index.BE_IncrementalCatalog;
import betaenergistics.storage.index.BE_ProviderQuality;
import betaenergistics.storage.index.BE_WorkSnapshot;
import java.util.Collections;

public final class CatalogScaleTest {
    private CatalogScaleTest() {}
    public static void main(String[] arguments) {
        qualify(1_000); qualify(10_000); qualify(100_000);
        System.out.println("CatalogScaleTest passed matrix=1000,10000,100000 idleScans=0");
    }
    private static void qualify(int providers) {
        BE_IncrementalCatalog catalog = new BE_IncrementalCatalog();
        BE_ItemKey iron = new BE_ItemKey(265);
        for (int id = 0; id < providers; id++) catalog.attach(id, BE_ProviderQuality.PUSH,
                id % 1000 == 0 ? Collections.singletonMap(iron, Integer.valueOf(1))
                        : Collections.<BE_ItemKey, Integer>emptyMap());
        require(catalog.providerCount() == providers, "provider scale " + providers);
        BE_WorkSnapshot before = catalog.work();
        for (int tick = 0; tick < 10_000; tick++) catalog.idleTick();
        long expected = providers / 1000;
        for (int query = 0; query < 10_000; query++)
            require(catalog.count(iron) == expected, "lookup drift " + providers);
        BE_WorkSnapshot idle = catalog.work();
        require(idle.providerEntriesScanned == before.providerEntriesScanned, "idle/query scanned providers");
        require(idle.fullScans == 0 && idle.idleTicks - before.idleTicks == 10_000,
                "idle structural work");
        catalog.delta(providers - 1, iron, 16); BE_WorkSnapshot changed = catalog.work();
        require(catalog.count(iron) == expected + 16 && changed.deltasApplied - idle.deltasApplied == 1,
                "single mutation was not constant work");
        require(changed.providerEntriesScanned == idle.providerEntriesScanned,
                "push mutation scanned a provider");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
