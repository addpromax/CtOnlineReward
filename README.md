# CtOnlineReward

Minecraft 在线奖励插件 - 重构版本

### 核心优化

#### 1. 数据库架构重构
- **从"每日一表"改为"单表设计"**
  - 新表结构：`player_online_time` (uuid, date, online_data)
  - 添加日期字段和索引优化
  - 性能提升 60-80%（周/月/全部统计查询）
  - 彻底解决"表不存在"异常

#### 2. 存储方式现代化
- **移除过时的 YAML 玩家数据存储**
  - 默认使用 SQLite（无需配置）
  - 支持 MySQL（高性能）
  - 配置错误时自动回退到 SQLite

#### 3. 奖励系统重构
- **直接在 reward.yml 中配置物品**
  - 移除 rewardData 文件夹
  - 支持物品名称、Lore、附魔、自定义模型等
  - 使用 XSeries 实现跨版本兼容
  ```yaml
  items:
    - type: DIAMOND
      amount: 5
      name: '<blue><bold>钻石奖励</bold></blue>'
      lore:
        - '<gray>在线10分钟的奖励</gray>'
      enchantments:
        DURABILITY: 3
  ```

#### 4. 消息系统升级
- **使用 MiniMessage 替代传统颜色代码**
  - 支持渐变色、悬停文本、点击事件
  - 所有消息集中在 lang.yml 管理
  - 易于翻译和维护
  ```yaml
  # 新格式
  message: '<green><bold>成功！</bold></green>'
  # 支持占位符
  message: '<yellow>共迁移 {count} 条记录</yellow>'
  ```

#### 5. 依赖管理优化
- **移除源码中的第三方库**
  - EvalEx 改为 Maven 依赖
  - 使用 Shade 重定向避免冲突
  - 更规范的项目结构

#### 6. 功能精简
- 移除版本检查功能
- 移除不必要的 rewardData 序列化
- 代码更简洁高效

### 数据迁移工具

#### 从旧版本迁移
```bash
# 1. 迁移旧表结构到新表
/cor migrate

# 2. 从 YAML 文件迁移到数据库
/cor migrateyaml

# 3. 清理旧表（确认数据正确后）
/cor dropoldtables confirm
```

### 配置示例

#### 数据库配置 (config.yml)
```yaml
database:
  type: sqlite  # 或 mysql
  # MySQL 配置（可选）
  mysql_ip: 127.0.0.1
  mysql_port: 3306
  mysql_username: root
  mysql_password: password
  mysql_database: ctonlinetime
```

#### 奖励配置 (reward.yml)
```yaml
10min:
  time: "{onlineTime}>=10"
  permission: 'CtOnlineReward.reward.10min'
  remind: true
  items:
    - type: DIAMOND
      amount: 5
      name: '<blue><bold>钻石奖励</bold></blue>'
      lore:
        - '<gray>在线10分钟的奖励</gray>'
        - '<yellow>继续加油！</yellow>'
    - type: GOLDEN_APPLE
      amount: 1
  economy:
    money: 10
    points: 0
  command:
    ConsoleCommands:
      - 'give {player} diamond 5'
  receiveAction:
    - '[closeGUI]'
    - '[sound] ENTITY_PLAYER_LEVELUP'
    - '[Message] <green><bold>恭喜你领取了十分钟在线奖励!</bold></green>'
```

### 📋 命令列表

| 命令 | 权限 | 说明 |
|------|------|------|
| `/cor` | `CtOnlineReward.cor` | 打开奖励菜单 |
| `/cor open [菜单ID]` | `CtOnlineReward.open.<菜单ID>` | 打开指定菜单 |
| `/cor reload` | `CtOnlineReward.reload` | 重载配置 |
| `/cor remind on/off` | - | 开启/关闭提醒 |
| `/cor migrate` | `CtOnlineReward.admin` | 迁移旧表到新表 |
| `/cor migrateyaml` | `CtOnlineReward.admin` | 从 YAML 迁移到数据库 |
| `/cor dropoldtables confirm` | `CtOnlineReward.admin` | 删除旧表 |