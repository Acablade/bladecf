package me.acablade.bladecommandframework.test;

import me.acablade.bladecommandframework.annotations.Argument;
import me.acablade.bladecommandframework.annotations.CommandInfo;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandInfo(commandName = "test")
public class TestCommand {

	@CommandInfo(commandName = "bruh")
	public void onWorld(CommandSender sender, @Argument(name = "69420") String string, @Argument(name = "nice") int yes){
		sender.sendMessage(string);
		sender.sendMessage(yes + "");
	}

	@CommandInfo(commandName = "player",onlyPlayer = true)
	public void onPlayer(Player player, double targetPlayer){
		player.sendMessage(targetPlayer + "");
	}


}
