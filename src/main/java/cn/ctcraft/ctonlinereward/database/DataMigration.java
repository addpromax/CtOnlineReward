package cn.ctcraft.ctonlinereward.database;

import cn.ctcraft.ctonlinereward.CtOnlineReward;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 数据迁移工具类
 * 用于将旧的每日一表结构迁移到新的单表结构
 * 以及从 YAML 文件迁移到数据库
 */
public class DataMigration {
    private final CtOnlineReward plugin = CtOnlineReward.getPlugin(CtOnlineReward.class);

    /**
     * 从 YAML 文件迁移到数据库
     */
    public int migrateYamlToDatabase() {
        File playerDataFolder = new File(plugin.getDataFolder(), "playerData");
        if (!playerDataFolder.exists() || !playerDataFolder.isDirectory()) {
            plugin.getLogger().warning("§c未找到 playerData 文件夹");
            return 0;
        }

        File[] yamlFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (yamlFiles == null || yamlFiles.length == 0) {
            plugin.getLogger().warning("§c未找到任何 YAML 数据文件");
            return 0;
        }

        plugin.getLogger().info("§e发现 " + yamlFiles.length + " 个 YAML 文件");
        int totalMigrated = 0;

        try (Connection connection = CtOnlineReward.hikariCPBase.getSqlConnectionPool().getConnection()) {
            String dbType = plugin.getConfig().getString("database.type", "sqlite");
            String insertSql;
            
            if (dbType.equalsIgnoreCase("mysql")) {
                insertSql = "INSERT INTO `player_online_time` (`uuid`, `date`, `online_data`) VALUES (?, ?, ?) " +
                           "ON DUPLICATE KEY UPDATE `online_data` = VALUES(`online_data`)";
            } else {
                insertSql = "INSERT OR REPLACE INTO `player_online_time` (`uuid`, `date`, `online_data`) VALUES (?, ?, ?)";
            }

            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                for (File yamlFile : yamlFiles) {
                    String fileName = yamlFile.getName();
                    String date = fileName.replace(".yml", "");
                    
                    // 验证日期格式（8位数字）
                    if (!date.matches("\\d{8}")) {
                        plugin.getLogger().warning("§c跳过非日期格式文件: " + fileName);
                        continue;
                    }

                    try {
                        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(yamlFile);
                        Set<String> uuids = yaml.getKeys(false);
                        
                        int fileCount = 0;
                        for (String uuid : uuids) {
                            ConfigurationSection section = yaml.getConfigurationSection(uuid);
                            if (section == null) continue;

                            // 构建 JSON 数据
                            JsonObject jsonObject = new JsonObject();
                            
                            // 获取在线时间
                            int time = section.getInt("time", 0);
                            jsonObject.addProperty("time", time);
                            
                            // 获取奖励列表
                            List<String> rewards = section.getStringList("reward");
                            JsonArray rewardArray = new JsonArray();
                            for (String reward : rewards) {
                                rewardArray.add(reward);
                            }
                            jsonObject.add("reward", rewardArray);

                            // 插入数据库
                            ps.setString(1, uuid);
                            ps.setString(2, date);
                            ps.setString(3, jsonObject.toString());
                            ps.addBatch();
                            fileCount++;
                            totalMigrated++;

                            // 每1000条执行一次批处理
                            if (fileCount % 1000 == 0) {
                                ps.executeBatch();
                            }
                        }

                        // 执行剩余的批处理
                        if (fileCount % 1000 != 0) {
                            ps.executeBatch();
                        }

                        plugin.getLogger().info("§a已迁移文件 " + fileName + "，共 " + fileCount + " 条记录");
                        
                    } catch (Exception e) {
                        plugin.getLogger().severe("§c迁移文件 " + fileName + " 失败：" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            plugin.getLogger().info("§a§lYAML 数据迁移完成！共迁移 " + totalMigrated + " 条记录");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("§c数据库连接失败：" + e.getMessage());
            e.printStackTrace();
        }

        return totalMigrated;
    }

    /**
     * 执行MySQL数据迁移
     */
    public void migrateMysqlData() {
        try (Connection connection = CtOnlineReward.hikariCPBase.getSqlConnectionPool().getConnection()) {
            // 获取所有旧表
            List<String> oldTables = getOldTables(connection);
            
            if (oldTables.isEmpty()) {
                plugin.getLogger().info("§a没有发现需要迁移的旧表");
                return;
            }

            plugin.getLogger().info("§e开始迁移数据，发现 " + oldTables.size() + " 个旧表");
            
            int totalMigrated = 0;
            for (String tableName : oldTables) {
                int count = migrateTable(connection, tableName);
                totalMigrated += count;
                plugin.getLogger().info("§a已迁移表 " + tableName + "，共 " + count + " 条记录");
            }
            
            plugin.getLogger().info("§a§l数据迁移完成！共迁移 " + totalMigrated + " 条记录");
            plugin.getLogger().info("§e提示：迁移完成后，建议备份旧表后删除以释放空间");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("§c数据迁移失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 执行SQLite数据迁移
     */
    public void migrateSQLiteData() {
        try (Connection connection = CtOnlineReward.hikariCPBase.getSqlConnectionPool().getConnection()) {
            // 获取所有旧表
            List<String> oldTables = getOldTables(connection);
            
            if (oldTables.isEmpty()) {
                plugin.getLogger().info("§a没有发现需要迁移的旧表");
                return;
            }

            plugin.getLogger().info("§e开始迁移数据，发现 " + oldTables.size() + " 个旧表");
            
            int totalMigrated = 0;
            for (String tableName : oldTables) {
                int count = migrateTable(connection, tableName);
                totalMigrated += count;
                plugin.getLogger().info("§a已迁移表 " + tableName + "，共 " + count + " 条记录");
            }
            
            plugin.getLogger().info("§a§l数据迁移完成！共迁移 " + totalMigrated + " 条记录");
            plugin.getLogger().info("§e提示：迁移完成后，建议备份旧表后删除以释放空间");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("§c数据迁移失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取所有旧表（日期格式的表名）
     */
    private List<String> getOldTables(Connection connection) throws SQLException {
        List<String> oldTables = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        
        try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                // 匹配日期格式的表名（8位数字）
                if (tableName.matches("\\d{8}") && !tableName.equals("player_online_time")) {
                    oldTables.add(tableName);
                }
            }
        }
        
        return oldTables;
    }

    /**
     * 迁移单个表的数据
     */
    private int migrateTable(Connection connection, String tableName) throws SQLException {
        String selectSql = "SELECT `uuid`, `online_data` FROM `" + tableName + "`";
        String insertSql = "INSERT INTO `player_online_time` (`uuid`, `date`, `online_data`) VALUES (?, ?, ?) " +
                          "ON DUPLICATE KEY UPDATE `online_data` = VALUES(`online_data`)";
        
        int count = 0;
        
        try (PreparedStatement selectPs = connection.prepareStatement(selectSql);
             PreparedStatement insertPs = connection.prepareStatement(insertSql);
             ResultSet rs = selectPs.executeQuery()) {
            
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String onlineData = rs.getString("online_data");
                
                insertPs.setString(1, uuid);
                insertPs.setString(2, tableName); // 表名就是日期
                insertPs.setString(3, onlineData);
                insertPs.addBatch();
                count++;
                
                // 每1000条执行一次批处理
                if (count % 1000 == 0) {
                    insertPs.executeBatch();
                }
            }
            
            // 执行剩余的批处理
            if (count % 1000 != 0) {
                insertPs.executeBatch();
            }
        }
        
        return count;
    }

    /**
     * 删除旧表（请谨慎使用，建议先备份）
     */
    public void dropOldTables() {
        try (Connection connection = CtOnlineReward.hikariCPBase.getSqlConnectionPool().getConnection()) {
            List<String> oldTables = getOldTables(connection);
            
            if (oldTables.isEmpty()) {
                plugin.getLogger().info("§a没有发现需要删除的旧表");
                return;
            }

            plugin.getLogger().warning("§c§l警告：即将删除 " + oldTables.size() + " 个旧表！");
            
            for (String tableName : oldTables) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate("DROP TABLE IF EXISTS `" + tableName + "`");
                    plugin.getLogger().info("§a已删除旧表：" + tableName);
                }
            }
            
            plugin.getLogger().info("§a§l旧表删除完成！");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("§c删除旧表失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
