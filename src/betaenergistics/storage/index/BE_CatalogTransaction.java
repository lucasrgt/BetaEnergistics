package betaenergistics.storage.index;

import betaenergistics.storage.BE_ItemKey;
import java.util.ArrayList;
import java.util.List;

/** Atomic logical batch; catalog changes become visible only on commit. */
public final class BE_CatalogTransaction {
    private final BE_IncrementalCatalog catalog;
    private final List<BE_StorageDelta> deltas = new ArrayList<BE_StorageDelta>();
    private boolean closed;

    public BE_CatalogTransaction(BE_IncrementalCatalog catalog) {
        if (catalog == null) throw new NullPointerException("catalog"); this.catalog = catalog;
    }
    public BE_CatalogTransaction add(long providerId, BE_ItemKey key, long amount) {
        if (closed) throw new IllegalStateException("transaction closed");
        deltas.add(new BE_StorageDelta(providerId, key, amount)); return this;
    }
    public void commit() {
        if (closed) throw new IllegalStateException("transaction closed");
        catalog.applyTransaction(deltas); closed = true;
    }
    public void rollback() {
        if (closed) throw new IllegalStateException("transaction closed"); closed = true;
    }
    public int size() { return deltas.size(); }
    public boolean closed() { return closed; }
}
