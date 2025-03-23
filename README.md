# Orryx

![pEFaJDs.png](https://s21.ax1x.com/2025/01/16/pEFaJDs.png)

[![Static Badge](https://img.shields.io/badge/%E4%B8%AD%E6%96%87-%E5%BC%80%E5%A7%8B%E4%BD%BF%E7%94%A8WIKI-orryx?style=flat-square&logo=n8n&logoColor=darkred&color=darkred)](https://o0vvjwgpeju.feishu.cn/wiki/Syzzw7aQwixJ4YkXoOAcyYkfnOg)
[![Coverage Status](https://coveralls.io/repos/github/zhibeigg/Orryx/badge.svg?branch=master)](https://coveralls.io/github/zhibeigg/Orryx?branch=master)
> 跨时代技能插件，支持实现复杂逻辑，为稳定高效而生。
## 构建发行版本

[<img src="https://camo.githubusercontent.com/a654761ad31039a9c29df9b92b1dc2be62d419f878bf665c3288f90254d58693/68747470733a2f2f77696b692e70746d732e696e6b2f696d616765732f362f36392f5461626f6f6c69622d706e672d626c75652d76322e706e67" alt="" width="300">](https://github.com/TabooLib/taboolib)

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

## BStats
[![](https://bstats.org/signatures/bukkit/Orryx.svg)](https://bstats.org/plugin/bukkit/Orryx/24289/)