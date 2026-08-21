package betaenergistics.storage.index;

import betaenergistics.storage.BE_ItemKey;
import java.util.Map;

/** Supplies one provider-local snapshot for budgeted dirty reconciliation. */
public interface BE_ProviderSnapshotSource {
    Map<BE_ItemKey, Integer> snapshot(long providerId);
}
