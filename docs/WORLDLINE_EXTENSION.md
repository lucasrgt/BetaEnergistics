# Worldline extension for BetaEnergistics

The extension is an adapter, not a Minecraft dependency in product code. A
consumer compiles Java 8 specs through the `dev.worldline.test` Gradle plugin;
the Worldline runner and runtime provider stay on the test side.

Host-only specifications live under `tests/worldline/src/test/java` and run
without Minecraft:

```text
tests/worldline/gradlew.bat -p tests/worldline worldlineDoctor worldlineTest
```

An official legacy provider must implement `TestRuntimeProvider` and return a
fresh `TestRuntimeSession` for every attempt. Its runtime should additionally
implement these optional capabilities when a test needs them:

- `ChunkLifecycleRuntime` for explicit load and unload;
- `TileObservableRuntime` for neutral tile observations;
- `RuntimeWorkObservable` for deterministic performance counters.

The provider owns ModLoader, mappings, generated Minecraft state, and process
cleanup. It must never expose obfuscated or RetroMCP names through the spec API.
It must fail closed when its exact runtime, mod artifact, mapping evidence, or
official JAR is absent. Official runtimes are serialized by the Worldline lock.

Before removing polling from an inventory adapter, publish a
`MutationCoverage` manifest covering its verified slot writes, decrement,
container transfer, NBT load, chunk attach/detach, and external automation
paths. Names in that list are requirements, not claimed vanilla mappings;
each real entry needs its promoted Worldline mapping ID and evidence.

The current repository deliberately does not vendor a provider or Minecraft
binary. BE-X1 and the runtime half of BE-X2/BE-X3/BE-X7 remain blocked until a
legitimate legacy toolchain is configured.
