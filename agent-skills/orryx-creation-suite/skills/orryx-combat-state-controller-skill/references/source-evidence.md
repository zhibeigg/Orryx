# Combat source evidence

Reviewed: 2026-07-13.

## Status model

- `src/main/kotlin/org/gitee/orryx/module/state/Status.kt` loads CancelBukkitAttack and AttackSpeed script behavior.
- `states/GeneralAttackState.kt` and `PressGeneralAttackState.kt` parse Connection ranges and use attack speed when running.
- `states/BlockState.kt` loads Check, Invincible and optional BlockAction.
- `states/DodgeState.kt` loads Invincible and Connection ranges, and applies invincibility in Tick-derived milliseconds.
- `core/kether/actions/StateActions.kt` implements the `running` action against named states.

## Production cases

- `example/Orryx/status/剑修.yml` couples Condition, Controller `长剑`, attack-speed calculation, three General Attack states, Block, Dodge/Roll states and input routing.
- Its `running` calls form explicit transitions; Check/Connection/Invincible values are defined relative to Animation Duration.
- `example/Orryx/controllers/长剑.yml` defines animation Layers and client Triggers for initialization, animation start/end, idle, walking, sprinting and sneaking.

## Derived rule

Status and controller IDs/animations form one dependency graph. A valid server state definition can still be unusable when the client controller or assets are absent, so both sides are validated together.
