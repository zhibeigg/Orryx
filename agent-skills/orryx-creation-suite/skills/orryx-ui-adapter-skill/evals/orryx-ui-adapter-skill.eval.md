# Eval Spec: orryx-ui-adapter-skill

Binary regression gate for discriminated UI backends. Case 3 is a negative mixed/universal backend request.

## Criteria

1. Output is valid JSON.
2. Component is fixed to `ui`.
3. All five shared arrays exist.
4. Status agrees with error diagnostics.
5. Valid artifacts stay under one supported UI backend and contain no reload action.

## Spec

```json
{
  "skill": "orryx-ui-adapter-skill",
  "run": "py -3 scripts/run_pipeline.py --input {input} --output {output}",
  "criteria": [
    {"id": "valid-json", "text": "Output parses as JSON", "type": "command", "cmd": "py -3 -c \"import json,sys; json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8'))\" {output}"},
    {"id": "fixed-component", "text": "Launcher fixes component to ui", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d['component']=='ui'\" {output}"},
    {"id": "five-arrays", "text": "Shared result arrays are present", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert all(isinstance(d[k],list) for k in ('artifacts','references','requirements','diagnostics','checks'))\" {output}"},
    {"id": "status-consistent", "text": "Invalid status exactly matches error diagnostics", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert (d['status']=='invalid') == any(x.get('severity')=='error' for x in d['diagnostics'])\" {output}"},
    {"id": "single-backend-gate", "text": "Valid artifacts stay under one supported backend and contain no reload path", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); p=[a.get('path','') for a in d['artifacts']]; b={x.split('/')[1] for x in p if x.startswith('ui/')}; assert d['status']=='invalid' or (len(b)==1 and b.pop() in {'bukkit','dragoncore','germplugin','arcartx'}); assert all('reload' not in x.lower() for x in p)\" {output}"}
  ],
  "golden": [
    {"id": "case-1", "input": "golden/case-1/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"},
    {"id": "case-2", "input": "golden/case-2/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"},
    {"id": "case-3", "input": "golden/case-3/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"}
  ]
}
```
