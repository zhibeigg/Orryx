# Agent instructions

- 仅使用五类：Passive、Direct、DirectAim、Pressing、PressingAim。
- 按 basename 生成技能 ID，Name/Icon 只作显示资源。
- 建立 Description、Variables、Actions 的数值与行为对应关系。
- Passive 声明事件效果时生成 Station scaffold/reference，不宣称 Passive 自动执行 Actions。
- DirectAim/PressingAim 在非 1.12.2 上必须报不兼容。
- 主线程敏感动作使用最小同步区段，不建议阻塞。
- 优先遵循破空斩、蓄意轰拳、内劲与拳修内劲的真实模式。
- 只返回 artifact，不写配置、不 reload。
