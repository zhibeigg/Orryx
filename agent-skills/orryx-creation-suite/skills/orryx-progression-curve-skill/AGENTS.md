# Agent instructions

- 同时处理经验、法力、精力与升级点，不把它们混成一个量纲。
- 对 Min..Max 每一级求值并累计，必须包含边界级。
- 经验引用使用 experiences 文件 basename。
- 报告负值、下降、非整数经验、突变和公式不可求值。
- 折线图使用 `application/vnd.orryx.line-chart+json` 协议，不生成不可审计图片。
- chart 的 x/series 等长、level 升序、数值有限且无时间戳。
- 以 `experiences/default.yml` 和 `jobs/剑修.yml` 为事实基线。
- 不运行服务器、不 reload、不写工作区。
