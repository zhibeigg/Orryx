# Orryx 在线编辑器

## 启用与访问

```yaml
Editor:
  License: "中心签发的 Orryx License"
```

`Editor.License` 是中心用于识别并校验 Orryx 服务器身份的凭据。配置该身份凭据后插件会自动连接中心，不再需要额外开关。License 到期不影响实时编辑器，但 License 不存在、被禁用或绑定 IP 与当前服务器 IP 不匹配时，中心仍会拒绝连接。拥有 `Orryx.Command.Editor` 权限的玩家可执行 `/orryx edit`；该命令仅允许玩家执行，控制台不能签发玩家访问入口。

插件只在中心确认 Token 注册成功后发送可点击消息。游戏聊天仅显示“点击打开编辑器 · 5 分钟内有效”，不会显示或复制完整 URL；点击事件携带实际入口。

## 固定中心地址

中心地址由插件代码固定，不接受配置覆盖：

- WebSocket：`wss://orryx.mcwar.cn/ws/server`
- 浏览器入口：`https://orryx.mcwar.cn/connect#token=<一次性 Token>`

旧配置中的 `Editor.Enable` 与 `Editor.PublicUrl` 仍会被忽略。这样可以避免开关残留为 `false`、地址缺失或地址格式错误影响固定中心连接，但不代表匿名或空 License 可以使用；未配置 License 时插件不会发起连接，中心也不会接受缺少身份凭据的服务器。

## Token 安全语义

- Token 有效期固定为 300 秒；
- Token 保持单次使用语义；
- Token 仅位于 URL fragment，不使用查询参数；
- URL fragment 不会作为普通 HTTP 请求目标的一部分发送给服务端，编辑器前端负责读取并完成连接；
- 插件不会把完整入口输出到游戏聊天的可见文本。
