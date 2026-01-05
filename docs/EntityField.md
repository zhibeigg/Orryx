# EntityField 实体字段参考

本文档列出了所有可用的实体字段名称及其说明。

## 字段列表

| 字段名                   | 返回类型           | 说明                                        |
|-----------------------|----------------|-------------------------------------------|
| `UUID`                | UUID           | 实体的唯一标识符                                  |
| `ID`                  | String         | Adyeshach 实体的 ID（非 Adyeshach 实体返回 "none"） |
| `NAME`                | String         | 实体名称                                      |
| `TYPE`                | String         | 实体类型                                      |
| `YAW`                 | Float          | 偏航角（水平旋转角度）                               |
| `PITCH`               | Float          | 俯仰角（垂直旋转角度）                               |
| `HEIGHT`              | Double         | 实体高度                                      |
| `WIDTH`               | Double         | 实体宽度                                      |
| `LOCATION`            | LocationTarget | 实体位置                                      |
| `EYE_LOCATION`        | LocationTarget | 实体眼睛位置                                    |
| `DIRECTION`           | Vector         | 实体朝向方向向量                                  |
| `MOVE_SPEED`          | Double         | 移动速度                                      |
| `HEALTH`              | Double?        | 当前生命值（仅限生物实体）                             |
| `MAX_HEALTH`          | Double         | 最大生命值（仅限生物实体）                             |
| `VEHICLE`             | Entity?        | 实体乘坐的载具                                   |
| `VELOCITY`            | Vector         | 速度向量                                      |
| `BODY_IN_ARROW`       | Int?           | 身体上的箭数量（仅限生物实体）                           |
| `GRAVITY`             | Boolean        | 是否受重力影响                                   |
| `FIRED`               | Boolean        | 是否着火                                      |
| `FROZEN`              | Boolean        | 是否被冰冻                                     |
| `ON_GROUND`           | Boolean        | 是否在地面上                                    |
| `INSIDE_VEHICLE`      | Boolean        | 是否在载具内                                    |
| `SILENT`              | Boolean        | 是否静音                                      |
| `CUSTOM_NAME_VISIBLE` | Boolean        | 自定义名称是否可见                                 |
| `GLOWING`             | Boolean        | 是否发光                                      |
| `IN_WATER`            | Boolean        | 是否在水中                                     |
| `INVULNERABLE`        | Boolean        | 是否无敌                                      |
| `DEATH`               | Boolean        | 是否已死亡                                     |
| `VALID`               | Boolean        | 实体是否有效                                    |
