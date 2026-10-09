# Gothening的一键进食

一个 Minecraft 1.21.1 / NeoForge 模组。按下一个按键，服务端会从玩家可访问的食物来源中寻找食物，并通过 Minecraft 正常食用流程让玩家吃掉一个食物。

## 版本信息

- Minecraft：1.21.1
- NeoForge：21.1.233
- Java：21
- Mod ID：`gothenings_not_yet_eat_all`
- 当前版本：0.1.0

## License

- License: MIT
- Copyright (c) 2026 Gothening

## 安装

1. 将 `gothenings_not_yet_eat_all-0.1.0.jar` 放入 `.minecraft/mods`。
2. 启动游戏。

所有第三方存储模组都是可选依赖。没有安装 RS、AE2、Sophisticated Backpacks、Sophisticated Storage 或 Spice of Life 时，模组仍然可以正常运行。

## 使用

- 默认按键：`V`
- 每次按键，服务端最多执行一次正常食用
- 客户端只发送 `eat_request`，不决定吃什么、从哪里拿
- 搜索只在按键请求发生时执行，不会每 tick 扫描
- 玩家满饥饿、正在使用其他物品、死亡或无法行动时，不会强行消耗食物

## 食物来源

1. 玩家主背包
2. 玩家周围 8 格内的原版容器，例如箱子、陷阱箱、木桶、潜影盒
3. Refined Storage，可访问附近网格或背包中的绑定网络物品
4. Applied Energistics 2，可访问已打开的 ME 终端或背包中的无线终端
5. Sophisticated Backpacks，玩家身上可访问的精妙背包
6. Sophisticated Storage，玩家周围 8 格内的 Controller、Storage IO 和存储方块

Provider 顺序固定，但食物选择会先经过统一的 Spice of Life 偏好判断：只要存在未吃过的食物，就不会优先吃已经吃过的食物。

## Spice of Life: Carrot Edition

模组通过 `SOLCarrotAPI` / `FoodCapability#hasEaten` 查询食物记录。已安装时，未吃过的食物优先；未安装或关闭兼容开关时，所有食物都按普通候选处理。

## 配置

配置文件位于：

```text
.minecraft/config/gothenings_not_yet_eat_all-common.toml
```

开发环境位于：

```text
run/config/gothenings_not_yet_eat_all-common.toml
```

修改配置后需要重启游戏或服务器。

配置内容包括：

- 按键启用 / 按键代码
- 玩家背包、原版容器、RS、AE2、Sophisticated Backpacks、Sophisticated Storage 的开关
- Spice of Life 兼容开关
- `food_blacklist` 食物黑名单

黑名单使用完整 Item ID，例如：

```toml
food_blacklist = ["minecraft:rotten_flesh", "farmersdelight:cooked_rice"]
```

## 第三方食物

只要物品通过 `ItemStack#getFoodProperties(player)` 正常提供食物属性，就会被自动识别。无需为每个食物 Mod 单独适配。

依赖特殊右键流程、自定义能力或非标准食物组件才能食用的物品，不保证自动进食兼容。

## 构建

```text
.\gradlew.bat build
```

构建产物：

```text
build/libs/gothenings_not_yet_eat_all-0.1.0.jar
```

## 说明

- 服务端是最终权威，客户端不能指定具体物品。
- 食物消耗、饥饿值、饱和度、食物效果和剩余物品都由 Minecraft 正常食用流程处理。
- 本版本不包含自动清除状态效果和领地食物保护功能。
