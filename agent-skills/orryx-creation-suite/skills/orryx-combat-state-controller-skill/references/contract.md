# Combat component contract

## Input

UTF-8 JSON object; launcher fixes `component` to `combat`.

Recommended fields:

- `request`, `mode`, `target_backend`.
- `status`: `id`, `options`, `states`, `action`.
- `controller`: `id`, `layers`, `triggers`, `animations`, `assets`.
- `ids`: state/controller/animation definitions and `running` references.

Status options include Condition, Controller, CancelHeldEventWhenPlaying, CancelBukkitAttack, AttackSpeed and backend-specific animation state. State entries include Type, Connection/Check/Invincible, resource cost, Animation, Action and BlockAction.

## Semantic checks

- Status references an existing controller.
- Every `running` target is a defined state.
- General Attack has Key, Duration and a valid Connection when chaining.
- Block has Check within Duration, supported DamageType, SuccessKey dependency and nonnegative Invincible.
- Dodge has four directions, Duration, valid Invincible/Connection and explicit Spirit semantics.
- All ranges satisfy `0 <= start <= end <= duration`.
- Controller layers/triggers contain or depend on the animations referenced by status.
- Backend-specific controller fields are not silently converted.

## Output

Shared arrays:

- `artifacts`: status/controller proposals.
- `diagnostics`: severity-coded interval, reference and asset findings.
- `references`: state/running/controller/animation dependency graph.
- `checks`: binary status-controller and timeline checks.
- `requirements`: backend, asset, plugin, and follow-up requirements.

Any error blocks materialization. The component never reloads the server.
