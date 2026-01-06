package cn.ctcraft.ctonlinereward.database;

import cn.ctcraft.ctonlinereward.CtOnlineReward;
import cn.ctcraft.ctonlinereward.utils.Util;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLiteBase implements DataService {
    private final CtOnlineReward ctOnlineReward = CtOnlineReward.getPlugin(CtOnlineReward.class);

    public SQLiteBase(){
        createTable();
    }

    public Connection getConnection() throws SQLException {
        return CtOnlineReward.hikariCPBase.getSqlConnectionPool().getConnection();
    }


    public void createTable() {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS `player_online_time`  (" +
                     "  `uuid` varchar(36) NOT NULL," +
                     "  `date` varchar(8) NOT NULL," +
                     "  `online_data` TEXT DEFAULT NULL," +
                     "  PRIMARY KEY (`uuid`, `date`)" +
                     ")")) {
            ps.executeUpdate();
            ctOnlineReward.getLogger().info("[CtOnlineReward] 每日玩家数据表创建成功!");
            
            // 创建索引以提升查询性能
            createIndexes(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void createIndexes(Connection connection) {
        try {
            // 为 uuid 创建索引（用于查询单个玩家的所有数据）
            try (PreparedStatement ps = connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_uuid ON player_online_time(uuid)")) {
                ps.executeUpdate();
            }
            
            // 为 date 创建索引（用于查询特定日期的数据）
            try (PreparedStatement ps = connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_date ON player_online_time(date)")) {
                ps.executeUpdate();
            }
            
            ctOnlineReward.getLogger().info("[CtOnlineReward] 数据库索引创建成功!");
        } catch (SQLException e) {
            ctOnlineReward.getLogger().warning("[CtOnlineReward] 索引创建失败: " + e.getMessage());
        }
    }

    @Override
    public int getPlayerOnlineTime(OfflinePlayer pLayer) {
        JsonObject playerOnlineData = getPlayerOnlineData(pLayer);
        JsonElement time = playerOnlineData.get("time");
        if (time == null){
            insertPlayerOnlineTime(pLayer,0);
            return 0;
        }
        return time.getAsInt();
    }

    @Override
    public void addPlayerOnlineTime(OfflinePlayer player, int time) {
        String date = Util.getDate();
        String sql = "UPDATE `player_online_time` SET `online_data` = ? WHERE `uuid` = ? AND `date` = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            JsonObject playerOnlineData = getPlayerOnlineData(player);
            playerOnlineData.addProperty("time", time);
            ps.setString(1, playerOnlineData.toString());
            ps.setString(2, player.getUniqueId().toString());
            ps.setString(3, date);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public JsonObject getPlayerOnlineData(OfflinePlayer player) {
        String date = Util.getDate();
        String sql = "SELECT `online_data` FROM `player_online_time` WHERE `uuid` = ? AND `date` = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String onlineData = rs.getString(1);
                    if (onlineData != null && !onlineData.isEmpty()) {
                        JsonParser jsonParser = new JsonParser();
                        JsonElement parse = jsonParser.parse(onlineData);
                        if (!parse.isJsonNull()) {
                            return parse.getAsJsonObject();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new JsonObject();
    }

    @Override
    public void insertPlayerOnlineTime(OfflinePlayer player, int time) {
        String date = Util.getDate();
        String sql = "INSERT INTO `player_online_time` (`uuid`, `date`, `online_data`) VALUES (?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, date);
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("time", time);
            jsonObject.add("reward", new JsonArray());
            ps.setString(3, jsonObject.toString());
            int i = ps.executeUpdate();
            if (i < 0) {
                ctOnlineReward.getLogger().warning("[CtOnlineReward] 数据库异常，数据插入失败！");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getPlayerRewardArray(OfflinePlayer player) {
        JsonObject playerOnlineData = getPlayerOnlineData(player);
        JsonElement reward = playerOnlineData.get("reward");
        List<String> rewardList = new ArrayList<>();
        if (reward == null) {
            return rewardList;
        }
        JsonArray asJsonArray = reward.getAsJsonArray();
        for (JsonElement jsonElement : asJsonArray) {
            rewardList.add(jsonElement.getAsString());
        }
        return rewardList;
    }

    @Override
    public boolean addRewardToPlayData(String rewardId, Player player) {
        String date = Util.getDate();
        String sql = "UPDATE `player_online_time` SET `online_data` = ? WHERE `uuid` = ? AND `date` = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            JsonObject playerOnlineData = getPlayerOnlineData(player);
            JsonElement reward = playerOnlineData.get("reward");
            if (reward == null) {
                playerOnlineData.add("reward", new JsonArray());
                reward = playerOnlineData.get("reward");
            }
            JsonArray asJsonArray = reward.getAsJsonArray();
            asJsonArray.add(rewardId);
            playerOnlineData.add("reward", asJsonArray);
            ps.setString(1, playerOnlineData.toString());
            ps.setString(2, player.getUniqueId().toString());
            ps.setString(3, date);
            int i = ps.executeUpdate();
            return i > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int getPlayerOnlineTimeWeek(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        List<String> weekString = Util.getWeekString();
        int onlineTime = 0;
        
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT `online_data` FROM `player_online_time` WHERE `uuid` = ? AND `date` IN (" +
                     String.join(",", weekString.stream().map(s -> "?").toArray(String[]::new)) + ")")) {
            ps.setString(1, uuid);
            for (int i = 0; i < weekString.size(); i++) {
                ps.setString(i + 2, weekString.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String onlineData = rs.getString(1);
                    if (onlineData != null && !onlineData.isEmpty()) {
                        JsonParser jsonParser = new JsonParser();
                        JsonElement parse = jsonParser.parse(onlineData);
                        if (!parse.isJsonNull()) {
                            int onlineTimeByJsonObject = Util.getOnlineTimeByJsonObject(parse.getAsJsonObject());
                            onlineTime += onlineTimeByJsonObject;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return onlineTime;
    }

    @Override
    public int getPlayerOnlineTimeMonth(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        List<String> monthString = Util.getMonthString();
        int onlineTime = 0;
        
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT `online_data` FROM `player_online_time` WHERE `uuid` = ? AND `date` IN (" +
                     String.join(",", monthString.stream().map(s -> "?").toArray(String[]::new)) + ")")) {
            ps.setString(1, uuid);
            for (int i = 0; i < monthString.size(); i++) {
                ps.setString(i + 2, monthString.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String onlineData = rs.getString(1);
                    if (onlineData != null && !onlineData.isEmpty()) {
                        JsonParser jsonParser = new JsonParser();
                        JsonElement parse = jsonParser.parse(onlineData);
                        if (!parse.isJsonNull()) {
                            int onlineTimeByJsonObject = Util.getOnlineTimeByJsonObject(parse.getAsJsonObject());
                            onlineTime += onlineTimeByJsonObject;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return onlineTime;
    }

    @Override
    public int getPlayerOnlineTimeAll(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        int onlineTime = 0;
        
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT `online_data` FROM `player_online_time` WHERE `uuid` = ?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String onlineData = rs.getString(1);
                    if (onlineData != null && !onlineData.isEmpty()) {
                        JsonParser jsonParser = new JsonParser();
                        JsonElement parse = jsonParser.parse(onlineData);
                        if (!parse.isJsonNull()) {
                            int onlineTimeByJsonObject = Util.getOnlineTimeByJsonObject(parse.getAsJsonObject());
                            onlineTime += onlineTimeByJsonObject;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return onlineTime;
    }
}
