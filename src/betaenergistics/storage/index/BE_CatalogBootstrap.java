package betaenergistics.storage.index;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

/** Incremental provider bootstrap with an explicit per-tick budget. */
public final class BE_CatalogBootstrap {
    private final Queue<BE_ProviderSeed> pending = new ArrayDeque<BE_ProviderSeed>();
    private final int total;

    public BE_CatalogBootstrap(Collection<BE_ProviderSeed> seeds) {
        if (seeds == null) throw new NullPointerException("seeds");
        pending.addAll(seeds); total = pending.size();
    }
    public int process(BE_IncrementalCatalog catalog, int budget) {
        if (catalog == null) throw new NullPointerException("catalog");
        if (budget <= 0) throw new IllegalArgumentException("budget");
        int count = 0;
        while (count < budget && !pending.isEmpty()) {
            BE_ProviderSeed seed = pending.remove();
            catalog.attach(seed.id(), seed.quality(), seed.items()); count++;
        }
        return count;
    }
    public int total() { return total; }
    public int remaining() { return pending.size(); }
    public int completed() { return total - pending.size(); }
    public boolean complete() { return pending.isEmpty(); }
}
