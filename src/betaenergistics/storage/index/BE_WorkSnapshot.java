package betaenergistics.storage.index;

/** Immutable structural-work counters for regression assertions. */
public final class BE_WorkSnapshot {
    public final long providerSnapshots;
    public final long providerEntriesScanned;
    public final long deltasApplied;
    public final long dirtyReconciles;
    public final long fullScans;
    public final long countQueries;
    public final long directoryQueries;
    public final long shardLookups;
    public final long idleTicks;
    public final long snapshotsPublished;
    public final long transactionsCommitted;

    BE_WorkSnapshot(long[] values) {
        providerSnapshots = values[0]; providerEntriesScanned = values[1];
        deltasApplied = values[2]; dirtyReconciles = values[3]; fullScans = values[4];
        countQueries = values[5]; directoryQueries = values[6]; shardLookups = values[7];
        idleTicks = values[8]; snapshotsPublished = values[9];
        transactionsCommitted = values[10];
    }
}
