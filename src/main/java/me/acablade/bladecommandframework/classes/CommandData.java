package me.acablade.bladecommandframework.classes;

import lombok.Data;
import me.acablade.bladecommandframework.annotations.BaseCommand;
import me.acablade.bladecommandframework.annotations.CommandInfo;
import me.acablade.bladecommandframework.annotations.FallbackCommand;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

@Data
public class CommandData {

	private final String name;
	private final Class clazz;
	private final boolean valid;
	private final Object object;
	private String permission;
	private boolean playerOnly = false;
	private SubCommandData fallbackCommand;
	private SubCommandData baseCommand;
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
			if(this.baseCommand==null&& method.isAnnotationPresent(BaseCommand.class)){
				this.baseCommand = SubCommandData.builder()
						.classStack(new Stack<>())
						.method(method)
						.parent(getObject())
						.valid(true)
						.name("null")
						.permission("")
						.playerOnly(((CommandInfo)clazz.getAnnotation(CommandInfo.class)).onlyPlayer())
						.build();
			}
			if(!method.isAnnotationPresent(CommandInfo.class)){

				continue;
			}

			CommandInfo commandInfo = method.getAnnotation(CommandInfo.class);

			SubCommandData subCommandData = new SubCommandData(commandInfo.commandName(), getObject(), method);

			this.subCommandDataList.add(subCommandData);

			if(this.fallbackCommand!=null && method.isAnnotationPresent(FallbackCommand.class)){
				this.fallbackCommand = subCommandData;
			}




		}
	}

	public Optional<SubCommandData> getSubCommand(String name){
		return this.subCommandDataList.stream().filter(scd -> scd.getName().equalsIgnoreCase(name)).findAny();
	}

}
