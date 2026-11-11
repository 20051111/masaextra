# MasaExtra

Minecraft **1.20.4** Fabric 模组。把 [EarlyCompat](https://github.com/chenjunfu2/EarlyCompat) 的精确方块放置（Easy Place）逻辑完整移植过来，让 Litematica 的轻松放置在服务端支持精确协议时，能还原投影里的完整方块状态。

> **注意：所有配置项默认全部关闭。** 第一次运行会生成一份全部为 `false` 的配置文件，需要手动开启想要的功能。已有配置文件则以文件内容为准。

---

## 功能简介

### 精确放置（移植自 EarlyCompat）

Litematica 原生的 V2 协议只能表达水平朝向，楼梯的形态、台阶的上下、活板门的开合、墙挂方块的"墙上/地面"全都丢失。这个模组把完整方块状态编码进 hit vector，服务端解码后按投影的精确状态放置。

- **墙挂方块**：火把、告示牌、头颅、旗帜、珊瑚扇等 `StandingAndWallBlockItem`，协议低 3 位携带「墙上/地面 + 朝向」。
- **方块适配器**：楼梯、活板门、门、拉杆、红石线、铁轨、藤蔓、蜡烛、海泡菜等 30 余种方块各自携带完整状态位。
- **铁轨形状保持**：放置后不再与邻居重新连接，保留投影中的形状。
- **支撑缺失时取消**：墙面不存在时拒绝放置，而不是退回放一个地面火把。

### 可选模组集成

- **Tweakeroo**：位置吸附到方块中心（上升沿触发一次）、进入世界刷新 Gamma。
- **Tweakermore**：自动取材料期间暂停轻松放置。

所有集成都在 `isModLoaded` 检查后按需加载，缺失对应模组时功能自动禁用，不会崩溃。

---

## 配置

### 客户端

文件：`config/masaextra-client.json`
游戏内：通过 GUI 的 `全部` / `Litematica` / `Tweakeroo` / `OMMC` 四个页签修改。

| 配置项 | 键名 | 类型 | 默认值 |
|---|---|---|---|
| 总开关 | `masaextraEnabled` | 布尔 | `false` |
| 服务端精确放置 | `masaextraUseServerExact` | 布尔 | `false` |
| 编码完整方块状态（V2 协议） | `easyPlaceV2ProtocolExtra` | 布尔 | `false` |
| 支撑缺失时取消 | `masaextraSuppressMissingSupport` | 布尔 | `false` |
| 同位置冷却（毫秒） | `masaextraCooldownMs` | 整数 `0`–`5000` | `250` |
| 位置吸附到方块中心 | `masaextraSnapAlignEnabled` | 布尔 | `false` |
| 吸附触发方式 | `masaextraSnapAlignTrigger` | 枚举 `snap_aim` / `hotkey` | `snap_aim` |
| 吸附热键 | `masaextraSnapAlignHotkey` | 热键 | 空 |
| 自动取材料时暂停轻松放置 | `masaextraSuppressEasyPlaceOnAutoCollect` | 布尔 | `false` |
| 进入世界刷新 Gamma | `masaextraRefreshGammaOnJoin` | 布尔 | `false` |
| OMMC 高亮实体严格匹配 | `masaextraOmmcHighlightStrictMatch` | 布尔 | `false` |
| 调试模式 | `masaextraDebugMode` | 布尔 | `false` |
| 聊天栏输出调试信息 | `masaextraChatDebug` | 布尔 | `false` |

### 服务端

文件：`config/exactplace-server.json`

| 配置项 | 键名 | 类型 | 默认值 |
|---|---|---|---|
| 启用 | `enabled` | 布尔 | `false` |
| 要求手持对应物品 | `requireItem` | 布尔 | `true` |
| 最大交互距离 | `maxDistance` | 浮点 | `6.0` |
| 每秒请求上限 | `rateLimitPerSecond` | 整数 | `20` |
| 允许方块实体 | `allowBlockEntities` | 布尔 | `false` |
| 调试模式 | `debug` | 布尔 | `false` |
| 编码完整方块状态 | `easyPlaceV2ProtocolExtra` | 布尔 | `false` |
| 保持铁轨精确形状 | `easyPlaceRailBlockNoShapeUpdate` | 布尔 | `false` |
| 方块黑名单 | `blacklist` | 字符串数组 | 见下 |

黑名单默认值：

```json
["minecraft:bedrock", "minecraft:command_block", "minecraft:chain_command_block",
 "minecraft:repeating_command_block", "minecraft:structure_block", "minecraft:jigsaw",
 "minecraft:spawner", "minecraft:end_portal_frame"]
```

> `requireItem` 默认保持 `true`：它是防作弊护栏，关掉意味着玩家可以凭空生成方块。不建议关闭。

---

## 安装

1. 安装 Fabric Loader（1.20.4）与 Fabric API
2. 必需依赖：**Litematica**、**malilib**
3. 把 `masaextra-<version>.jar` 放进 `mods/`
4. **服务端也要装同一个 jar**，并在 `config/exactplace-server.json` 里把 `enabled` 设为 `true`
5. 进游戏后手动开启需要的配置项

可选依赖：carpet-extra（服务端精确协议规则）、Tweakeroo、Tweakermore。

---

## 构建

需要 JDK 21+ 与 Gradle 9：

```bash
./gradlew build
```

产物在 `build/libs/masaextra-<version>.jar`。

`libs/` 下的四个 jar 是编译期可选依赖，构建时需要存在：

| 文件 | 用途 | 许可 |
|---|---|---|
| `mixinextras-fabric-0.5.5.jar` | `@Local` / `@WrapOperation` 注解 | MIT |
| `carpet-extra-1.20.3-1.4.128.jar` | carpet 的 `accurateBlockPlacement` 规则 | 见其仓库 |
| `tweakeroo-fabric-1.20.4-0.19.4.jar` | Snap Aim 开关 | LGPL-3.0 |
| `tweakermore-v3.33.2-mc1.20.4.jar` | 自动取材料开关 | LGPL-3.0 |

---

## 许可

代码为本项目的原创移植工作。`libs/` 下的第三方 jar 版权归各自作者所有，仅作编译期依赖。
