# Selector component contract

## Input

UTF-8 JSON object; launcher fixes `component` to `selector`.

Recommended fields:

- `request`, `mode`: `inline`, `preset`, or `validate`.
- `selector`: inline expression or structured entries.
- `preset`: `id`, `actions`, and ordered `parameters`.
- `container`: source type, expected target type and composition operation.
- `filters`: self/type/team/pvp/health/order/limit requirements.

## Semantic checks

- Geometry entries add entity/location targets; stream entries process the existing container.
- Entry order is preserved. A stream filter without a prior source is an error unless an external container is declared.
- Geometry parameters are finite, nonnegative where required and use the selector-specific order.
- Preset placeholders are contiguous `&v0..&vN` and each has type, unit and description.
- Unknown selectors, hidden parameters and entity/location type mismatches are errors.
- Enemy targeting explicitly addresses self, team/PVP and ARMOR_STAND where applicable.
- Reusable presets avoid unrelated damage/cooldown/sound side effects.

## Output

Shared arrays:

- `artifacts`: normalized inline string or selectors.yml preset.
- `diagnostics`: parameter, ordering, type and filter findings.
- `references`: preset IDs, parameter bindings and selector dependencies.
- `checks`: binary geometry/stream/container checks.
- `requirements`: argument, caller-context, and safe-reuse requirements.

No server reload is executed.
