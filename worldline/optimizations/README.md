# BetaEnergistics Optimization Metadata

This directory is the source of truth for optimization metadata owned by
BetaEnergistics. Each stable `betaenergistics.*` ID maps to one implementation
decision in `catalog/`; `TEMPLATE.properties` is the starting point for new
records.

The records use the neutral `worldline.optimization.v1` schema. They document
status, behavioral delta, risks, rollback, implementation symbols, and bounded
evidence without introducing a Worldline runtime dependency or enabling a
feature.

Run the repository-owned validation from the repository root:

```text
java tools/harness/OptimizationRecordsCheck.java
```
