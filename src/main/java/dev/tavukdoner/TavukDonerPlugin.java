package dev.tavukdoner;

import dev.tavukdoner.listener.ConsumeListener;
import dev.tavukdoner.recipe.RecipeManager;
import dev.tavukdoner.skewer.SkewerListener;
import dev.tavukdoner.skewer.SkewerManager;
import dev.tavukdoner.skewer.SkewerRenderer;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class TavukDonerPlugin extends JavaPlugin {

    private SkewerManager skewerManager;
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Messages.init(this);
        dev.tavukdoner.protection.Protection.init(this);

        SkewerRenderer renderer = new SkewerRenderer(this);
        this.skewerManager = new SkewerManager(this, renderer);
        this.skewerManager.onEnable();

        getServer().getPluginManager().registerEvents(new SkewerListener(this, skewerManager), this);

        this.recipeManager = new RecipeManager(this);
        this.recipeManager.register();
        getServer().getPluginManager().registerEvents(this.recipeManager, this);

        getServer().getPluginManager().registerEvents(new ConsumeListener(this), this);

        TavukDonerCommand commandHandler = new TavukDonerCommand(this);
        PluginCommand command = getCommand("tavukdoner");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        }

        getLogger().info("TavukDoner v" + getPluginMeta().getVersion() + " basariyla aktif edildi.");
    }

    @Override
    public void onDisable() {
        if (recipeManager != null) {
            recipeManager.unregister();
        }
        if (skewerManager != null) {
            skewerManager.onDisable();
        }
    }

    public SkewerManager getSkewerManager() {
        return skewerManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
}
