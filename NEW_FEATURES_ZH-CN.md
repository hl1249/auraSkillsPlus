# AuraSkills 本次新增功能说明

本文档对应当前工作区相对于原版 AuraSkills 的新增功能与改动。

## 1. 玩家个人技能等级上限

每名玩家、每个技能都可以拥有独立等级上限。未单独设置时，使用 `player_caps.yml` 的 `default-cap`；当前默认值为 `5`。

实际可升到的等级为：

`min(基础上限 + 已分配上限点, skills.yml 中该技能的全局 max_level)`

因此，无论经验来源、普通升级或管理员设置等级，均不能超过该玩家的实际技能上限。

数据文件：`plugins/AuraSkills/player_caps.yml`

## 2. 管理员上限命令

以下命令均使用玩家名称，不需要 UUID；技能和在线玩家名称支持 Tab 补全。

| 命令 | 作用 |
| --- | --- |
| `/sk addLevelLimit <技能> <玩家名> <数量>` | 增加该玩家该技能的基础上限 |
| `/sk setLevelLimit <技能> <玩家名> <等级>` | 设置该玩家该技能的基础上限 |
| `/sk getLevelLimit <技能> <玩家名>` | 查看基础上限、已分配点、全局上限和实际可达上限 |
| `/sk resetLevelLimit <技能> <玩家名>` | 清除个人基础上限，恢复使用默认上限 |

对应权限：`auraskills.command.cap.add`、`cap.set`、`cap.get`、`cap.reset`。

## 3. 可分配技能上限点

管理员可以向玩家发放“技能上限点”。玩家获得点数后，可自行选择分配到砍树、采矿、剑术、防御、炼金等任意已启用技能。

| 命令 | 作用 |
| --- | --- |
| `/sk addSkillLimitPoints <玩家名> <数量>` | 增加可分配上限点 |
| `/sk setSkillLimitPoints <玩家名> <数量>` | 设置可分配上限点余额 |
| `/sk getSkillLimitPoints <玩家名>` | 查询可分配上限点余额 |
| `/sk takeSkillLimitPoints <玩家名> <数量>` | 扣除未使用的上限点 |

对应权限：`auraskills.command.limitpoints.add`、`limitpoints.set`、`limitpoints.get`、`limitpoints.take`。

## 4. 玩家分配 GUI

玩家使用 `/sk limitpoints` 打开界面，需要权限 `auraskills.command.limitpoints.use`。

- 第一页沿用 AuraSkills 技能菜单的五行布局，展示所有已启用技能。
- 每个技能悬浮说明显示“上限等级”和“已分配点数”。
- 顶部经验瓶显示当前未使用的可分配点余额。
- 即使余额为 `0`，玩家仍可以点击技能进入分配页查看。
- 进入分配页时，本次待分配点默认是 `0`。
- 中间下界之星代表本次待分配点；点数大于 0 时，以物品堆叠数量显示。
- 左侧 `+1` 增加待分配点，不能超过余额或该技能全局等级上限。
- 右侧 `-1` 最低可减至 `0`。
- 只有点击确认才会扣除余额并提高该技能上限；待分配为 `0` 或余额不足时不能完成分配。

菜单文件：

- `plugins/AuraSkills/menus/limit_points.yml`
- `plugins/AuraSkills/menus/limit_point_confirm.yml`

## 5. 工具、护甲、药水和箭矢等级限制

新增配置文件 `plugins/AuraSkills/equipment_requirements.yml`，用于按技能等级限制物品使用。

- `mining`：镐
- `foraging`：斧头
- `farming`：锄头
- `excavation`：铲子
- `defense`：全套护甲
- `alchemy`：药水效果类型
- `archery`：普通箭、光灵箭、药水箭

默认阶段示例：木质 `0`、石质 `5`、铁质 `50`、金质 `100`、钻石 `150`、下界合金 `300`。可直接修改配置中的材料与所需等级。

OP 以及拥有 `auraskills.requirement.bypass` 权限的玩家不受工具、护甲、药水和箭矢等级限制。

原版不存在的材料名（例如原版没有的铜制工具）会被安全跳过，不会导致服务器启动失败。

## 6. 配置与升级注意事项

- `player_caps.yml` 当前数据版本为 `2`，包含基础上限、已分配点和可用点。
- 新增/修改 Java 代码后必须完整重启服务器；`/sk reload` 只能重载配置，不能加载新的 JAR 代码。
- 服务器使用的构建产物为 `build/libs/AuraSkills-2.3.12.jar`。

