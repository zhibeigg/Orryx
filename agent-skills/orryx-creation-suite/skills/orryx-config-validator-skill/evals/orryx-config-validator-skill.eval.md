# orryx-config-validator-skill eval

```json
{
  "skill": "orryx-config-validator-skill",
  "run": "py -3 scripts/run_pipeline.py --input {input} --output {output}",
  "criteria": [
    {
      "id": "component-fixed",
      "text": "Wrapper always returns validator component.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('component')=='validator'\" {output}"
    },
    {
      "id": "valid-status",
      "text": "Result status is binary ok or invalid.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('status') in ('ok','invalid')\" {output}"
    },
    {
      "id": "diagnostics-array",
      "text": "Diagnostics and checks are arrays.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert isinstance(d.get('diagnostics'),list) and isinstance(d.get('checks'),list)\" {output}"
    },
    {
      "id": "deterministic-provenance",
      "text": "Output records the suite version and a stable input digest.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); p=d.get('provenance',{}); assert p.get('suiteVersion') and len(p.get('inputDigest',''))==64\" {output}"
    }
  ],
  "golden": [
    {"id": "case-1", "input": "golden/case-1/input.json", "expected": null, "expected_status": "pending-first-green"},
    {"id": "case-2", "input": "golden/case-2/input.json", "expected": null, "expected_status": "pending-first-green"},
    {"id": "case-3", "input": "golden/case-3/input.json", "expected": null, "expected_status": "pending-first-green"}
  ]
}
```

Case 1 is an empty valid workspace. Cases 2 and 3 exercise real Orryx trees and are expected to surface reference, ordering, or basename diagnostics.
