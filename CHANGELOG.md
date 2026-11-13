# MasaExtra

## 2.7.0

精确方块放置逻辑改为完全采用 [EarlyCompat](https://github.com/chenjunfu2/EarlyCompat) 的实现。

### 精确放置（EasyPlace）重写

- 协议编解码（`BlockProtocolStateAdapter` / `MultiStageBlockProtocolStateAdapter` /
  `ItemStackProtocolDataAdapter` / `PlaceStateAccessor` / `BlockPlacer` /
  `EasyPlaceExtraProtocolHelper`）逐字移植自 EarlyCompat，仅做 1.20.1 Yarn → 1.20.4
  官方映射的名称转换。
- 客户端 `WorldUtils.applyCarpetProtocolHitVec` 改为 EarlyCompat 的完整替换逻辑：墙挂
  方块走原始协议（bit0 墙/地 + bit1-2 朝向），其余方块走带标志位的扩展协议。
- 新增 `WorldUtils.doEasyPlaceAction` 注入：多阶段方块（藤蔓、发光地衣、雪层、蜡烛、
  海泡菜、海龟蛋）按投影缺失量点击多次。
- 新增 `PlacementHandler.applyPlacementProtocolV2` 注入：客户端预测状态与密
  服务端完全一致。
- 服务端新增 `BlockItem.useOn` 注入：支持带物品数据的方块，并记录当前是否为
  精确放置流程（供铁轨、拉杆判断）。
- 新增可选 carpet-extra 兼容：`carpetextra.utils.BlockPlacer` 注入，未装该模组时由
  mixin 插件安全跳过。

### 原版方块适配器（按 EarlyCompat 逐一对照）

楼梯、活板门、门、栅栏门、按钮/拉杆（附着面 + 通电）、铃铛、营火、蜡烛、命令方块、
阳光探测器、探测铁轨、动力铁轨、普通铁轨、灯笼、音符盒、红石线、海泡菜、告示牌、
悬挂告示牌、头颅、雪层、结构方块、海龟蛋、藤蔓、多面方块（发光地衣）、巨型蘑菇方块、
拼图方块、光源方块、旗帜、红石火把、墙上的红石火把、石墙。

### 新增配置

- `easyPlaceV2ProtocolExtra`：编码完整方块状态（默认开）。
- `easyPlaceRailBlockNoShapeUpdate`：保持铁轨精确形状，不自动重新连接（默认开）。

### 未移植

EarlyCompat 的 `InventoryPreviewFix`（物品栏预览）与 `PcaProtocolFix`（PCA 同步协议）
与精确放置无关，本模组不包含。

## 2.6.0 及更早

历史版本内容见 git 记录。此前 CHANGELOG 因编码问题已损坏，此前的条目未能保留。

## 2.8.10 修复记录

### 墙挂方块（火把/告示牌/头颅等）精确放置修复

三个独立缺陷，任何一个都会让墙火把失效：

1. **`canPlace` 用错重载**：之前调用 `BlockItem.canPlace(BlockPlaceContext, BlockState)`，
   它除了 `canSurvive` 还会检查 `isUnobstructed`（会撞到玩家），导致墙挂变体一律被拒。
   EarlyCompat 原文用的是 `StandingAndWallBlockItem` 自己的
   `canPlace(LevelReader, BlockState, BlockPos)`，等价于只查 `canSurvive`。

2. **`wallBlock.getStateForPlacement` 在 1.20.4 会返回 null**：`WallTorchBlock` 在 1.20.4
   会遍历玩家视线方向、只返回"某个能存活的方向"，与投影无关；墙面在玩家身后时直接 null。
   EarlyCompat 只用它取属性集合（朝向随后必被覆盖），故为 null 时回落到 `defaultBlockState()`。

3. **与 MasaGadget 的 mixin 优先级冲突**：MasaGadget 的 `fixAccurateProtocol`
   （priority 900）同样注入 `applyCarpetProtocolHitVec` 的 HEAD 并 cancel，而我们默认 priority
   1000 排在它之后，编码器从未执行。改为 `priority = 800` 后解决。

验证：日志中 `redstone_wall_torch[facing=west,lit=true]` 编码为 `protocolValue=13 (0b1101)`，
即 bit0=1（墙上）、facing=WEST、LIT=true，与投影一致。
