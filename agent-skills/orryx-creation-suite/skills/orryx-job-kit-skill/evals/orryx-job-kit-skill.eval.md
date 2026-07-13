# orryx-job-kit-skill eval

```json
{
  "skill": "orryx-job-kit-skill",
  "run": "py -3 scripts/run_pipeline.py --input {input} --output {output}",
  "criteria": [
    {
      "id": "component-fixed",
      "text": "Wrapper always returns job component.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('component')=='job'\" {output}"
    },
    {
      "id": "valid-status",
      "text": "Result status is ok or invalid.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('status') in ('ok','invalid')\" {output}"
    },
    {
      "id": "job-or-error",
      "text": "Valid keys produce job YAML; invalid keys produce diagnostics.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('status')=='invalid' or any(a.get('path','').startswith('jobs/') for a in d.get('artifacts',[]))\" {output}"
    },
    {
      "id": "no-parent-job-fiction",
      "text": "Generated YAML never invents ParentJob.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert all('ParentJob' not in a.get('content','') for a in d.get('artifacts',[]))\" {output}"
    },
    {
      "id": "advancement-requirement",
      "text": "The second-job case records an external scaffold requirement.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); second=any(a.get('path')=='jobs/剑宗.yml' for a in d.get('artifacts',[])); assert (not second) or d.get('requirements')\" {output}"
    }
  ],
  "golden": [
    {"id": "case-1", "input": "golden/case-1/input.json", "expected": null, "expected_status": "pending-first-green"},
    {"id": "case-2", "input": "golden/case-2/input.json", "expected": null, "expected_status": "pending-first-green"},
    {"id": "case-3", "input": "golden/case-3/input.json", "expected": null, "expected_status": "pending-first-green"}
  ]
}
```

Cases cover a normal job kit, a conservative second-job scaffold, and an invalid path-like job ID.
