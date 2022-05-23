package me.acablade.bladecommandframework.classes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
@Getter
public class CommandActor {

	private final CommandSender commandSender;


	public void reply(String msg){
		commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',msg));
	}

	public Player asPlayer(){
		return (Player) commandSender;
	}

	public Player requirePlayer(){
		if(!isPlayer())
			throw new RuntimeException(String.format("Actor not player (%s)",commandSender.getName()));
		return asPlayer();
	}

	public boolean isPlayer(){
		return commandSender instanceof Player;
	}

}
