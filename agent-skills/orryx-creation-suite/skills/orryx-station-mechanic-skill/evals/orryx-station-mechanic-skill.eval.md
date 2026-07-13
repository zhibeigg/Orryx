# Eval Spec: orryx-station-mechanic-skill

Binary regression gate for the station launcher and shared contract. Case 3 is a negative case with invalid Priority and missing Actions; it must still return a structured invalid report.

## Criteria

1. Output is valid JSON.
2. Component is fixed to `station`.
3. All five shared result arrays exist.
4. `status` agrees with error diagnostics.
5. Successful results contain a station artifact and no reload artifact.

## Spec

```json
{
  "skill": "orryx-station-mechanic-skill",
  "run": "py -3 scripts/run_pipeline.py --input {input} --output {output}",
  "criteria": [
    {"id": "valid-json", "text": "Output parses as JSON", "type": "command", "cmd": "py -3 -c \"import json,sys; json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8'))\" {output}"},
    {"id": "fixed-component", "text": "Launcher fixes component to station", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d['component']=='station'\" {output}"},
    {"id": "five-arrays", "text": "Shared result arrays are present", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert all(isinstance(d[k],list) for k in ('artifacts','references','requirements','diagnostics','checks'))\" {output}"},
    {"id": "status-consistent", "text": "Invalid status exactly matches error diagnostics", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert (d['status']=='invalid') == any(x.get('severity')=='error' for x in d['diagnostics'])\" {output}"},
    {"id": "station-artifact-gate", "text": "Valid output has station YAML and never emits reload artifacts", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d['status']=='invalid' or any(a.get('path','').startswith('stations/') for a in d['artifacts']); assert all('reload' not in a.get('path','').lower() for a in d['artifacts'])\" {output}"}
  ],
  "golden": [
    {"id": "case-1", "input": "golden/case-1/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"},
    {"id": "case-2", "input": "golden/case-2/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"},
    {"id": "case-3", "input": "golden/case-3/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"}
  ]
}
```
