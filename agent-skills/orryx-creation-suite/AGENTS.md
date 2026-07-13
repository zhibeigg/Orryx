# Orryx Creation Suite Agent Instructions

## Purpose

This directory contains the cross-platform Orryx Agent SKILL suite. It generates and validates Orryx Minecraft plugin configuration from production-backed templates and source-derived constraints.

## Activation

Use `/orryx-creation-suite` for requests spanning multiple Orryx domains. Route focused requests to the component skills under `skills/`.

## Required behavior

1. Treat configuration basenames as runtime IDs unless the relevant loader proves otherwise.
2. Inspect the current workspace before assigning new IDs.
3. Generate into staging and validate before materializing files.
4. Keep `Options.Name`, file ID, `Icon`, animation ID, controller ID and UI asset ID distinct.
5. Route event-driven passive behavior to the Station component.
6. Treat Status and Controller as one coordinated change.
7. Reject unsupported Aim generation outside Minecraft 1.12.2 unless the user explicitly requests a nonfunctional scaffold.
8. Never copy secrets from production examples.
9. Never automatically reload Orryx after writing files.
10. Report unresolved external assets and third-party plugin requirements.

## Commands

```bash
py -3 scripts/run_pipeline.py --input request.json --output result.json
py -3 scripts/materialize.py --input materialize-request.json --output materialize-result.json
py -3 scripts/build_action_schema.py --check
py -3 scripts/validate_suite.py
py -3 scripts/validate_skills.py
py -3 scripts/security_scan.py
py -3 scripts/run_evals.py --rollout
```

Use `python3` instead of `py -3` on Linux and macOS.

Read `SKILL.md` for routing and `references/` for detailed contracts. Component-specific instructions live in each component's `SKILL.md` and `AGENTS.md`.
