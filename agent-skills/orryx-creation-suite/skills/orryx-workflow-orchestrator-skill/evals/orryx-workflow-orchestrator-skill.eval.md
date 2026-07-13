# Eval Spec: orryx-workflow-orchestrator-skill

Binary regression gate for ordered multi-component orchestration. Case 1 supplies the full validator→kether→ability→progression→job→station→combat→selector→ui order. Case 3 is a materialize/reload request with missing references and an unsupported step; it must be invalid.

## Criteria

1. Output is valid JSON.
2. Component is fixed to `orchestrator`.
3. All five shared arrays exist.
4. Status agrees with error diagnostics.
5. No artifact is a reload/restart action; invalid output contains errors.

## Spec

```json
{
  "skill": "orryx-workflow-orchestrator-skill",
  "run": "py -3 scripts/run_pipeline.py --input {input} --output {output}",
  "criteria": [
    {"id": "valid-json", "text": "Output parses as JSON", "type": "command", "cmd": "py -3 -c \"import json,sys; json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8'))\" {output}"},
    {"id": "fixed-component", "text": "Launcher fixes component to orchestrator", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d['component']=='orchestrator'\" {output}"},
    {"id": "five-arrays", "text": "Shared result arrays are present", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert all(isinstance(d[k],list) for k in ('artifacts','references','requirements','diagnostics','checks'))\" {output}"},
    {"id": "status-consistent", "text": "Invalid status exactly matches error diagnostics", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert (d['status']=='invalid') == any(x.get('severity')=='error' for x in d['diagnostics'])\" {output}"},
    {"id": "no-live-reload", "text": "Artifacts never perform reload/restart and invalid results expose diagnostics", "type": "command", "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); s=' '.join(a.get('path','')+' '+a.get('content','') for a in d['artifacts']).lower(); assert 'reloadserver' not in s and 'restartserver' not in s; assert d['status']=='ok' or len(d['diagnostics'])>0\" {output}"}
  ],
  "golden": [
    {"id": "case-1", "input": "golden/case-1/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"},
    {"id": "case-2", "input": "golden/case-2/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"},
    {"id": "case-3", "input": "golden/case-3/input.json", "expected": null, "split": "val", "expected_status": "pending-first-green"}
  ]
}
```
