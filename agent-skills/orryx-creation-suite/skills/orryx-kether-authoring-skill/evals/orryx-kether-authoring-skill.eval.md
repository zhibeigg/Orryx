# orryx-kether-authoring-skill eval

```json
{
  "skill": "orryx-kether-authoring-skill",
  "run": "py -3 scripts/run_pipeline.py --input {input} --output {output}",
  "criteria": [
    {
      "id": "component-fixed",
      "text": "Wrapper always returns kether component.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('component')=='kether'\" {output}"
    },
    {
      "id": "valid-status",
      "text": "Result status is ok or invalid.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('status') in ('ok','invalid')\" {output}"
    },
    {
      "id": "script-scanned",
      "text": "Every supplied script reaches the static scanner.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert any(x.get('code')=='KETHER_SCRIPT_SCANNED' for x in d.get('checks',[]))\" {output}"
    },
    {
      "id": "schema-contract",
      "text": "Output retains structured diagnostics and requirements instead of prose-only failure.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert isinstance(d.get('diagnostics'),list) and isinstance(d.get('requirements'),list)\" {output}"
    }
  ],
  "golden": [
    {"id": "case-1", "input": "golden/case-1/input.json", "expected": null, "expected_status": "pending-first-green"},
    {"id": "case-2", "input": "golden/case-2/input.json", "expected": null, "expected_status": "pending-first-green"},
    {"id": "case-3", "input": "golden/case-3/input.json", "expected": null, "expected_status": "pending-first-green"}
  ]
}
```

Cases cover skill context, asynchronous Station thread warnings, and an unbalanced experience-context negative script.
