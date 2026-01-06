package cn.ctcraft.ctonlinereward.command;

import cn.ctcraft.ctonlinereward.CtOnlineReward;
import cn.ctcraft.ctonlinereward.database.YamlData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab 补全处理器
 */
public class TabCompleter implements org.bukkit.command.TabCompleter {
    private static final TabCompleter instance = new TabCompleter();
    private final CtOnlineReward plugin = CtOnlineReward.getPlugin(CtOnlineReward.class);

    public static TabCompleter getInstance() {
        return instance;
    }

    private TabCompleter() {
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("cor")) {
            return null;
        }

        List<String> completions = new ArrayList<>();

        // 第一个参数：主命令
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            
            // 基础命令
            if (sender instanceof Player && sender.hasPermission("CtOnlineReward.cor")) {
                subCommands.add("open");
                subCommands.add("time");
            }
            
            // 管理命令
            if (sender.hasPermission("CtOnlineReward.reload")) {
                subCommands.add("reload");
            }
            
            if (sender instanceof Player) {
                subCommands.add("remind");
            }
            
            if (sender.hasPermission("CtOnlineReward.admin")) {
                subCommands.add("migrate");
                subCommands.add("migrateyaml");
                subCommands.add("dropoldtables");
            }

            // 过滤匹配的命令
            String input = args[0].toLowerCase();
            completions = subCommands.stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }
        // 第二个参数
        else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "open":
                    // 补全菜单 ID
                    if (sender.hasPermission("CtOnlineReward.cor")) {
                        String input = args[1].toLowerCase();
                        completions = YamlData.guiYaml.keySet().stream()
                                .filter(menuId -> sender.hasPermission("CtOnlineReward.open." + menuId))
                                .filter(menuId -> menuId.toLowerCase().startsWith(input))
                                .sorted()
                                .collect(Collectors.toList());
                    }
                    break;
                    
                case "remind":
                    // 补全 on/off
                    if (sender instanceof Player) {
                        completions = Arrays.asList("on", "off").stream()
                                .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    break;
                    
                case "dropoldtables":
                    // 补全 confirm
                    if (sender.hasPermission("CtOnlineReward.admin")) {
                        if ("confirm".startsWith(args[1].toLowerCase())) {
                            completions.add("confirm");
                        }
                    }
                    break;
            }
        }

        return completions;
    }
}
