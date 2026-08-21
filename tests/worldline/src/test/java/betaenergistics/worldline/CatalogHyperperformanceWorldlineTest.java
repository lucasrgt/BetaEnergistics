package betaenergistics.worldline;

import betaenergistics.storage.BE_ItemKey;
import betaenergistics.storage.index.BE_IncrementalCatalog;
import betaenergistics.storage.index.BE_ProviderQuality;
import java.util.Collections;
import worldline.test.WorldlineSpec;
import static worldline.test.Expect.expect;
import static worldline.test.Worldline.describe;
import static worldline.test.Worldline.test;

/** External TestKit consumer for deterministic catalog invariants. */
public final class CatalogHyperperformanceWorldlineTest extends WorldlineSpec {
    @Override protected void define() {
        describe("BetaEnergistics catalog", () -> {
            test("keeps idle work independent of provider count", context -> {
                BE_IncrementalCatalog catalog = catalog(10_000);
                long scans = catalog.work().providerEntriesScanned;
                for (int tick = 0; tick < 1_000; tick++) catalog.idleTick();
                expect(catalog.work().providerEntriesScanned).toEqual(scans);
                expect(catalog.work().fullScans).toEqual(0L);
            }).tag("hyperperformance");
            test("publishes exact committed deltas", context -> {
                BE_IncrementalCatalog catalog = catalog(1_000);
                BE_ItemKey iron = new BE_ItemKey(265);
                catalog.delta(999, iron, 64);
                expect(catalog.count(iron)).toEqual(64L);
                expect(catalog.providers(iron).contains(Long.valueOf(999))).toBeTrue();
            }).tag("invariant");
            test("keeps old snapshots immutable", context -> {
                BE_IncrementalCatalog catalog = catalog(1);
                betaenergistics.storage.index.BE_CatalogSnapshot before = catalog.snapshot();
                catalog.delta(0, new BE_ItemKey(266), 12);
                expect(before.typeCount()).toEqual(0);
                expect(catalog.snapshot().typeCount()).toEqual(1);
            }).tag("invariant");
        });
    }
    private static BE_IncrementalCatalog catalog(int providers) {
        BE_IncrementalCatalog value = new BE_IncrementalCatalog();
        for (int id = 0; id < providers; id++) value.attach(id, BE_ProviderQuality.PUSH,
                Collections.<BE_ItemKey, Integer>emptyMap());
        return value;
    }
}
