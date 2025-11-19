# KTS 脚本执行系统

一套完整的 Kotlin Script (KTS) 脚本执行框架，支持脚本编译、缓存、热重载和生命周期管理。

## 功能特性

- ✅ **脚本编译与缓存**: 自动编译脚本并缓存编译结果，提升性能
- ✅ **热重载**: 支持在运行时重载脚本，无需重启服务器
- ✅ **文件监听**: 自动检测脚本文件变化并重新加载
- ✅ **上下文管理**: 提供完善的脚本执行上下文，支持变量传递和共享
- ✅ **异步执行**: 支持同步和异步脚本执行
- ✅ **超时控制**: 可设置脚本执行超时时间
- ✅ **错误诊断**: 详细的编译和运行时错误信息
- ✅ **依赖管理**: 支持脚本间的依赖关系

## 核心组件

### 1. KtsScriptContext
脚本执行上下文，提供变量存储和访问功能。

```kotlin
val context = KtsScriptContext(plugin, player)
context["myVar"] = "Hello World"
context.putAll(mapOf("x" to 10, "y" to 20))
```

### 2. KtsScriptCompiler
脚本编译器，负责编译和缓存脚本。

```kotlin
// 编译文件
val result = KtsScriptCompiler.compile(File("script.kts"))

// 编译代码
val result = KtsScriptCompiler.compileCode("println(\"Hello\")")

// 清空缓存
KtsScriptCompiler.clearCache()
```

### 3. KtsScriptExecutor
脚本执行器，负责执行编译后的脚本。

```kotlin
val executor = KtsScriptExecutor(plugin)
val context = KtsScriptContext(plugin, player)

// 执行文件
val result = executor.execute(File("script.kts"), context, timeout = 5000)

// 执行代码
val result = executor.executeCode("1 + 1", "calc", context)
```

### 4. KtsScriptManager
脚本管理器，提供脚本的加载、卸载、重载等功能。

```kotlin
val manager = KtsScriptManager(plugin, File("scripts"))

// 加载所有脚本
manager.loadAll()

// 执行脚本
val result = manager.execute("myScript", player, mapOf("arg" to "value"))

// 启用自动重载
manager.autoReload = true

// 重载脚本
manager.reload("myScript")
manager.reloadAll()
```

### 5. KtsScriptLoader
脚本配置加载器，从 YAML 配置加载脚本元数据。

```kotlin
val loader = KtsScriptLoader("myScript", configuration, scriptFile)
println("脚本名称: ${loader.name}")
println("版本: ${loader.version}")
println("作者: ${loader.author}")
```

### 6. KtsScriptUtils
工具类，提供便捷的脚本操作方法。

```kotlin
// 设置默认管理器
KtsScriptUtils.setDefaultManager(manager)

// 执行脚本文件
KtsScriptUtils.executeFile(
    file = File("test.kts"),
    player = player,
    onSuccess = { result -> println("成功: $result") },
    onFailure = { error -> println("失败: $error") }
)

// 异步执行
KtsScriptUtils.executeCodeAsync("println(\"Hello\")").thenAccept { result ->
    result.onSuccess { println("结果: $it") }
          .onFailure { println("错误: $it") }
}

// 验证脚本
if (KtsScriptUtils.validateScript(File("script.kts"))) {
    println("脚本语法正确")
}
```

## 使用示例

### 基础用法

```kotlin
// 1. 创建管理器
val manager = KtsScriptManager(plugin, File(plugin.dataFolder, "scripts"))
KtsScriptUtils.setDefaultManager(manager)

// 2. 加载脚本
manager.loadAll()

// 3. 执行脚本
val result = manager.execute(
    scriptName = "hello",
    player = player,
    variables = mapOf("name" to "World")
)

// 4. 处理结果
when (result) {
    is KtsExecutionResult.Success -> {
        println("执行成功: ${result.value}")
    }
    is KtsExecutionResult.Failure -> {
        println("执行失败: ${result.message}")
        result.printDiagnostics()
    }
}
```

### 使用扩展函数

