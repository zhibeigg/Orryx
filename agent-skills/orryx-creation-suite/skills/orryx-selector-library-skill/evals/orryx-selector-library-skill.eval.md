# Eval Spec: orryx-selector-library-skill

Binary regression gate for selector preset generation. Case 3 is the negative empty selector map.

## Criteria

1. Output is valid JSON.
2. Component is fixed to `selector`.
3. All five shared arrays exist.
4. Status agrees with error diagnostics.
5. A valid result produces only `selectors.yml` and never a reload artifact.

## Spec

```json
{
  "skill": "orryx-selector-library-skill",
  "run": "py -3 scripts/run_pipeline.py --input {input} --output {output}",
  "criteria": [
    {"id": "valid-json", "text": "Output parses as JSON", "type": "command", "cmd": "py -3 -c \"import json,sys; json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8'))\" {output}"},
    {"id": "fixed-component", "text": "Launcher fixes component to selector", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d['component']=='selector'\" {output}"},
    {"id": "five-arrays", "text": "Shared result arrays are present", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert all(isinstance(d[k],list) for k in ('artifacts','references','requirements','diagnostics','checks'))\" {output}"},
    {"id": "status-consistent", "text": "Invalid status exactly matches error diagnostics", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert (d['status']=='invalid') == any(x.get('severity')=='error' for x in d['diagnostics'])\" {output}"},
    {"id": "selector-artifact-gate", "text": "Valid output contains selectors.yml only and no reload artifact", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); p=[a.get('path','') for a in d['artifacts']]; assert d['status']=='invalid' or p==['selectors.yml']; assert all('reload' not in x.lower() for x in p)\" {output}"}
  ],
  "golden": [
    {"id": "case-1", "input": "golden/case-1/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"},
    {"id": "case-2", "input": "golden/case-2/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"},
    {"id": "case-3", "input": "golden/case-3/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"}
  ]
}
```
