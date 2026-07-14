# Orryx 在线编辑器

## 启用与访问

```yaml
Editor:
  Enable: true
  PublicUrl: "https://orryx.mcwar.cn"
  License: "购买后获得的 license"
```

启用并完成中心服务器注册后，拥有 `Orryx.Command.Editor` 权限的玩家可执行 `/orryx edit`。该命令仅允许玩家执行，控制台不能签发玩家访问入口。

插件只在中心确认 Token 注册成功后发送可点击消息。游戏聊天仅显示“点击打开编辑器 · 5 分钟内有效”，不会显示或复制完整 URL；点击事件携带实际入口。

## PublicUrl 约束

`Editor.PublicUrl` 是浏览器入口的公开基址，默认值为 `https://orryx.mcwar.cn`。读取配置时必须同时满足：

- 是包含主机名的绝对 URL；
- scheme 必须为 `https`；
- 不包含 userInfo；
- 不包含查询参数；
- 不包含片段。

非法配置采用 fail-closed：插件断开或拒绝建立编辑器中心连接，不注册一次性 Token，也不生成浏览器入口。查询参数形式不受支持，不能配置 `https://example.com?token=...`。

允许使用路径前缀。插件会移除末尾斜杠并追加 `/connect#token=...`：

```text
PublicUrl: https://editor.example.com/orryx
生成入口: https://editor.example.com/orryx/connect#token=<一次性 Token>
```

## Token 安全语义

- Token 有效期固定为 300 秒；
- Token 保持单次使用语义；
- Token 仅位于 URL fragment，不使用查询参数；
- URL fragment 不会作为普通 HTTP 请求目标的一部分发送给服务端，编辑器前端负责读取并完成连接；
- 插件不会把完整入口输出到游戏聊天的可见文本。
