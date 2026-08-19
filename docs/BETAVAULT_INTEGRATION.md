# BetaVault Integration

Status: host integration GO.

BetaEnergistics owns `betaenergistics.storage-cell/1`, its deterministic codec,
capacity/type invariants, and mutation facade. BetaVault owns the stable
object ID, save identity, checksummed object store, transactions, journal, and
canonical Minecraft reference.

The `--integration` gate compiles both repositories at their pinned revisions
and proves:

- a copied/TMI physical representation retains one logical reference;
- two stale aliases cannot both extract the same contents;
- the accepted mutation conserves the remaining count;
- save/restart resolves the same logical cell and contents;
- a reference from another save is rejected.

## Remaining runtime boundary

Vanilla Beta 1.7.3 ItemStack persists and transmits only item ID, count, and a
16-bit damage value. The host adapter therefore does not yet replace disk
damage in gameplay. A controlled Worldline extension must carry the canonical
reference through ItemStack construction, copy/split, NBT, equality, inventory
packets, container clicks, drops, and TMI creation. That extension needs a
differential runtime smoke before the legacy registries can be retired.
