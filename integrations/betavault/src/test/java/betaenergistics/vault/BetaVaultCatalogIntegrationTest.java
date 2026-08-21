package betaenergistics.vault;

import betaenergistics.storage.BE_ItemKey;
import betaenergistics.storage.index.BE_CatalogSnapshot;
import betaenergistics.storage.index.BE_IncrementalCatalog;
import betavault.minecraft.VaultReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class BetaVaultCatalogIntegrationTest {
    private BetaVaultCatalogIntegrationTest() {}
    public static void main(String[] arguments) throws Exception {
        Path root = Files.createTempDirectory("betaenergistics-catalog-");
        try {
            BE_ItemKey iron = new BE_ItemKey(265), gold = new BE_ItemKey(266);
            BE_CellVault vault = BE_CellVault.open(root.resolve("world"));
            VaultReference cell = vault.create(0, 4096);
            BE_IncrementalCatalog first = new BE_IncrementalCatalog();
            BE_CellCatalogBridge bridge = new BE_CellCatalogBridge(first, 7, vault.read(cell));
            try (BE_CellMutation mutation = vault.begin(cell, bridge)) {
                require(mutation.insert(iron, 1024) == 1024, "iron insert");
                require(mutation.insert(gold, 64) == 64, "gold insert");
                mutation.commit();
            }
            BE_CatalogSnapshot beforeRestart = first.snapshot();
            require(beforeRestart.count(iron) == 1024 && first.work().transactionsCommitted == 1,
                    "durable delta publication");
            BE_CellVault restarted = BE_CellVault.open(root.resolve("world"));
            BE_IncrementalCatalog rebuilt = new BE_IncrementalCatalog();
            new BE_CellCatalogBridge(rebuilt, 7, restarted.read(cell));
            require(rebuilt.snapshot().totals().equals(beforeRestart.totals()), "restart catalog equivalence");
            System.out.println("  BetaVault catalog: delta commit and restart rebuild PASS");
        } finally { delete(root); }
    }
    private static void delete(Path root) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) Files.delete(path);
        }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
