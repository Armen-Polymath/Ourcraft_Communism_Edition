package me.polymath;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class OurCraft extends JavaPlugin {

    private ShareManager shareManager;

    @Override
    public void onEnable() {
        this.shareManager = new ShareManager(this);
        getServer().getPluginManager().registerEvents(shareManager, this);

        getLogger().info("Ourcraft enabled.");
    }

    @Override
    public void onDisable() {
        if (shareManager != null) {
            shareManager.stopSharingAndClearAll(); // ensure clean shutdown behavior
        }
        getLogger().info("Ourcraft disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("share")) {
            shareManager.startSharingAndClearAll(sender);
            return true;
        }

        if (cmd.equals("unshare")) {
            shareManager.stopSharingAndClearAll(sender);
            return true;
        }

        return false;
    }
}