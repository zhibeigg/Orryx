# orryx-ability-authoring-skill eval

```json
{
  "skill": "orryx-ability-authoring-skill",
  "run": "py -3 scripts/run_pipeline.py --input {input} --output {output}",
  "criteria": [
    {
      "id": "component-fixed",
      "text": "Wrapper always returns ability component.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('component')=='ability'\" {output}"
    },
    {
      "id": "skill-artifact",
      "text": "A valid key produces a skills YAML artifact even when compatibility diagnostics invalidate it.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert any(a.get('path','').startswith('skills/') and a.get('path','').endswith('.yml') for a in d.get('artifacts',[]))\" {output}"
    },
    {
      "id": "valid-status",
      "text": "Result status is ok or invalid.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('status') in ('ok','invalid')\" {output}"
    },
    {
      "id": "passive-or-check",
      "text": "Generation records ability checks or passive Station requirements.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('checks') or d.get('requirements')\" {output}"
    }
  ],
  "golden": [
    {"id": "case-1", "input": "golden/case-1/input.json", "expected": null, "expected_status": "pending-first-green"},
    {"id": "case-2", "input": "golden/case-2/input.json", "expected": null, "expected_status": "pending-first-green"},
    {"id": "case-3", "input": "golden/case-3/input.json", "expected": null, "expected_status": "pending-first-green"}
  ]
}
```

Cases cover Direct, Passive plus Station intent, and a DirectAim request rejected on Minecraft 1.20.4.
