# GridMod for Necesse

GridMod is a client-side quality-of-life mod that makes construction feel like working inside a level editor. It overlays tile/chunk grids, adds a multi-layer paint system with blueprint saving/loading, and surfaces settlement tools so you can stage builds before committing resources.

## Highlights

- **Grid overlay:** Toggle tile, chunk, and sub-chunk lines with individual opacity/spacing controls so you can eyeball perfect spacing for roads, farms, and settlement bounds.
- **Three-layer paint system:** Paint floors, walls, and props on independent layers, hide/show them, and erase without touching underlying work. Brush sizes 1–32 plus erase-on-hold make sketching layouts quick.
- **Quick palette UI:** A left-side overlay exposes paint controls, brush size, blueprint management, settlement helpers, and color pickers without opening separate menus.
- **Blueprint workflow:** Save local or global `.gridpaint` files, rotate/flip them on placement, and load bundled defaults (Guidehouse, Crafting outline, etc.) from a read-only dropdown.
- **Settlement helpers:** Preview settlement tiers, snap the flag to your position, and visualize the current boundary box in real time.

## Installation

1. Download the latest release jar (or build it yourself—see below).
2. Drop the jar into `Steam/steamapps/common/Necesse/mods/`.
3. Launch Necesse. GridMod appears in the in-game Mods list and auto-loads for single-player and client-side multiplayer (servers do not need the mod).

## Building from source

Prerequisites:

- Java 8/17 JDK (the repo uses the OpenJDK 17 bundle under `OpenJDK17U-…`).
- A local Necesse install; `build.gradle` points to `F:/Steam/steamapps/common/Necesse`.

Steps:

```sh
./gradlew buildModJar
```

The jar is emitted to `build/jar/GridMod-<gameVersion>-<modVersion>.jar`. Copy that into your Necesse `mods/` folder for testing.

### Development tasks

- `./gradlew runClient` – launches the Steam client with this mod jar.
- `./gradlew runDevClient` – launches a dev-auth client (requires a dev server).
- `./gradlew runServer` – starts a dedicated server with the built jar.

## Controls & usage tips

- Bind at least: **Open Grid UI**, **Toggle Grid**, **Paint: Toggle**, **Paint: Clear**, **Paint: Toggle overlay** under `Controls → Mods → GridMod`.
- Use the quick palette buttons to rotate/flip blueprint ghosts while placing.
- Selection tools (Rect / Edge / Edge+Fill / All) operate per layer so you can isolate specific paint groups before saving or moving them.
- Default paint/grid colors and opacities live under the Grid UI’s color tabs; use “Reset” to restore curated defaults if you experiment.

## Documentation & roadmap

- `docs/workshop-description.md` – copy-ready Workshop text with full feature rundown.
- `docs/release-notes.md` – change log per version.
- `docs/todo.md` – roadmap items and future feature ideas (shared paint, advanced brushes, etc.).

Contributions or bug reports are welcome