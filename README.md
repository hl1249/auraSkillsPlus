# AuraSkillsPlus

> AuraSkillsPlus 是基于 AuraSkills 的 RPG 技能扩展版本，新增玩家个人技能等级上限、可分配上限点 GUI，以及工具、护甲、药水和箭矢的技能等级限制。  
> AuraSkillsPlus is an RPG skills extension based on AuraSkills. It adds per-player skill caps, a GUI point-allocation system, and skill requirements for tools, armor, potions, and arrows.

## 功能 / Features

- **个人技能等级上限 / Per-player skill caps**：每位玩家的每项技能都可拥有独立上限；未单独设置时使用默认上限。  
  Every player can have an individual cap for each skill; the configured default is used when no personal cap exists.
- **可分配上限点 / Allocatable cap points**：管理员发放上限点后，玩家可在 GUI 中自行投入任意已启用技能。  
  Administrators can grant cap points, which players can allocate to any enabled skill in a GUI.
- **技能等级装备限制 / Skill-gated equipment**：采矿限制镐、砍树限制斧、草药/农业限制锄、挖掘限制铲、防御限制护甲、炼金限制药水、箭术限制箭矢。  
  Mining gates pickaxes, Foraging gates axes, Farming gates hoes, Excavation gates shovels, Defense gates armor, Alchemy gates potions, and Archery gates arrows.
- **可配置规则 / Configurable rules**：默认上限、玩家数据与每种物品所需等级均可通过 YAML 配置。  
  Default caps, player data, and required levels for every material are configurable through YAML.

## 安装 / Installation

1. 构建或取得插件 JAR。  
   Build or obtain the plugin JAR.
2. 将 JAR 放入服务器的 `plugins` 目录。  
   Place the JAR in the server `plugins` directory.
3. 完整重启服务器，使插件生成配置文件。  
   Fully restart the server to generate configuration files.
4. 根据服务器玩法编辑 `plugins/AuraSkills/player_caps.yml` 和 `plugins/AuraSkills/equipment_requirements.yml`。  
   Adjust `plugins/AuraSkills/player_caps.yml` and `plugins/AuraSkills/equipment_requirements.yml` for your server.

> Java 代码更新后必须完整重启服务器；`/sk reload` 只能重载配置，不能加载新 JAR。  
> A full server restart is required after Java code changes. `/sk reload` reloads configuration only; it cannot load a new JAR.

## 配置 / Configuration

### 默认技能上限 / Default skill cap

文件：`plugins/AuraSkills/player_caps.yml`

```yaml
version: 2
default-cap: 5
players: {}
```

未为玩家单独设置基础上限时，每项技能默认最高为 `default-cap`。实际技能上限为：

```text
min(基础上限 + 已分配上限点, skills.yml 中该技能的 max_level)
```

When no individual base cap is set, each skill uses `default-cap`. The effective cap is:

```text
min(base cap + allocated cap points, the skill max_level in skills.yml)
```

### 装备与消耗品要求 / Equipment and consumable requirements

文件：`plugins/AuraSkills/equipment_requirements.yml`

默认阶段为：木质 `0`、石质 `5`、铁质 `50`、金质 `100`、钻石 `150`、下界合金 `300`。可直接修改材料名和所需等级。

The default tiers are: wood `0`, stone `5`, iron `50`, gold `100`, diamond `150`, and netherite `300`. Edit material names and levels directly to customize them.

原版不存在的材料（例如铜制工具）会被安全跳过。  
Materials unavailable in vanilla Minecraft, such as copper tools, are safely skipped.

## 命令 / Commands

所有命令使用 `/sk` 别名；技能和在线玩家参数支持 Tab 补全。  
All commands use the `/sk` alias. Skill and online-player arguments support tab completion.

| 命令 / Command | 作用 / Purpose | 权限 / Permission |
| --- | --- | --- |
| `/sk addLevelLimit <skill> <player> <amount>` | 增加技能基础上限 / Add a skill base cap | `auraskills.command.cap.add` |
| `/sk setLevelLimit <skill> <player> <cap>` | 设置技能基础上限 / Set a skill base cap | `auraskills.command.cap.set` |
| `/sk getLevelLimit <skill> <player>` | 查询上限详情 / View cap details | `auraskills.command.cap.get` |
| `/sk resetLevelLimit <skill> <player>` | 重置基础上限 / Reset base cap | `auraskills.command.cap.reset` |
| `/sk addSkillLimitPoints <player> <amount>` | 发放可分配点 / Grant available points | `auraskills.command.limitpoints.add` |
| `/sk setSkillLimitPoints <player> <amount>` | 设置可分配点余额 / Set available-point balance | `auraskills.command.limitpoints.set` |
| `/sk getSkillLimitPoints <player>` | 查询可分配点 / View available points | `auraskills.command.limitpoints.get` |
| `/sk takeSkillLimitPoints <player> <amount>` | 扣除未使用点 / Remove unused points | `auraskills.command.limitpoints.take` |
| `/sk limitpoints` | 打开玩家分配 GUI / Open the allocation GUI | `auraskills.command.limitpoints.use` |

管理员（OP）和拥有 `auraskills.requirement.bypass` 的玩家会绕过工具、护甲、药水与箭矢的技能等级限制。  
Operators and players with `auraskills.requirement.bypass` bypass the skill requirements for tools, armor, potions, and arrows.

## 分配 GUI / Allocation GUI

`/sk limitpoints` 的技能列表沿用 AuraSkills 的技能菜单布局。顶部经验瓶显示未使用点数，技能 Lore 显示该技能上限与已分配点数。

The `/sk limitpoints` skill list follows the AuraSkills menu layout. The top experience bottle shows unused points, while each skill lore shows its cap and allocated points.

选择技能后：

- 下界之星表示本次待分配点，进入时默认为 `0`。  
  The Nether Star represents pending points and starts at `0`.
- 点击 `+1` 或 `-1` 调整数量；点数不能超过余额、技能也不能超过全局上限。  
  Use `+1` and `-1` to adjust the amount. It cannot exceed the balance or the skill global maximum.
- 只有确认操作才会扣除余额并提高技能上限。余额为 `0` 时仍可进入页面查看，但不能完成分配。  
  Only confirmation deducts points and raises the cap. Players may still view the page with a zero balance, but cannot complete allocation.

## 构建 / Building

要求：JDK 与本项目 Gradle 配置兼容。  
Requirement: a JDK compatible with this project's Gradle configuration.

Windows：

```powershell
.\gradlew.bat :bukkit:shadowJar --console=plain
```

Linux/macOS：

```bash
./gradlew :bukkit:shadowJar --console=plain
```

构建产物位于 `build/libs/AuraSkills-<version>.jar`。  
The resulting JAR is located at `build/libs/AuraSkills-<version>.jar`.

## 开发文档 / Development documentation

完整的功能、模块映射、规则边界和验收步骤见：  
For full feature details, module mapping, rule boundaries, and acceptance steps, see:

- [新增功能说明 / New Features](NEW_FEATURES_ZH-CN.md)

## 上游项目 / Upstream project

AuraSkillsPlus 以 AuraSkills 为基础开发。原版 AuraSkills 的通用技能、属性、能力、奖励、战利品与 API 文档，请参阅其官方资源。  
AuraSkillsPlus is built on AuraSkills. Refer to the official AuraSkills resources for its general skills, stats, abilities, rewards, loot, and API documentation.

- [AuraSkills website](https://aurelium.dev/auraskills)
- [AuraSkills wiki](https://wiki.aurelium.dev/auraskills)
