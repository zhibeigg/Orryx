# Selector source evidence

Reviewed: 2026-07-13.

## Parser behavior

- `src/main/kotlin/org/gitee/orryx/core/parser/StringParser.kt` tokenizes entries beginning with `@` or `!@` and preserves order.
- Geometry selectors add targets to a Container; stream selectors process the current Container.
- Unknown selector names are reported as unregistered.
- `container()` uses `ensureSync`, demonstrating that selector evaluation may touch Bukkit state and is not arbitrary background work.

## Built-ins

`core/selector/geometry` contains range, sector, annular, ring, ray/rayhit, line, cylinder, cone, frustum, OBB, nearest, look-at, location/vector offset, floor and scatter implementations. `core/selector/stream` contains filters/transforms such as health, offset and other container operations.

## Presets

- `core/kether/actions/SelectorActions.kt` loads selector presets.
- `core/selector/presets/PresetsLoader.kt` and manager load preset Actions.
- `example/Orryx/selectors.yml` demonstrates positional `&v0`, `inline`, `container`, `merge`, `removeIf` and source/type context.

The skill therefore treats entry order, container type and `&vN` bindings as contract data rather than free-form text.
