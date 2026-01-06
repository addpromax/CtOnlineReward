package cn.ctcraft.ctonlinereward.command;

import cn.ctcraft.ctonlinereward.CtOnlineReward;
import cn.ctcraft.ctonlinereward.database.YamlData;
import cn.ctcraft.ctonlinereward.inventory.InventoryFactory;
import cn.ctcraft.ctonlinereward.utils.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public class CommandExecute {
    private static final CommandExecute instance = new CommandExecute();
    private final CtOnlineReward ctOnlineReward;
    
    private CommandExecute(){
        ctOnlineReward = CtOnlineReward.getPlugin(CtOnlineReward.class);
    }

    public static CommandExecute getInstance(){
        return instance;
    }

    public void openInventory(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (sender instanceof Player) {
                Inventory menu = InventoryFactory.build("menu.yml", (Player) sender);
                ((Player) sender).openInventory(menu);
            } else {
                MessageUtil.sendLang(sender, "command.player-only");
            }
            return;
        }

        if (args.length == 2) {
            String menuId = args[1];
            Map<String, YamlConfiguration> guiYaml = YamlData.guiYaml;
            if (!guiYaml.containsKey(menuId)) {
                MessageUtil.sendLang(sender, "command.menu-not-found");
                return;
            }

            boolean hasPermission = sender.hasPermission("CtOnlineReward.open." + menuId);
            if (!hasPermission) {
                MessageUtil.sendLang(sender, "command.no-permission");
                return;
            }

            try {
                if (sender instanceof Player) {
                    Inventory build = InventoryFactory.build(menuId, (Player) sender);
                    ((Player) sender).openInventory(build);
                } else {
                    MessageUtil.sendLang(sender, "command.player-only");
                }
            } catch (Exception e) {
                MessageUtil.sendLang(sender, "command.menu-error", 
                    MessageUtil.placeholders("menu", menuId));
                e.printStackTrace();
            }
            return;
        }

        MessageUtil.sendLang(sender, "command.invalid-args", 
            MessageUtil.placeholders("usage", "/cor open [菜单ID(可选)]"));
    }
}
