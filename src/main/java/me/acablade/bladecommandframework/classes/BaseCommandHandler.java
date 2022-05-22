package me.acablade.bladecommandframework.classes;

import me.acablade.bladecommandframework.annotations.Argument;
import me.acablade.bladecommandframework.annotations.CommandInfo;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class BaseCommandHandler implements CommandExecutor, TabExecutor {

    private Map<Class, ValueResolver> valueResolverMap;
    private List<CommandData> commandDataList;

    public BaseCommandHandler(){
        this.valueResolverMap = new HashMap<>();
        this.commandDataList = new ArrayList<>();
        registerValueResolver(Player.class, args -> Bukkit.getPlayer((String)args.pop()));
        registerValueResolver(World.class, args -> (World)Bukkit.getWorld((String)args.pop()));
        registerValueResolver(int.class, args -> Integer.parseInt((String)args.pop()));
        registerValueResolver(double.class, args -> Double.parseDouble((String)args.pop()));
        registerValueResolver(float.class, args -> Float.parseFloat((String)args.pop()));
        registerValueResolver(String.class, args -> {
            String start = (String) args.pop();
            LinkedList<String> linkedList = new LinkedList<>();
            if(start.equals("\"")) return start;
            if(start.endsWith("\"") && !start.startsWith("\"")){
                String curr = "";
                curr = start.substring(0,start.length()-1);
                linkedList.add(curr);
                while(!args.isEmpty()&&!(curr = (String)args.pop()).startsWith("\"")){
                    linkedList.add(curr);
                }
                curr = curr.substring(1);
                linkedList.add(curr);
            }else if (!start.endsWith("\"") && !start.startsWith("\"")){
                return start;
            }else if (start.endsWith("\"") && start.startsWith("\"")){
                return start.substring(1,start.length()-1);
            }
            Collections.reverse(linkedList);
            return String.join(" ", linkedList);
        });
    }
    public void registerCommand(Object commandClass){

        if(!commandClass.getClass().isAnnotationPresent(CommandInfo.class)) return;

        CommandInfo commandInfo = commandClass.getClass().getAnnotation(CommandInfo.class);

        commandDataList.add(new CommandData(commandInfo.commandName(),commandClass));
        PluginCommand command = Bukkit.getServer().getPluginCommand(commandInfo.commandName());
        command.setExecutor(this);
        command.setTabCompleter(this);

    }

    public void registerValueResolver(Class clazz, ValueResolver valueResolver){
        valueResolverMap.put(clazz,valueResolver);
    }

    public Stack<String> parseArgs(String command){
        return parseArgs(command.split(" "));
    }

    public Stack<String> parseArgs(String[] args){
        Stack<String> stringStack = new Stack<>();
        LinkedList<String> tmp = new LinkedList<>(Arrays.asList(args));
        tmp.remove(0);
        stringStack.addAll(tmp);
        return stringStack;
    }

    public <T> T resolveValue(Class<? extends T> clazz, Stack<String> args){
        return (T) valueResolverMap.get(clazz).resolve(args);
    }

    public void executeCommand(CommandSender sender, CommandData commandData, String[] args){
        Optional<SubCommandData> oscd = commandData.getSubCommand(args[0]);
        if(!oscd.isPresent()) return;
        SubCommandData scd = oscd.get();

        Stack<String> argStack = parseArgs(args);
        Stack<Class> classStack = new Stack<>();

        classStack.addAll(scd.getClassStack());

        List<Object> objects = new LinkedList<>();

        while(!classStack.isEmpty()){
            Class clazz = classStack.pop();
            objects.add(resolveValue(clazz, argStack));
        }

        Collections.reverse(objects);

        scd.execute(sender,objects.toArray(new Object[0]));


    }

    private Optional<CommandData> findCommandData(String name){
        return commandDataList.stream().filter(cd -> cd.getName().equalsIgnoreCase(name)).findAny();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Optional<CommandData> ocd = findCommandData(command.getName());

        if(!ocd.isPresent()) return false;

        CommandData cd = ocd.get();

        executeCommand(sender,cd,args);


        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        Optional<CommandData> ocd = findCommandData(command.getName());

        if(!ocd.isPresent()) return Collections.emptyList();

        CommandData cd = ocd.get();

        if(args.length==1) return cd.getSubCommandDataList().stream().map(SubCommandData::getName).collect(Collectors.toList());
        else if(args.length>1&&cd.getSubCommand(args[0]).isPresent()) {
            Optional<SubCommandData> oscd = cd.getSubCommand(args[0]);
            if(!oscd.isPresent()) return Collections.emptyList();
            SubCommandData subCommandData = oscd.get();
            Method method = subCommandData.getMethod();
            if(subCommandData.getClassStack().size()<args.length-1) return Collections.emptyList();
            String tabComplete = subCommandData.getClassStack().get(args.length-2).getSimpleName();
            Annotation[][] annotations = method.getParameterAnnotations();
            for (Annotation ann: annotations[args.length-1]){
                if(!(ann instanceof Argument)) continue;
                Argument argument = (Argument) ann;
                tabComplete = argument.name();
                break;
            }
            return Collections.singletonList(tabComplete);
        }

        return Collections.emptyList();
    }
}
