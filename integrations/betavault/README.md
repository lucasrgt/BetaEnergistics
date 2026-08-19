# BetaVault integration

This module owns BetaEnergistics storage-cell schemas and invariants while
BetaVault owns identity, deterministic bytes, transactions, journaling, and
save binding.

Physical copies carry one canonical `VaultReference`; they do not mint copied
contents. Concurrent mutations aliasing that reference use optimistic
transactions, so a stale duplicate extraction fails instead of duplicating
items.

The host integration gate proves this contract against the pinned real
BetaVault sources. It does not claim that vanilla Beta 1.7.3 `ItemStack`
already carries the reference: vanilla persists and transmits only item ID,
count, and 16-bit damage. Extending its save, copy/split, equality, and packet
boundaries is a controlled Worldline runtime milestone.
