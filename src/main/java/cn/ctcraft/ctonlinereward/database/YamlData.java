package cn.ctcraft.ctonlinereward.database;

import com.google.gson.JsonArray;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * YAML 配置数据存储类
 * 注意：此类仅用于存储配置文件数据，不再用于玩家数据存储
 */
public class YamlData {
    // GUI 配置文件
    public static Map<String, YamlConfiguration> guiYaml = new HashMap<>();
    
    // 奖励配置文件
    public static YamlConfiguration rewardYaml;
    
    // 提醒 JSON 数据
    public static JsonArray remindJson;
    
    // 时间限制配置 [开始时间, 结束时间]
    public static int[] timeLimit = {-1, -1};
}
