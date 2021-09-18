package deponn.depmount;

import org.bukkit.plugin.java.JavaPlugin;

public final class DepMount extends JavaPlugin {
    @Override
    public void onEnable() {
        // WorldEditがない場合はプラグインを無効化
        if (!getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
            // Plugin startup logic
            getLogger().info("DepMountを動作させるためにはWorldEditが必要です。");
            setEnabled(false);
            return;
        }

        // Plugin startup logic
        getLogger().info("DepMountが有効化されました。");

        // コマンドを登録
        getServer().getPluginCommand("/mountain").setExecutor(new CommandListener());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("DepMountが無効化されました。");
    }
}
