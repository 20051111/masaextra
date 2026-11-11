# MasaExtra

Minecraft **1.20.4** Fabric mod。核心是把 [EarlyCompat](https://github.com/chenjunfu2/EarlyCompat) 的精确方块放置（Easy Place）逻辑完整移植过来，让 Litematica 的轻松放置在服务端支持精确协议时能还原投影里的完整方块状态。

## 功能

### 精确放置（移植自 EarlyCompat）

- **V2 精确协议**：把完整方块状态编码进 hit vector 的 X 分量，服务端解码后按投影的精确朝向/形态放置，而不只是水平朝向。
- **墙挂方块**：火把、告示牌、头颅、旗帜、珊瑚扇等 `StandingAndWallBlockItem`，协议低 3 位携带「墙上/地面 + 朝向」，修复"墙火把被放成竖火把"。
- **方块适配器**：楼梯、活板门、门、拉杆、红石线、铁轨、藤蔓、蜡烛、海泡菜等 30 余种方块各自携带完整状态位。
- **铁轨形状保持**：放置后不再与邻居重新连接，保留投影中的形状。
- **支撑缺失时取消**：墙面不存在时拒绝放置，而不是退回放一个地面火把。

### Tweakeroo 集成

- **位置吸附到方块中心**：触发时可把玩家坐标吸附到所站方块正中心，使射线起点固定、放置结果可复现。
  - 触发方式二选一：跟随 Tweakeroo 的 Snap Aim 开关，或按住自定义热键。
  - **只在触发上升沿对齐一次**，之后转动视角、走动、停下都不再改动坐标。
- **进入世界刷新 Gamma**：修复 Tweakeroo 的 gamma 覆写在进入游戏后失效。

### 其它

- 同一位置放置冷却可调（默认 250ms，比 Litematica 原生的 2 秒快 8 倍）。
- OMMC 高亮实体严格匹配。
- 诊断日志（游戏日志 / 聊天栏）。

## 构建

需要 JDK 21+ 与 Gradle 9。

```bash
./gradlew build
```

产物在 `build/libs/masaextra-<version>.jar`。

`libs/` 下的三个 jar 是编译期可选依赖，构建时需要存在：

| 文件 | 用途 | 许可 |
|---|---|---|
| `mixinextras-fabric-0.5.5.jar` | `@Local` / `@WrapOperation` 注解 | MIT |
| `carpet-extra-1.20.3-1.4.128.jar` | carpet 的 `accurateBlockPlacement` 规则 | 见其仓库 |
| `tweakeroo-fabric-1.20.4-0.19.4.jar` | Snap Aim 开关 | LGPL-3.0 |

运行时全部可选：模组通过 `isModLoaded` 检查后按需加载对应集成，缺失时对应功能自动禁用而不会崩溃。

## 服务端

服务端需安装同一个 jar，并在 `config/exactplace-server.json` 中设置 `"enabled": true`。客户端与服务端 jar 相同，但服务端只加载 `mixins` 部分。

## 配置

`config/masaextra-client.json`，游戏内可通过 `全部` / `Litematica` / `Tweakeroo` / `OMMC` 四个页签修改。

## 许可

代码为原创移植工作。第三方 mod 的 jar 位于 `libs/`，版权归各自作者所有，仅作编译期依赖。
