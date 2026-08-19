# Standalone Repository Comparison

Compared on 2026-08-18.

The `mods/authoral/beta-energistics` entry in `minecraft-beta-modding` records
Gitlink commit `5648e6bb3b4d9a2bad864a6e6c6d8946ea444a6e`. The standalone repository's
`main` branch resolves to that exact commit. Their tracked BetaEnergistics
trees are therefore byte-identical; there is no newer monorepo-only version to
merge.

The old workspace supplied location rather than ownership:

- the mod was already a submodule with its own history;
- `scripts/transpile.sh` inferred shared libraries through `../../../libraries`;
- local ignored RetroMCP and test directories supplied the game toolchain;
- no standalone canonical verification existed.

The standalone baseline removes the positional library assumption. Dependency
revisions are explicit, checkout paths are injected, and `Verify.java` owns a
portable host gate. The historical submodule can be removed from the monorepo
after downstream references are updated; retaining it is optional and should
not be treated as dependency management.

The following integration milestone now compiles the real storage-cell domain
against pinned BetaVault sources. The repository still does not migrate the
whole mod to StationAPI, compile every Minecraft-linked class, or extend the
vanilla ItemStack transport; those need executable runtime gates of their own.
