package me.acablade.bladecommandframework.classes;

import lombok.Data;
import me.acablade.bladecommandframework.annotations.CommandInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
public class CommandData {

	private final String name;
	private final Class clazz;
	private final boolean valid;
	private final Object object;
	private String permission;
	private boolean playerOnly = false;
	private List<SubCommandData> subCommandDataList;

	public CommandData(String name, Object object){

		this.name = name;
		this.clazz = object.getClass();
		this.object = object;
		this.valid = clazz.isAnnotationPresent(CommandInfo.class);

		if(!valid) return;

		CommandInfo annotation = (CommandInfo) clazz.getAnnotation(CommandInfo.class);

		this.permission = annotation.permission();
		this.playerOnly = annotation.onlyPlayer();

		this.subCommandDataList = new ArrayList<>();

		registerSubcommands();

	}
	private void registerSubcommands(){
		for(Method method: clazz.getDeclaredMethods()){
			if(!method.isAnnotationPresent(CommandInfo.class)) continue;

			CommandInfo commandInfo = method.getAnnotation(CommandInfo.class);

			SubCommandData subCommandData = new SubCommandData(commandInfo.commandName(), getObject(), method);

			this.subCommandDataList.add(subCommandData);
		}
	}

	public Optional<SubCommandData> getSubCommand(String name){
		return this.subCommandDataList.stream().filter(scd -> scd.getName().equalsIgnoreCase(name)).findAny();
	}

}
