# Orchestrator source evidence

Reviewed: 2026-07-13.

## Repository structure

Orryx configuration is distributed across `skills/`, `jobs/`, `stations/`, `controllers/`, `status/`, `selectors.yml`, `experiences/` and `ui/`. Kether-bearing fields create cross-file references to skills, selectors, flags, controllers, animations and UI assets.

## Ordering rationale

- Validator must establish legal IDs and paths before any generator produces artifacts.
- Kether must be checked before domain stages because ability, job, station, combat and selector configs embed scripts.
- Ability/progression/job establish gameplay definitions consumed by station/combat/UI.
- Station and combat establish runtime behavior; selector provides reusable targeting; UI consumes the finalized IDs, icons and ordered bindings.

This yields the required order: validator → kether → ability → progression → job → station → combat → selector → ui.

## Operational boundary

Orryx exposes reload mechanisms in the plugin, but creation-suite output is a configuration planning/materialization concern. Server reload/restart is intentionally excluded so validation never mutates a live runtime.
