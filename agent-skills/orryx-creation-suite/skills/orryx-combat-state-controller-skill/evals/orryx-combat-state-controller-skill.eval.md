# Eval Spec: orryx-combat-state-controller-skill

Binary regression gate for status/controller generation. Case 3 is the negative empty-state request.

## Criteria

1. Output is valid JSON.
2. Component is fixed to `combat`.
3. All five shared arrays exist.
4. Status agrees with error diagnostics.
5. A valid result contains both status and controller artifacts.

## Spec

```json
{
  "skill": "orryx-combat-state-controller-skill",
  "run": "py -3 scripts/run_pipeline.py --input {input} --output {output}",
  "criteria": [
    {"id": "valid-json", "text": "Output parses as JSON", "type": "command", "cmd": "py -3 -c \"import json,sys; json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8'))\" {output}"},
    {"id": "fixed-component", "text": "Launcher fixes component to combat", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d['component']=='combat'\" {output}"},
    {"id": "five-arrays", "text": "Shared result arrays are present", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert all(isinstance(d[k],list) for k in ('artifacts','references','requirements','diagnostics','checks'))\" {output}"},
    {"id": "status-consistent", "text": "Invalid status exactly matches error diagnostics", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert (d['status']=='invalid') == any(x.get('severity')=='error' for x in d['diagnostics'])\" {output}"},
    {"id": "paired-artifacts", "text": "Valid output contains status and controller YAML", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); p=[a.get('path','') for a in d['artifacts']]; assert d['status']=='invalid' or (any(x.startswith('status/') for x in p) and any(x.startswith('controllers/') for x in p)); assert all('reload' not in x.lower() for x in p)\" {output}"}
  ],
  "golden": [
    {"id": "case-1", "input": "golden/case-1/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"},
    {"id": "case-2", "input": "golden/case-2/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"},
    {"id": "case-3", "input": "golden/case-3/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"}
  ]
}
```
