package betaenergistics.vault;

import betavault.core.StableObjectId;
import betavault.minecraft.MinecraftSaveVault;
import betavault.minecraft.VaultReference;
import betavault.store.Handle;
import java.nio.file.Path;

/** Save-bound facade joining BetaEnergistics cells to BetaVault mechanics. */
public final class BE_CellVault {
    private final MinecraftSaveVault vault;

    private BE_CellVault(MinecraftSaveVault vault) { this.vault = vault; }

    public static BE_CellVault open(Path worldDirectory) {
        return new BE_CellVault(MinecraftSaveVault.open(worldDirectory));
    }

    public VaultReference create(int tier, int capacity) {
        StableObjectId id = vault.store().create(BE_CellCodec.INSTANCE, BE_CellRecord.empty(tier, capacity));
        return vault.reference(vault.store().handle(id, BE_CellCodec.INSTANCE));
    }

    public BE_CellRecord read(VaultReference reference) { return vault.store().read(handle(reference)); }

    public BE_CellMutation begin(VaultReference reference) {
        return new BE_CellMutation(vault.store().begin(), handle(reference));
    }

    public BE_CellMutation begin(VaultReference reference, BE_CellCommitListener listener) {
        if (listener == null) throw new NullPointerException("listener");
        return new BE_CellMutation(vault.store().begin(), handle(reference), listener);
    }

    public String worldId() { return vault.worldId(); }

    private Handle<BE_CellRecord> handle(VaultReference reference) {
        return vault.resolve(reference, BE_CellCodec.INSTANCE);
    }
}
