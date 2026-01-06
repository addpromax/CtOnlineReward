package cn.ctcraft.ctonlinereward.utils;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import org.bukkit.inventory.ItemStack;

public class ItemUtils {
    public static ItemStack createSkull(String texture) {
        // 使用 XMaterial 自动处理跨版本材质问题
        ItemStack item = XMaterial.PLAYER_HEAD.parseItem();
        if (item == null) {
            // 如果解析失败（极端情况），使用默认材质
            item = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
        }

        if (texture == null || texture.isEmpty()) {
            return item;
        }

        // 使用 XSkull 处理材质设置（兼容 1.20.5+ 和旧版本）
        try {
            Profileable profile = Profileable.detect(texture);
            XSkull.of(item).profile(profile).apply();
        } catch (Exception e) {
            // 如果设置失败，返回默认头颅
            e.printStackTrace();
        }
        
        return item;
    }
}
