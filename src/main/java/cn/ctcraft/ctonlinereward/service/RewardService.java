package cn.ctcraft.ctonlinereward.service;

import cn.ctcraft.ctonlinereward.CtOnlineReward;
import cn.ctcraft.ctonlinereward.database.YamlData;
import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Logger;

public class RewardService {
    private static final RewardService instance = new RewardService();
    CtOnlineReward ctOnlineReward;

    private RewardService() {
        ctOnlineReward = CtOnlineReward.getPlugin(CtOnlineReward.class);
    }

    public static RewardService getInstance() {
        return instance;
    }

    /**
     * 从奖励ID获取物品列表
     */
    public List<ItemStack> getItemStackFromRewardId(String rewardId) {
        YamlConfiguration rewardYaml = YamlData.rewardYaml;
        if (!rewardYaml.contains(rewardId)) {
            return null;
        }
        
        ConfigurationSection rewardSection = rewardYaml.getConfigurationSection(rewardId);
        if (rewardSection == null || !rewardSection.contains("items")) {
            return null;
        }

        List<ItemStack> items = new ArrayList<>();
        List<Map<?, ?>> itemsList = rewardSection.getMapList("items");
        
        for (Map<?, ?> itemMap : itemsList) {
            ItemStack item = createItemFromConfig(itemMap);
            if (item != null) {
                items.add(item);
            }
        }
        
        return items.isEmpty() ? null : items;
    }

    /**
     * 从配置创建物品
     */
    private ItemStack createItemFromConfig(Map<?, ?> config) {
        try {
            // 获取材质类型
            String typeStr = (String) config.get("type");
            if (typeStr == null) {
                ctOnlineReward.getLogger().warning("§c物品配置缺少 type 字段");
                return null;
            }

            // 使用 XMaterial 解析材质（跨版本兼容）
            Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(typeStr);
            if (!xMaterial.isPresent()) {
                ctOnlineReward.getLogger().warning("§c未知的材质类型: " + typeStr);
                return null;
            }

            ItemStack item = xMaterial.get().parseItem();
            if (item == null) {
                ctOnlineReward.getLogger().warning("§c无法创建物品: " + typeStr);
                return null;
            }

            // 设置数量
            int amount = config.containsKey("amount") ? ((Number) config.get("amount")).intValue() : 1;
            item.setAmount(amount);

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // 设置显示名称
                if (config.containsKey("name")) {
                    String name = (String) config.get("name");
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                }

                // 设置 Lore
                if (config.containsKey("lore")) {
                    List<?> loreList = (List<?>) config.get("lore");
                    List<String> lore = new ArrayList<>();
                    for (Object line : loreList) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', line.toString()));
                    }
                    meta.setLore(lore);
                }

                // 设置附魔
                if (config.containsKey("enchantments")) {
                    Map<?, ?> enchants = (Map<?, ?>) config.get("enchantments");
                    for (Map.Entry<?, ?> entry : enchants.entrySet()) {
                        String enchantName = entry.getKey().toString();
                        int level = ((Number) entry.getValue()).intValue();
                        
                        Optional<XEnchantment> xEnchant = XEnchantment.matchXEnchantment(enchantName);
                        if (xEnchant.isPresent()) {
                            Enchantment enchant = xEnchant.get().getEnchant();
                            if (enchant != null) {
                                meta.addEnchant(enchant, level, true);
                            }
                        }
                    }
                }

                // 设置物品标志
                if (config.containsKey("flags")) {
                    List<?> flags = (List<?>) config.get("flags");
                    for (Object flag : flags) {
                        try {
                            ItemFlag itemFlag = ItemFlag.valueOf(flag.toString().toUpperCase());
                            meta.addItemFlags(itemFlag);
                        } catch (IllegalArgumentException e) {
                            ctOnlineReward.getLogger().warning("§c未知的物品标志: " + flag);
                        }
                    }
                }

                // 设置自定义模型数据 (1.14+)
                if (config.containsKey("customModelData")) {
                    try {
                        int customModelData = ((Number) config.get("customModelData")).intValue();
                        meta.setCustomModelData(customModelData);
                    } catch (Exception e) {
                        // 旧版本不支持，忽略
                    }
                }

                // 设置是否无法破坏
                if (config.containsKey("unbreakable")) {
                    boolean unbreakable = (Boolean) config.get("unbreakable");
                    try {
                        meta.setUnbreakable(unbreakable);
                    } catch (Exception e) {
                        // 旧版本不支持，忽略
                    }
                }

                item.setItemMeta(meta);
            }

            return item;
            
        } catch (Exception e) {
            ctOnlineReward.getLogger().warning("§c创建物品时出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
