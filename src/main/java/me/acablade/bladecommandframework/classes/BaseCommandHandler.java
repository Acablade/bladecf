package me.acablade.bladecommandframework.classes;

import me.acablade.bladecommandframework.annotations.Argument;
import me.acablade.bladecommandframework.annotations.CommandInfo;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class BaseCommandHandler implements CommandExecutor, TabExecutor {

    private Map<Class, ValueResolver> valueResolverMap;
    private List<CommandData> commandDataList;

    public BaseCommandHandler(){
        this.valueResolverMap = new HashMap<>();
        this.commandDataList = new ArrayList<>();
        registerValueResolver(Player.class, ctx -> {
            String value = ctx.pop();
            if(value.equalsIgnoreCase("self") || value.equalsIgnoreCase("me")){
                return ctx.actor().requirePlayer();
            }
            Player player = Bukkit.getPlayerExact(value);
            if(player==null)
                throw new RuntimeException(String.format("Player not found (%s)", value));
            return player;
        });
        registerValueResolver(World.class, args -> (World)Bukkit.getWorld(args.pop()));
        registerValueResolver(int.class, ValueResolver.ValueResolverContext::popInt);
        registerValueResolver(double.class, ValueResolver.ValueResolverContext::popDouble);
        registerValueResolver(float.class, ValueResolver.ValueResolverContext::popFloat);
        registerValueResolver(boolean.class, bool());
        registerValueResolver(String.class, ctx -> {
            String start = ctx.pop();
            LinkedList<String> linkedList = new LinkedList<>();
            if(start.equals("\"")) return start;
            if(start.endsWith("\"") && !start.startsWith("\"")){
                String curr = "";
                curr = start.substring(0,start.length()-1);
                linkedList.add(curr);
                while(!ctx.arguments().isEmpty()&&!(curr = ctx.pop()).startsWith("\"")){
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

    private ValueResolver<Boolean> bool(){
        return context -> {
            String v = context.pop();
            switch (v.toLowerCase()) {
                case "true":
                case "yes":
                case "ye":
                case "y":
                case "yeah":
                case "ofcourse":
                case "mhm":
                    return true;
                case "false":
                case "no":
                case "n":
                    return false;
                default:
                    throw new RuntimeException(String.format("Boolean not found with value (%s)",v));
            }
        };
    }

    public void registerCommand(Object commandClass){

        if(!commandClass.getClass().isAnnotationPresent(CommandInfo.class)) return;

        CommandInfo commandInfo = commandClass.getClass().getAnnotation(CommandInfo.class);

        commandDataList.add(new CommandData(commandInfo.commandName(),commandClass));

        //maybe do some shit w command map ??
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

    private StackTraceSanitizer stackTraceSanitizer = StackTraceSanitizer.defaultSanitizer();

    public <T> Optional<T> resolveValue(Class<? extends T> clazz, Stack<String> args, CommandActor actor){
        try{
            return (Optional<T>) Optional.ofNullable(valueResolverMap.get(clazz).resolve(new ValueResolverContext(args,actor)));
        }catch (Throwable t){
            stackTraceSanitizer.sanitize(t);
            t.printStackTrace();
            return Optional.empty();
        }
    }


    private static final class ValueResolverContext implements ValueResolver.ValueResolverContext {

        private final Stack<String> args;
        private final CommandActor actor;

        public ValueResolverContext(Stack<String> args, CommandActor actor) {
            this.args = args;
            this.actor = actor;
        }

        @Override
        public Stack<String> arguments() {
            return args;
        }

        @Override
        public String pop() {
            return (String)args.pop();
        }

        @Override
        public int popInt() {
            return Integer.parseInt(pop());
        }

        @Override
        public double popDouble() {
            return Double.parseDouble(pop());
        }

        @Override
        public byte popByte() {
            return Byte.parseByte(pop());
        }

        @Override
        public short popShort() {
            return Short.parseShort(pop());
        }

        @Override
        public float popFloat() {
            return Float.parseFloat(pop());
        }

        @Override
        public long popLong() {
            return Long.parseLong(pop());
        }

        @Override
        public CommandActor actor() {
            return actor;
        }
    }

    public void executeCommand(CommandSender sender, CommandData commandData, String[] args) throws RuntimeException {
        CommandActor actor = new CommandActor(sender);

        if(args.length==0){
            commandData.getFallbackCommand().execute(actor);
            return;
        }
        Optional<SubCommandData> oscd = commandData.getSubCommand(args[0]);
        if(!oscd.isPresent()) return;


        if(commandData.isPlayerOnly()&&!actor.isPlayer()){
            actor.reply("&cOnly players can send this command!");
            return;
        }

        SubCommandData scd = oscd.get();

        if(scd.isPlayerOnly()&&!actor.isPlayer()){
            actor.reply("&cOnly players can send this command!");
            return;
        }

        Stack<String> argStack = parseArgs(args);
        Stack<Class> classStack = new Stack<>();

        classStack.addAll(scd.getClassStack());

        List<Object> objects = new LinkedList<>();



        while(!classStack.isEmpty()){
            Class clazz = classStack.pop();
            objects.add(resolveValue(clazz, argStack, actor));
        }

        Collections.reverse(objects);

        scd.execute(actor,objects.toArray(new Object[0]));


    }

    private Optional<CommandData> findCommandData(String name){
        return commandDataList.stream().filter(cd -> cd.getName().equalsIgnoreCase(name)).findAny();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Optional<CommandData> ocd = findCommandData(command.getName());

        if(!ocd.isPresent()) return false;

        CommandData cd = ocd.get();

        try {
            executeCommand(sender,cd,args);
        } catch (RuntimeException e) {
            stackTraceSanitizer.sanitize(e);
            e.printStackTrace();
        }


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
