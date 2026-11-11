# 移植规则：照抄 EarlyCompat

**每次修改 masaextra 的精确放置逻辑之前，必须先重复确认这条要求，再动手。**

## 硬性要求

精确方块放置相关的一切逻辑，**必须与 EarlyCompat 原文逐字对应**。不允许：

- 自己发明新分支、新枚举、新判据
- "顺手修好"原文里看起来奇怪的地方
- 用自己的理解替换原文的 API 调用

允许的改动只有两类：

1. **1.20.1 Yarn → 1.20.4 官方映射的名称转换**（如 `ItemPlacementContext` → `BlockPlaceContext`、
   `Vec3d` → `Vec3`、`Settings` → `Item.Properties`、`with()` → `setValue()`）
2. **编译所必需的语法适配**（构造器签名、泛型）

## 每次修改的固定流程

1. 打开 `_build\reference\EarlyCompat-master\src\main\java\chenjunfu2\earlycompat\` 下对应的原文
2. 把原文整段贴出来，逐行对照当前实现
3. 列出差异，逐条判断属于「允许的映射转换」还是「我自己加的」
4. 属于后者的一律删除，恢复原文行为
5. 自检 → 构建

## 已知我偏离过原文的地方（教训）

| 位置 | 我做的 | 原文实际是 |
|---|---|---|
| `MixinStandingAndWallBlockItem` | 用 `BlockState.canSurvive` + 自造 `wrongSide` + `isUnobstructed` + `WALL_BLOCKED` 枚举 | 只调 `this.canPlace(context, tryAlternative)`，失败置 null 后 `setReturnValue(null); cancel()` |
| `MixinStandingAndWallBlockItem` | 加了 carpet-extra 规则 gate，且缺省 false | gate 的 accessor 缺省 **true**；原文客户端**无条件**执行 |
| `BlockPlacer` | 加了 `Outcome`/`Decoded` 枚举和多处调试分支 | 原文只有 `relativeHitX < 2.0 → null`、wall/floor 两分支，最后 `return blockState` |

## 判断依据

原文在 1.20.1 用 `canPlace(context, state)`，1.20.4 里 `BlockItem.canPlace(BlockPlaceContext, BlockState)`
同名同义（`mustSurvive()` 判定后才查 `canSurvive`，随后必查 `isUnobstructed`）。
所以**原文调用的就是 `this.canPlace(context, state)`**，不该换成 `canSurvive`。

## 已知的第三方 mixin 冲突（必须检查）

| 冲突方 | 位置 | 现象 | 处理 |
|---|---|---|---|
| MasaGadget `fixCarpetAccurateProtocol.MixinWorldUtils`（priority **900**） | `WorldUtils.applyCarpetProtocolHitVec` 的 `HEAD`，`cancellable=true` | 它先执行并 cancel，导致我们的编码器**一条都不执行**：类加载日志有、`HEAD` 日志没有，服务端始终收到它编码的 `facing*2+2`（无墙/地位） | 我们的同类注入设 `priority = 800`，抢在它前面 |
| MasaGadget `fixCarpetAccurateProtocol.MixinBlockItem`（priority 980） | `BlockItem.getPlacementState` | 与 `StandingAndWallBlockItem` 不冲突（后者重写了该方法） | 无需处理 |

**排查方法**：先看 `[masaextra/encode] CLASS LOADED` 与 `HEAD` 是否成对出现。
只有前者 → 注入被别人的 cancel 抢先，检查 priority。
两者都有 → 注入正常，问题在编码/解码逻辑。

**Mixin priority 语义**：数值**小**的先应用、先执行；同一位置若前一个 `cancel()`，后面的全部跳过。

## MasaGadget `preApplyCarpetProtocolHitVec` 的编码（对照参考）

它只按 `BlockUtils.getFirstPropertyFacingValue(state)` 编码：
`x = pos.x + relX + 2.0 + facing.get3DDataValue() * 2`，
再对 stairs/slab/trapdoor/door 叠加 32 的倍数或改 y。
**没有"墙/地"这一位**，所以 `redstone_wall_torch` 会被编码成 `4`（`WEST.get3DDataValue()`），
`4 & 1 == 0`，服务端按地面版解码 —— 这正是"墙上的火把被放成竖火把"的直接原因。
