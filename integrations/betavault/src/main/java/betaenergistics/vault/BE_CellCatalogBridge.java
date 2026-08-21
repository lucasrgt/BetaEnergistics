package betaenergistics.vault;

import betaenergistics.storage.BE_ItemKey;
import betaenergistics.storage.index.BE_IncrementalCatalog;
import betaenergistics.storage.index.BE_ProviderQuality;
import betaenergistics.storage.index.BE_StorageDelta;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Delta bridge from a durable BetaVault cell to one catalog provider. */
public final class BE_CellCatalogBridge implements BE_CellCommitListener {
    private final BE_IncrementalCatalog catalog;
    private final long providerId;

    public BE_CellCatalogBridge(BE_IncrementalCatalog catalog, long providerId, BE_CellRecord initial) {
        if (catalog == null || initial == null) throw new NullPointerException();
        this.catalog = catalog; this.providerId = providerId;
        catalog.attach(providerId, BE_ProviderQuality.PUSH, initial.contents());
    }

    @Override public void committed(BE_CellRecord before, BE_CellRecord after) {
        Set<BE_ItemKey> keys = new LinkedHashSet<BE_ItemKey>(before.contents().keySet());
        keys.addAll(after.contents().keySet());
        List<BE_StorageDelta> changes = new ArrayList<BE_StorageDelta>();
        for (BE_ItemKey key : keys) {
            long delta = (long) after.amount(key) - before.amount(key);
            if (delta != 0) changes.add(new BE_StorageDelta(providerId, key, delta));
        }
        if (!changes.isEmpty()) catalog.applyTransaction(changes);
    }
}
