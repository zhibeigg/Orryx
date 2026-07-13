# Station source evidence

Reviewed: 2026-07-13.

## Loader behavior

- `src/main/kotlin/org/gitee/orryx/core/station/stations/StationLoader.kt`
  - `Options.Event` is required and normalized to uppercase.
  - Weight defaults to 0; Priority defaults to NORMAL; IgnoreCancelled and Async default false.
  - Actions is required; Variables keys are normalized uppercase.
- `StationLoaderManager.kt`
  - stations are grouped by Event and Priority, then sorted by descending Weight.
  - `Async: true` launches execution in `pluginScope`; false runs inline with the listener.
  - script lifecycle invokes trigger onStart/onEnd and per-player running-space tracking.

## Direction and phase cases

- `example/Orryx/stations/拳修/拳修内劲.yml`: `Player Damage Pre` modifies attacker-side `&event[damage]`.
- `example/Orryx/stations/拳修/凝灵盾受击.yml`: `Player Damaged Pre` cancels/reduces defender-side damage, updates shield Flag and triggers break retaliation.
- `example/Orryx/stations/剑修/刹那冷却.yml`: Flag Change Post starts cooldown when the duration Flag disappears.
- `example/Orryx/stations/剑修/咒恶之锋.yml`: `Player Damage Post` skips cancelled events, collects defender and accumulates settled damage.

## Safety inference

Because Async directly changes where the script runs, event mutation and Bukkit object actions cannot be assumed safe. The skill therefore defaults station mechanics to synchronous execution unless the entire action chain is known async-safe.
