package cn.ctcraft.ctonlinereward.database;

import cn.ctcraft.ctonlinereward.CtOnlineReward;
import cn.ctcraft.ctonlinereward.utils.Util;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class MysqlBase implements DataService {
    private CtOnlineReward ctOnlineReward = CtOnlineReward.getPlugin(CtOnlineReward.class);

    public MysqlBase() {
        createTable();
    }

    public Connection getConnection() throws SQLException {
        return CtOnlineReward.hikariCPBase.getSqlConnectionPool().getConnection();
    }


    public void createTable() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS `player_online_time`  (" +
                    "  `uuid` varchar(36) NOT NULL COMMENT '玩家uuid'," +
                    "  `date` varchar(8) NOT NULL COMMENT '日期(yyyyMMdd)'," +
                    "  `online_data` TEXT DEFAULT NULL COMMENT '在线数据'," +
                    "  PRIMARY KEY (`uuid`, `date`)," +
                    "  INDEX `idx_date` (`date`)," +
                    "  INDEX `idx_uuid` (`uuid`)" +
                    ") ENGINE = InnoDB CHARACTER SET = utf8mb4";
            statement.executeUpdate(sql);
            ctOnlineReward.getLogger().info("[CtOnlineReward] 每日玩家数据表创建成功!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public int getPlayerOnlineTime(OfflinePlayer player) {
        JsonObject playerOnlineData = getPlayerOnlineData(player);
        JsonElement time = playerOnlineData.get("time");
        int onlineTime = time != null ? time.getAsInt() : -1;

        if (onlineTime == -1) {
            insertPlayerOnlineTime(player, 0);
        }

        return onlineTime;
    }

    @Override
    public void addPlayerOnlineTime(OfflinePlayer player, int time) {
        String date = Util.getDate();
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE `player_online_time` SET `online_data` = ? WHERE `uuid` = ? AND `date` = ?")) {
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
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT `online_data` FROM `player_online_time` WHERE `uuid` = ? AND `date` = ?")) {
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
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT INTO `player_online_time` (`uuid`, `date`, `online_data`) VALUES (?, ?, ?)")) {
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
        if (reward != null && reward.isJsonArray()) {
            JsonArray jsonArray = reward.getAsJsonArray();
            for (JsonElement jsonElement : jsonArray) {
                rewardList.add(jsonElement.getAsString());
            }
        }
        return rewardList;
    }


    @Override
    public boolean addRewardToPlayData(String rewardId, Player player) {
        String date = Util.getDate();
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE `player_online_time` SET `online_data` = ? WHERE `uuid` = ? AND `date` = ?")) {
            JsonObject playerOnlineData = getPlayerOnlineData(player);
            JsonElement reward = playerOnlineData.get("reward");
            if (reward == null) {
                playerOnlineData.add("reward", new JsonArray());
                reward = playerOnlineData.get("reward");
            }
            if (reward.isJsonArray()) {
                JsonArray rewardArray = reward.getAsJsonArray();
                rewardArray.add(rewardId);
            }
            ps.setString(1, playerOnlineData.toString());
            ps.setString(2, player.getUniqueId().toString());
            ps.setString(3, date);
            int rowsUpdated = ps.executeUpdate();
            return rowsUpdated > 0;
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
