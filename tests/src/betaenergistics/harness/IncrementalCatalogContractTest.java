package betaenergistics.harness;

import betaenergistics.storage.BE_CompositeStorage;
import betaenergistics.storage.BE_ItemKey;
import betaenergistics.storage.index.BE_CatalogBootstrap;
import betaenergistics.storage.index.BE_CatalogSnapshot;
import betaenergistics.storage.index.BE_CatalogTransaction;
import betaenergistics.storage.index.BE_IncrementalCatalog;
import betaenergistics.storage.index.BE_ProviderQuality;
import betaenergistics.storage.index.BE_ProviderSeed;
import betaenergistics.storage.index.BE_WorkSnapshot;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class IncrementalCatalogContractTest {
    private static final BE_ItemKey IRON = new BE_ItemKey(265), GOLD = new BE_ItemKey(266);
    private IncrementalCatalogContractTest() {}
    public static void main(String[] arguments) {
        legacyDefaultIsRollback(); compositeUsesDeltas(); bootstrapAndTransactions();
        System.out.println("IncrementalCatalogContractTest passed");
    }
    private static void legacyDefaultIsRollback() {
        BE_CompositeStorage storage = new BE_CompositeStorage();
        TestStorage provider = new TestStorage(1000, 0); provider.externalSet(IRON, 9);
        storage.addStorage(provider);
        require(storage.getCount(IRON) == 9 && storage.extract(IRON, 4, false) == 4
                && storage.getCount(IRON) == 5, "legacy rollback path");
        try { storage.catalogWork(); throw new AssertionError("disabled catalog was observable"); }
        catch (IllegalStateException expected) { }
    }
    private static void compositeUsesDeltas() {
        BE_CompositeStorage storage = new BE_CompositeStorage(true);
        TestStorage high = new TestStorage(1000, 10), low = new TestStorage(1000, 0);
        high.externalSet(IRON, 20); low.externalSet(IRON, 5);
        storage.addStorage(high); storage.addStorage(low);
        require(storage.getCount(IRON) == 25, "initial aggregate");
        BE_WorkSnapshot before = storage.catalogWork();
        require(storage.insert(IRON, 10, false) == 10 && storage.getCount(IRON) == 35, "delta insert");
        require(storage.extract(IRON, 7, false) == 7 && storage.getCount(IRON) == 28, "delta extract");
        require(storage.insert(GOLD, 3, true) == 3 && storage.getCount(GOLD) == 0, "simulation mutated index");
        BE_WorkSnapshot after = storage.catalogWork();
        require(after.deltasApplied - before.deltasApplied == 2, "committed delta count");
        long scanned = after.providerEntriesScanned; high.externalSet(IRON, 40); storage.markDirty(high);
        require(storage.getCount(IRON) == 45, "dirty reconciliation");
        require(storage.catalogWork().providerEntriesScanned > scanned, "provider was not reconciled");
        require(storage.catalogWork().fullScans == 0, "local dirty path became a full scan");
        storage.removeStorage(low); require(storage.getCount(IRON) == 40, "detach did not subtract provider");
    }
    private static void bootstrapAndTransactions() {
        Map<BE_ItemKey, Integer> first = map(IRON, 64), second = map(GOLD, 12);
        BE_IncrementalCatalog catalog = new BE_IncrementalCatalog();
        BE_CatalogBootstrap boot = new BE_CatalogBootstrap(Arrays.asList(
                new BE_ProviderSeed(1, BE_ProviderQuality.PUSH, first),
                new BE_ProviderSeed(129, BE_ProviderQuality.DIRTY_NOTIFY, second)));
        require(boot.process(catalog, 1) == 1 && !boot.complete() && catalog.count(IRON) == 64,
                "bounded bootstrap first tick");
        require(boot.process(catalog, 1) == 1 && boot.complete(), "bounded bootstrap completion");
        BE_CatalogSnapshot before = catalog.snapshot();
        new BE_CatalogTransaction(catalog).add(1, IRON, -16).add(129, IRON, 16).commit();
        require(catalog.count(IRON) == 64 && catalog.providers(IRON).size() == 2,
                "transaction aggregate/directory");
        require(before.count(IRON) == 64 && before.typeCount() == 2, "snapshot was mutable");
        long generation = catalog.providerGeneration(1); catalog.markDirty(1);
        catalog.reconcile(1, map(IRON, 40));
        require(!catalog.dirty(1) && catalog.providerGeneration(1) == generation + 1,
                "generation/reconcile");
    }
    private static Map<BE_ItemKey, Integer> map(BE_ItemKey key, int value) {
        Map<BE_ItemKey, Integer> result = new LinkedHashMap<BE_ItemKey, Integer>();
        result.put(key, Integer.valueOf(value)); return result;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
