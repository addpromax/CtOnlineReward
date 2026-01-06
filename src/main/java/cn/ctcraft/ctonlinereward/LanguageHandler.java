package cn.ctcraft.ctonlinereward;

import org.bukkit.configuration.file.YamlConfiguration;

public class LanguageHandler {
    private CtOnlineReward ctOnlineReward = CtOnlineReward.getPlugin(CtOnlineReward.class);
    private YamlConfiguration langYaml = CtOnlineReward.lang;
    private String prefix;

    public LanguageHandler() {
        prefix = langYaml.getString("prefix", "[在线奖励]");
    }

    public String getLang(String key) {
        String string = langYaml.getString(key);
        if (string == null) {
            ctOnlineReward.getLogger().warning("语言文件中未找到键: " + key);
            return "<red>Missing translation: " + key + "</red>";
        }
        return string.replace("{prefix}", prefix);
    }
}