## 7. 功能与代码模块对应表

| 功能 | 核心代码/配置模块 | 说明 |
| --- | --- | --- |
| 个人技能上限数据 | `bukkit/.../level/PlayerSkillCapManager.java` | 读写 `player_caps.yml`，计算基础上限、已分配点、可用点与实际技能上限 |
| 升级时的上限拦截 | `common/.../level/LevelManager.java` | 经验升级前按玩家实际技能上限阻止继续升级 |
| 管理员设定等级时的上限拦截 | `bukkit/.../commands/SkillCommand.java` | `/sk setlevel`、批量设定等级时不会突破个人上限 |
| 平台上限接口 | `common/.../AuraSkillsPlugin.java` | 提供 `getSkillLevelCap` 接口，默认仍使用技能全局上限 |
| Bukkit 平台接入 | `bukkit/.../AuraSkills.java` | 初始化个人上限管理器、生成配置、注册装备限制监听器 |
| 上限与技能点命令 | `bukkit/.../commands/PlayerCapCommand.java` | 实现 `addLevelLimit`、`addSkillLimitPoints` 等全部新增子命令及 Tab 补全 |
| 命令注册 | `bukkit/.../commands/CommandRegistrar.java` | 注册 `PlayerCapCommand` |
| 玩家技能点列表 GUI | `bukkit/.../menus/LimitPointsMenu.java` | 显示技能、个人上限、已分配点，并打开确认页面 |
| 技能点确认 GUI | `bukkit/.../menus/LimitPointConfirmMenu.java` | 处理 `+1`、`-1`、待分配下界之星数量和确认扣点 |
| GUI 注册与菜单文件生成 | `bukkit/.../menus/MenuRegistrar.java`、`MenuFileManager.java` | 注册并生成两个新增菜单 |
| GUI 配置 | `bukkit/src/main/resources/menus/limit_points.yml`、`limit_point_confirm.yml` | 定义技能列表页和确认页布局、物品、文本 |
| 工具/护甲/消耗品限制规则 | `bukkit/.../requirement/EquipmentRequirementManager.java` | 读取并解析装备、药水和箭矢所需技能等级 |
| 物品使用拦截 | `bukkit/.../requirement/EquipmentRequirementListener.java` | 在使用、装备或消耗时校验玩家技能等级 |
| 装备限制配置 | `bukkit/src/main/resources/equipment_requirements.yml` | 配置镐、斧、铲、护甲、药水和箭矢的等级阶段 |
| 玩家上限/点数配置 | `bukkit/src/main/resources/player_caps.yml` | 默认上限和玩家持久化数据的初始模板 |

## 8. 设计边界与验收要点

### 数据与上限规则

- 管理员通过 `setLevelLimit` / `addLevelLimit` 设置的是**基础上限**，不会改变玩家当前等级或经验。
- 玩家 GUI 投入的点记录在 `allocated-points`，与基础上限独立；重置基础上限不会删除已分配点。
- 可用点与已分配点均立即保存。余额不足、技能无效或已到全局上限时，分配不会扣点。
- 玩家已有等级高于后来调整的上限时，不会自动降级或删除经验，但不能再继续升级。

### 原版物品限制边界

- 原版 Minecraft 没有铜制工具和铜制护甲；如服务器其他插件注册了对应 Material，可在配置中加入，否则该材料会被忽略。
- 原版箭矢按普通箭、光灵箭、药水箭分级；如需自定义箭矢品质，需要后续按对应插件的物品/PDC 标记扩展。
- 装备资格检查的是玩家的**当前技能等级**，不是技能上限或未使用的上限点。

### 推荐验收流程

1. 设置 `default-cap: 5`，确认新玩家任一技能只能从 5 级以下升到 5 级。
2. 执行 `/sk addSkillLimitPoints <玩家名> 1`，确认余额变为 1，重启后仍存在。
3. 执行 `/sk limitpoints`，进入技能页后选择砍树，待分配下界之星初始为 0。
4. 点击 `+1`，确认下界之星显示数量 1；点击确认后，砍树上限增加 1、余额减少 1。
5. 余额为 0 时仍能进入技能分配页查看，但无法以 0 点确认，也不能实际增加上限。
6. 分别以不足等级尝试使用受限工具、护甲、药水和箭矢，确认使用会被拦截；达到配置等级后允许使用。
