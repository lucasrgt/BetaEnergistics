package betaenergistics.storage.index;

import betaenergistics.storage.BE_ItemKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded bootstrap input for one storage provider. */
public final class BE_ProviderSeed {
    private final long id;
    private final BE_ProviderQuality quality;
    private final Map<BE_ItemKey, Integer> items;

    public BE_ProviderSeed(long id, BE_ProviderQuality quality, Map<BE_ItemKey, Integer> items) {
        if (id < 0) throw new IllegalArgumentException("negative provider id");
        if (quality == null || items == null) throw new NullPointerException();
        this.id = id; this.quality = quality;
        this.items = Collections.unmodifiableMap(new LinkedHashMap<BE_ItemKey, Integer>(items));
    }
    public long id() { return id; }
    public BE_ProviderQuality quality() { return quality; }
    public Map<BE_ItemKey, Integer> items() { return items; }
}
