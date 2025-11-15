# Steam Workshop Release Checklist

Track the tasks needed before publishing GridMod to the Steam Workshop. Strike items once finished so we always know what’s left.

## Code & UX polish
- [x] Remove duplicate controls between the main Grid UI and the quick palette so every action lives in one place.
- [x] Sweep for dead code, debug prints, and unused assets (e.g., legacy blueprint helpers) to keep the jar lean.
- [x] Add defaults dropdown / tag the default pack so players can tell official vs user-created blueprints inside the quick palette.
- [x] Run a smoke test on a clean profile to confirm per-world paint data, defaults, and UI layouts survive restarts.

## Docs & marketing
- [x] Update README/`docs/todo.md` with the current feature list, controls, and bundled defaults.
- [x] Draft Workshop description and controls table
- [x] Prepare change log / release notes for version bump.
- [x] (screenshots/GIFs)

## Packaging & validation
- [ ] Bump `modVersion` / `mod.info`, regenerate the jar, and verify `resources/defaults/**` is present.
- [ ] Delete any local/testing blueprints from `mods-data/colox.gridmod/blueprints` before packaging.
- [ ] Build the release jar with `./gradlew buildModJar` and test it by running the client with only the jar in `build/jar/`.
- [ ] Assemble Workshop upload assets: jar, preview image, description text, tags, visibility settings.
- [ ] Upload via the Necesse Workshop uploader or Steam tools, then subscribe/download like a regular user to confirm it works.
