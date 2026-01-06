package cn.ctcraft.ctonlinereward.utils;

import cn.ctcraft.ctonlinereward.CtOnlineReward;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * MiniMessage 消息工具类
 */
public class MessageUtil {
    private static BukkitAudiences audiences;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    
    /**
     * 初始化 Adventure
     */
    public static void init(CtOnlineReward plugin) {
        audiences = BukkitAudiences.create(plugin);
    }
    
    /**
     * 关闭 Adventure
     */
    public static void close() {
        if (audiences != null) {
            audiences.close();
        }
    }
    
    /**
     * 解析 MiniMessage 格式的消息
     */
    public static Component parse(String message) {
        return miniMessage.deserialize(message);
    }
    
    /**
     * 解析带占位符的消息
     */
    public static Component parse(String message, Map<String, String> placeholders) {
        TagResolver.Builder builder = TagResolver.builder();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            builder.resolver(Placeholder.unparsed(entry.getKey(), entry.getValue()));
        }
        return miniMessage.deserialize(message, builder.build());
    }
    
    /**
     * 发送消息给玩家
     */
    public static void send(Player player, String message) {
        audiences.player(player).sendMessage(parse(message));
    }
    
    /**
     * 发送带占位符的消息给玩家
     */
    public static void send(Player player, String message, Map<String, String> placeholders) {
        audiences.player(player).sendMessage(parse(message, placeholders));
    }
    
    /**
     * 发送消息给命令发送者
     */
    public static void send(CommandSender sender, String message) {
        audiences.sender(sender).sendMessage(parse(message));
    }
    
    /**
     * 发送带占位符的消息给命令发送者
     */
    public static void send(CommandSender sender, String message, Map<String, String> placeholders) {
        audiences.sender(sender).sendMessage(parse(message, placeholders));
    }
    
    /**
     * 从语言文件获取并发送消息
     */
    public static void sendLang(CommandSender sender, String key) {
        String message = CtOnlineReward.languageHandler.getLang(key);
        send(sender, message);
    }
    
    /**
     * 从语言文件获取并发送带占位符的消息
     */
    public static void sendLang(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = CtOnlineReward.languageHandler.getLang(key);
        send(sender, message, placeholders);
    }
    
    /**
     * 从语言文件获取并发送消息给玩家
     */
    public static void sendLang(Player player, String key) {
        String message = CtOnlineReward.languageHandler.getLang(key);
        send(player, message);
    }
    
    /**
     * 从语言文件获取并发送带占位符的消息给玩家
     */
    public static void sendLang(Player player, String key, Map<String, String> placeholders) {
        String message = CtOnlineReward.languageHandler.getLang(key);
        send(player, message, placeholders);
    }
    
    /**
     * 转换传统颜色代码为 MiniMessage 格式（用于兼容旧配置）
     */
    public static String legacyToMiniMessage(String legacy) {
        if (legacy == null) return "";
        
        // 替换颜色代码
        return legacy
            .replace("&0", "<black>")
            .replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>")
            .replace("&5", "<dark_purple>")
            .replace("&6", "<gold>")
            .replace("&7", "<gray>")
            .replace("&8", "<dark_gray>")
            .replace("&9", "<blue>")
            .replace("&a", "<green>")
            .replace("&b", "<aqua>")
            .replace("&c", "<red>")
            .replace("&d", "<light_purple>")
            .replace("&e", "<yellow>")
            .replace("&f", "<white>")
            .replace("&l", "<bold>")
            .replace("&m", "<strikethrough>")
            .replace("&n", "<underlined>")
            .replace("&o", "<italic>")
            .replace("&r", "<reset>");
    }
    
    /**
     * 创建占位符 Map 的便捷方法
     */
    public static Map<String, String> placeholders(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
