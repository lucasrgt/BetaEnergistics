package betaenergistics.storage.index;

/** Mutable counters owned by one incremental catalog. */
final class BE_WorkCounters {
    private final long[] values = new long[11];

    void providerSnapshot() { values[0]++; }
    void providerEntries(long count) { values[1] += count; }
    void delta() { values[2]++; }
    void reconcile() { values[3]++; }
    void fullScan() { values[4]++; }
    void countQuery() { values[5]++; }
    void directoryQuery() { values[6]++; }
    void shardLookup() { values[7]++; }
    void idleTick() { values[8]++; }
    void snapshot() { values[9]++; }
    void transaction() { values[10]++; }
    BE_WorkSnapshot snapshotValue() { return new BE_WorkSnapshot(values.clone()); }
}
