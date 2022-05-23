package me.acablade.bladecommandframework.classes;

import lombok.*;
import me.acablade.bladecommandframework.annotations.CommandInfo;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Data
public class SubCommandData {

    private final String name;
    private final Method method;
    private final Object parent;
    private final boolean valid;
    private String permission;
    private boolean playerOnly = false;
    private Stack<Class> classStack = null;

    public SubCommandData(String name, Object parent,Method method){

        this.name = name;
        this.method = method;
        this.parent = parent;
        this.valid = method.isAnnotationPresent(CommandInfo.class);

        if(!valid) return;

        CommandInfo annotation = method.getAnnotation(CommandInfo.class);

        this.permission = annotation.permission();
        this.playerOnly = annotation.onlyPlayer();

        this.classStack = new Stack<>();

        for (int i = 1; i < method.getParameterCount(); i++) {
            classStack.add(method.getParameterTypes()[i]);
        }

    }

    public void execute(CommandSender commandSender,Object... args) throws RuntimeException{
        Object senderParam = commandSender;
        if(getMethod().getParameterTypes()[0] == Player.class && commandSender instanceof Player){
            senderParam = (Player) commandSender;
        }
        if(!commandSender.hasPermission(getPermission())){
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&cYou don't have enough permissions!"));
            return;
        }
        if(isPlayerOnly() && !(commandSender instanceof Player)){
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&cOnly players can send this command!"));
            return;
        }

        List<Object> argsList = new LinkedList<>();

        argsList.add(senderParam);
        for (int i = 0; i < args.length; i++) {
            Optional param = (Optional) args[i];
            if(!param.isPresent()){
                throw new RuntimeException(String.format("Param (%s) returned null",i));
            }
            Object arg = param.get();
            Class expectedArg = getMethod().getParameterTypes()[getMethod().getParameterTypes().length-1-i];
            if(arg==null){
                throw new IllegalArgumentException(String.format("Expected (%s) Found (%s)",expectedArg.getSimpleName(), null));
            }else if(arg.getClass().isInstance(expectedArg)){
                throw new IllegalArgumentException(String.format("Expected (%s) Found (%s)",expectedArg.getSimpleName(), arg.getClass().getSimpleName()));
            }
            argsList.add(arg);
        }

        try {
            getMethod().invoke(getParent(),argsList.toArray(new Object[0]));
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
