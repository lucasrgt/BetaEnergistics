package betaenergistics.vault;

import betaenergistics.storage.BE_ItemKey;
import betavault.core.BetaVaultException;
import betavault.minecraft.VaultReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Real BetaEnergistics-domain integration against the pinned BetaVault. */
public final class BetaVaultCellIntegrationTest {
    public static void main(String[] arguments) throws Exception {
        Path root = Files.createTempDirectory("betaenergistics-vault-");
        try {
            Path world = root.resolve("world");
            BE_CellVault vault = BE_CellVault.open(world);
            VaultReference physical = vault.create(0, 1024);
            VaultReference copiedByTmi = VaultReference.parse(physical.canonical());
            require(physical.equals(copiedByTmi), "physical copy changed logical identity");
            BE_ItemKey iron = new BE_ItemKey(265, 0);
            try (BE_CellMutation seed = vault.begin(physical)) {
                require(seed.insert(iron, 100) == 100, "seed insertion");
                seed.commit();
            }

            BE_CellMutation accepted = vault.begin(physical);
            BE_CellMutation staleCopy = vault.begin(copiedByTmi);
            require(accepted.extract(iron, 80) == 80, "accepted extraction");
            require(staleCopy.extract(iron, 80) == 80, "stale view");
            accepted.commit();
            reject(staleCopy::commit, "duplicated stale extraction");
            require(vault.read(physical).amount(iron) == 20, "post-conflict conservation");

            BE_CellVault restarted = BE_CellVault.open(world);
            require(restarted.read(physical).amount(iron) == 20, "restart identity/content");
            BE_CellVault otherWorld = BE_CellVault.open(root.resolve("other-world"));
            reject(() -> otherWorld.read(physical), "cross-world reference");
            System.out.println("  BetaVault integration: TMI alias, conflict, restart, world scope PASS");
        } finally { delete(root); }
    }

    private static void reject(Action action, String message) {
        try { action.run(); throw new IllegalStateException("accepted " + message); }
        catch (BetaVaultException expected) { require(expected.operation() != null, "missing diagnostic"); }
    }

    private static void delete(Path root) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) Files.delete(path);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private interface Action { void run(); }
}
