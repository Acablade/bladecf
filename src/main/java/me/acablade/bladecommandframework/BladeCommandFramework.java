package me.acablade.bladecommandframework;

import lombok.Getter;
import me.acablade.bladecommandframework.classes.BaseCommandHandler;
import me.acablade.bladecommandframework.test.TestCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BladeCommandFramework extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        BaseCommandHandler commandHandler = new BaseCommandHandler();

        commandHandler.registerCommand(new TestCommand());


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
