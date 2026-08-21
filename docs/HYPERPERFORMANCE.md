# Hyperperformance architecture

BetaEnergistics treats item discovery as incremental indexing. Normal queries
never walk the network graph or rescan every inventory.

```text
provider mutation -> provider delta -> shard aggregate -> global catalog
                                                |-> immutable terminal view
extraction request <- provider directory <------|
```

The implementation is deliberately split at stable boundaries:

- `BE_IncrementalCatalog` owns aggregate counts, provider generations, dirty
  reconciliation, stable 128-provider shards, and item-to-shard routing.
- `BE_CatalogBootstrap` admits providers under an explicit per-tick budget.
- `BE_CatalogTransaction` publishes a logical batch atomically.
- `BE_CatalogSnapshot` is immutable; the terminal never iterates live storage.
- `BE_CatalogView` caches normalized search and sorting by catalog generation.
- `BE_WorkSnapshot` exposes structural work without using wall-clock timing.
- `BE_CellCatalogBridge` publishes committed BetaVault cell changes as one
  catalog transaction and reconstructs the provider from its durable record.

Provider adapters declare `PUSH`, `DIRTY_NOTIFY`, or `POLL`. Native storage and
BetaVault cells use exact deltas. A dirty notification reconciles only its
provider. Polling is reserved for legacy inventories and must be budgeted.
Full-network scanning is not a normal-operation fallback.

## Qualification milestones

| ID | Contract | Current evidence |
|---|---|---|
| BE-X1 | Legacy ModLoader runtime provider | Extension contract documented; official runtime qualification is blocked until a legitimate RetroMCP/ModLoader input and test JAR are supplied. |
| BE-X2 | Inventory mutation coverage | Worldline fail-closed mutation manifest is available; every real legacy boundary still requires promoted mapping evidence. |
| BE-X3 | Chunk and tile lifecycle | Worldline exposes optional chunk load/unload and tile-observation capabilities. The legacy adapter must implement them before its specs can pass. |
| BE-X4 | Work counters and traces | Provider scans, deltas, shard lookups, idle ticks, snapshots, and transactions are observable. |
| BE-X5 | Invariants | Conservation, non-negative counts, atomic failure, immutable snapshots, generation invalidation, and no-full-scan contracts pass. |
| BE-X6 | Scale matrix | 100,000 providers, 10,000 idle ticks, 10,000 lookups, and a single mutation pass with zero idle provider scans. |
| BE-X7 | Restart and soak | 50,000 deterministic mutations match a reference model; BetaVault restart reconstructs the same catalog. Official multiplayer/runtime soak remains gated by BE-X1. |

The records under `optimizations/catalog` remain `candidate` and disabled by
default. `new BE_CompositeStorage()` retains legacy aggregation;
`new BE_CompositeStorage(true)` opts into the candidate index. Host tests prove
the architecture, not vanilla runtime equivalence.
Promotion requires external Worldline specs plus serialized official smokes.

## Performance contract

Expected item lookup and exact delta application are constant-time hash work.
Search scales with distinct item types, not stored stacks. Idle work must not
grow proportionally when a fixture grows from 1,000 to 100,000 providers.
Wall-clock numbers are secondary diagnostics; `BE_WorkSnapshot` counters are
the deterministic acceptance criteria.

Structural sharing and a Butter virtual grid are downstream optimizations.
They should be added only after a benchmark shows snapshot copying or widget
construction is the next bottleneck.
