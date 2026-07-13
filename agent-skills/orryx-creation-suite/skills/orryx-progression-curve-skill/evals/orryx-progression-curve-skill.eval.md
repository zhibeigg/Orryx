# orryx-progression-curve-skill eval

```json
{
  "skill": "orryx-progression-curve-skill",
  "run": "py -3 scripts/run_pipeline.py --input {input} --output {output}",
  "criteria": [
    {
      "id": "component-fixed",
      "text": "Wrapper always returns progression component.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('component')=='progression'\" {output}"
    },
    {
      "id": "valid-status",
      "text": "Result status is ok or invalid.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('status') in ('ok','invalid')\" {output}"
    },
    {
      "id": "experience-or-error",
      "text": "Valid ranges produce experience YAML; invalid ranges produce diagnostics.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('status')=='invalid' or any(a.get('path','').startswith('experiences/') for a in d.get('artifacts',[]))\" {output}"
    },
    {
      "id": "line-chart-protocol",
      "text": "Valid simulations emit a deterministic line-chart artifact.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('status')=='invalid' or any(a.get('kind')=='line-chart' or a.get('path','').endswith('-line-chart.json') for a in d.get('artifacts',[]))\" {output}"
    },
    {
      "id": "level-simulation",
      "text": "Valid simulations expose per-level check details.",
      "type": "command",
      "cmd": "py -3 -c \"import json,sys; d=json.load(open(sys.argv[1].strip(chr(39)+chr(34)),encoding='utf-8')); assert d.get('status')=='invalid' or any(isinstance(x.get('details'),dict) and x['details'].get('perLevel') for x in d.get('checks',[]))\" {output}"
    }
  ],
  "golden": [
    {"id": "case-1", "input": "golden/case-1/input.json", "expected": null, "expected_status": "pending-first-green"},
    {"id": "case-2", "input": "golden/case-2/input.json", "expected": null, "expected_status": "pending-first-green"},
    {"id": "case-3", "input": "golden/case-3/input.json", "expected": null, "expected_status": "pending-first-green"}
  ]
}
```

Cases cover the repository's linear default pattern, an exponential curve, and an invalid reversed level range.