```kotlin
// Player 执行脚本
player.executeKtsScript("myScript", mapOf("arg" to 123))
    .onSuccess { println("成功: $it") }
    .onFailure { println("失败: $it") }

// Player 执行代码
player.executeKtsCode("player.sendMessage(\"Hello!\")")

// 获取特定类型的返回值
val number: Int? = result.getAs<Int>()
val text = result.getAsOrDefault("default")
```

### 脚本文件示例

**hello.kts**
```kotlin
// 访问插件
println("插件名称: ${plugin.name}")

// 访问玩家
player?.let {
    it.sendMessage("Hello, ${it.name}!")
}

// 访问上下文变量
val name = context["name"] as? String ?: "Unknown"
println("名称: $name")

// 设置变量
context["result"] = "执行成功"

// 返回值
"脚本执行完成"
```

**calc.kts**
```kotlin
// 获取参数
val x = context["x"] as? Int ?: 0
val y = context["y"] as? Int ?: 0

// 计算
val result = x + y

// 返回结果
result
```

### 配置文件示例

**scripts/hello.yml**
```yaml
Options:
  # 脚本显示名称
  Name: "问候脚本"

  # 脚本描述
  Description: "向玩家发送问候消息"

  # 作者
  Author: "YourName"

  # 版本
  Version: "1.0.0"

  # 是否启用
  Enabled: true

  # 执行超时(毫秒)
  Timeout: 5000

  # 依赖的其他脚本
  Dependencies:
    - "common"
    - "utils"

  # 初始变量
  Variables:
    greeting: "Hello"
    times: 3
```

## API 集成

### 初始化

```kotlin
class YourPlugin : JavaPlugin() {
    private lateinit var scriptManager: KtsScriptManager

    override fun onEnable() {
        // 创建脚本目录
        val scriptsDir = File(dataFolder, "scripts")

        // 初始化管理器
        scriptManager = KtsScriptManager(this, scriptsDir)
        KtsScriptUtils.setDefaultManager(scriptManager)

        // 加载所有脚本
        scriptManager.loadAll()

        // 启用自动重载(可选)
        scriptManager.autoReload = true
    }

    override fun onDisable() {
        // 关闭管理器
        scriptManager.shutdown()
    }
}
```

### 在命令中使用

```kotlin
@CommandBody
fun reload() {
    val count = scriptManager.reloadAll()
    println("已重载 $count 个脚本")
}

@CommandBody
fun execute(@CommandBody scriptName: String, sender: CommandSender) {
    val player = sender as? Player

    scriptManager.execute(scriptName, player)
        .onSuccess { println("执行成功: $it") }
        .onFailure { println("执行失败: $it") }
}
```

## 性能优化

1. **编译缓存**: 编译结果会自动缓存，避免重复编译
2. **Hash 验证**: 使用 MD5 检测文件变化，只在必要时重新编译
3. **并发安全**: 使用 ConcurrentHashMap 确保线程安全
4. **异步执行**: 支持异步执行避免阻塞主线程

## 安全建议

1. **超时设置**: 为脚本设置合理的超时时间，防止死循环
2. **权限控制**: 限制脚本文件的访问权限
3. **输入验证**: 验证脚本输入参数
4. **异常处理**: 妥善处理脚本执行异常

## 常见问题

### Q: 如何在脚本中导入自定义类?
A: 在 KtsScriptCompiler 的 compilationConfiguration 中添加 defaultImports:
```kotlin
defaultImports("your.package.ClassName")
```

### Q: 如何调试脚本?
A: 使用 printDiagnostics() 查看详细的错误信息:
```kotlin
result.printDiagnostics()
```

### Q: 脚本执行超时怎么办?
A: 增加超时时间或优化脚本逻辑:
```kotlin
manager.execute(scriptName, player, timeout = 10000) // 10秒
```

## 版本历史

- **1.0.0** - 初始版本
  - 脚本编译与执行
  - 缓存管理
  - 热重载支持
  - 文件监听
  - 上下文管理

## 许可证

本项目采用与 Orryx 主项目相同的许可证。
