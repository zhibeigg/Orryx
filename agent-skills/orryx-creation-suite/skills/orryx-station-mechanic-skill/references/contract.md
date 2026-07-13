# Station component contract

## Input

UTF-8 JSON object. The launcher overwrites `component` with `station`.

Recommended fields:

- `request`: human-readable objective.
- `mode`: `plan`, `validate`, or `materialize` (materialize is still gated).
- `ids`: definitions and references relevant to skill/job/station/flag.
- `station`: proposed station object.
  - `id`, `event`, `phase`, `direction`.
  - `priority`, `weight`, `ignore_cancelled`, `async`.
  - `actions`, `flags`, `containers`, `accumulators`, `cooldowns`.
- `constraints`: target root, permitted paths, thread assumptions.

## Semantic checks

1. `Player Damage` means attacker context; `Player Damaged` means defender context.
2. Pre may modify/cancel damage; Post may observe settled damage.
3. Priority is a Bukkit enum; Weight is integer and orders stations descending within a group.
4. Async is rejected/warned when Actions touch event state, Bukkit entities/world/inventory, movement, damage, sound or particles.
5. Post accumulation checks cancellation, and all Flag/container/accumulator lifetimes have cleanup.
6. Shield branches handle full absorb, partial absorb, zero/break and repeated-trigger prevention.

## Output

`run_contract` returns exactly these arrays:

- `artifacts`: proposed YAML/config artifacts; empty on invalid materialization.
- `diagnostics`: `{severity, code, message, path?}` entries.
- `references`: IDs, source paths, event fields and dependency edges.
- `checks`: binary check results such as direction, phase and thread safety.
- `requirements`: thread, dependency, deployment, or follow-up requirements.

Top-level metadata may identify the component/status, but consumers must rely on the five arrays. Any diagnostic with severity `error` prevents materialization.

## Materialization boundary

The component never reloads Orryx or a Bukkit server. A materialize request can only propose/write allowed configuration after errors are zero; deployment remains external.
