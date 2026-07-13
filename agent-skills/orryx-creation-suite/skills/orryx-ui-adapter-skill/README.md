# orryx-ui-adapter-skill

Orryx UI 判别变体文档包，分别处理 bukkit、dragoncore、germplugin 与 arcartx，不做盲目互转。

## 运行

```text
py -3 scripts/run_pipeline.py --input assets/request.example.json --output ui-report.json
py -3 scripts/run_evals.py --validate
py -3 scripts/run_evals.py --rollout
```

启动器优先使用源码树 `shared/orryx_toolkit`，安装后使用技能同级 `orryx-creation-suite-runtime`。

## 验证重点

- 唯一 variant 与后端专属 schema
- placeholder 与有序数组索引对齐
- skill icon、GUI、图片、字体、声音等资产依赖
- 跨后端能力差异和不可保真项

本目录安装脚本只调用套件根安装器；组件单独提取时会明确失败。
