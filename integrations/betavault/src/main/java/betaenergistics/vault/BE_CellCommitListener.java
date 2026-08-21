package betaenergistics.vault;

/** Receives one durable cell transition after its optimistic commit succeeds. */
public interface BE_CellCommitListener {
    void committed(BE_CellRecord before, BE_CellRecord after);
}
