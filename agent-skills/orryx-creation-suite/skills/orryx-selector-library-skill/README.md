# orryx-selector-library-skill

Orryx 几何/流式 selector 与 `selectors.yml` 预设文档包，覆盖 `&vN`、container、顺序和过滤器。

## 运行

```text
py -3 scripts/run_pipeline.py --input assets/request.example.json --output selector-report.json
py -3 scripts/run_evals.py --validate
py -3 scripts/run_evals.py --rollout
```

启动器支持套件源码树与安装后的同级 runtime。输入 JSON 描述 inline/preset 请求，输出共享五数组报告。

## 安装

使用完整套件根安装器；本目录脚本仅转发。单独提取缺少 `orryx-creation-suite-runtime` 时会明确提示。

## Golden

3 个 rollout 案例覆盖内联扇形、带 `&v0/&v1` 的预设，以及过滤器顺序/缺参负例。
