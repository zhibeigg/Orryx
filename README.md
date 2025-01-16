# Orryx

[![pEFt9N8.png](https://s21.ax1x.com/2025/01/16/pEFt9N8.png)](https://imgse.com/i/pEFt9N8)
> 跨时代技能插件，支持实现复杂逻辑，为稳定高效而生。
## 构建发行版本

Orryx是免费的, 发行版本用于正常使用, 不含 TabooLib 本体。

```
./gradlew build
```

## 构建开发版本

开发版本包含 TabooLib 本体, 用于开发者使用, 但不可运行。

```
./gradlew taboolibBuildApi -PDeleteCode
```

> 参数 -PDeleteCode 表示移除所有逻辑代码以减少体积。