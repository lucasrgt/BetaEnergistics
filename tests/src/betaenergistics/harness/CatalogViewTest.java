package betaenergistics.harness;

import betaenergistics.storage.BE_ItemKey;
import betaenergistics.storage.index.BE_CatalogSnapshot;
import betaenergistics.storage.index.BE_CatalogView;
import betaenergistics.storage.index.BE_IncrementalCatalog;
import betaenergistics.storage.index.BE_ProviderQuality;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CatalogViewTest {
    private CatalogViewTest() {}
    public static void main(String[] arguments) {
        BE_IncrementalCatalog catalog = new BE_IncrementalCatalog();
        Map<BE_ItemKey, Integer> items = new LinkedHashMap<BE_ItemKey, Integer>();
        Map<BE_ItemKey, String> names = new LinkedHashMap<BE_ItemKey, String>();
        for (int index = 0; index < 5000; index++) {
            BE_ItemKey key = new BE_ItemKey(10000 + index);
            items.put(key, Integer.valueOf(index + 1));
            names.put(key, (index % 2 == 0 ? "Iron " : "Copper ") + index);
        }
        catalog.attach(1, BE_ProviderQuality.PUSH, items);
        BE_CatalogView view = new BE_CatalogView();
        BE_CatalogSnapshot first = catalog.snapshot();
        List<BE_ItemKey> result = view.query(first, names, " iron ", BE_CatalogView.Sort.COUNT, 32);
        require(result.size() == 32 && first.count(result.get(0)) == 4999, "count-sorted search");
        require(view.evaluations() == 1, "first evaluation");
        require(view.query(first, names, "IRON", BE_CatalogView.Sort.COUNT, 32) == result,
                "generation query cache");
        require(view.evaluations() == 1, "cache miss on equivalent query");
        catalog.delta(1, result.get(0), 10);
        view.query(catalog.snapshot(), names, "iron", BE_CatalogView.Sort.COUNT, 32);
        require(view.evaluations() == 2, "generation did not invalidate view");
        System.out.println("CatalogViewTest passed items=5000 cachedQueries=" + view.cachedQueries());
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
