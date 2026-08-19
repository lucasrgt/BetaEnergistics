package betaenergistics.vault;

import betaenergistics.storage.BE_ItemKey;
import betavault.store.Handle;
import betavault.store.TransactionResult;
import betavault.store.VaultTransaction;

/** One optimistic mutation of a logical storage cell. */
public final class BE_CellMutation implements AutoCloseable {
    private final VaultTransaction transaction;
    private final Handle<BE_CellRecord> handle;
    private BE_CellRecord record;

    BE_CellMutation(VaultTransaction transaction, Handle<BE_CellRecord> handle) {
        this.transaction = transaction;
        this.handle = handle;
        this.record = transaction.read(handle);
    }

    public int extract(BE_ItemKey key, int requested) {
        if (key == null || requested < 0) throw new IllegalArgumentException("extract request");
        int extracted = Math.min(record.amount(key), requested);
        record = record.withAmount(key, record.amount(key) - extracted);
        return extracted;
    }

    public int insert(BE_ItemKey key, int requested) {
        if (key == null || requested < 0) throw new IllegalArgumentException("insert request");
        int inserted = Math.min(record.capacity() - record.stored(), requested);
        record = record.withAmount(key, record.amount(key) + inserted);
        return inserted;
    }

    public BE_CellRecord snapshot() { return record; }

    public TransactionResult commit() {
        transaction.write(handle, record);
        return transaction.commit();
    }

    @Override public void close() { transaction.close(); }
}
