# Orchestrator component contract

## Input

UTF-8 JSON object; launcher fixes `component` to `orchestrator`.

Recommended fields:

- `request`, `mode` (`plan` or `materialize`).
- `components`: requested domain payloads.
- `definitions`: typed IDs with planned paths.
- `references`: typed edges from consumer field to provider ID.
- `allowed_root`: permitted Orryx configuration tree.

## Fixed stages

The runtime must report stages in this order:

`validator`, `kether`, `ability`, `progression`, `job`, `station`, `combat`, `selector`, `ui`.

Unused stages are skipped, not removed or reordered.

## Gates

1. Validator checks request shape, names, duplicate IDs, paths and prohibited operations.
2. Kether gate checks all script-bearing fields before domain materialization.
3. Domain stages append artifacts/references/requirements/diagnostics/checks.
4. Cross-component reference graph is checked again after all requested stages.
5. Materialization is allowed only when every requested stage completed, all references resolve, diagnostics contain zero errors, paths are allowed and mode explicitly requests materialize.
6. Any reload/restart/server-command request remains outside the write plan.

## Output

Shared arrays:

- `artifacts`: ordered component artifacts or write plan.
- `diagnostics`: aggregated severity-coded findings with stage labels.
- `references`: ID nodes and reference edges.
- `checks`: ordered gate results, including the fixed stage order.
- `requirements`: unresolved dependencies, remediation, deployment, and materialization prerequisites.

For invalid input, diagnostics must include at least one error and artifacts must not contain materializable output. The orchestrator never reloads a server.
