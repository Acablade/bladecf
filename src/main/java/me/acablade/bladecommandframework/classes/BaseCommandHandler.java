package me.acablade.bladecommandframework.classes;

import me.acablade.bladecommandframework.annotations.Argument;
import me.acablade.bladecommandframework.annotations.CommandInfo;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class BaseCommandHandler implements CommandExecutor, TabExecutor {

    private Map<Class, ValueResolver> valueResolverMap;
    private Map<Class, TabCompleter> tabCompleterMap;
    private List<CommandData> commandDataList;

    public BaseCommandHandler(){
        this.valueResolverMap = new HashMap<>();
        this.tabCompleterMap = new HashMap<>();
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
        registerValueResolver(Material.class, ctx -> Material.matchMaterial(ctx.pop().toUpperCase(Locale.ENGLISH)));
        registerValueResolver(GameMode.class, ctx -> GameMode.valueOf(ctx.pop().toUpperCase(Locale.ENGLISH)));

        registerTabCompleter(Material.class, () -> Arrays.stream(Material.values()).map(Material::name).collect(Collectors.toList()));
        registerTabCompleter(GameMode.class, () -> Arrays.stream(GameMode.values()).map(GameMode::name).collect(Collectors.toList()));
        registerTabCompleter(Player.class, () -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));

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
                case "nay":
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

    public void registerTabCompleter(Class clazz, TabCompleter tabCompleter){
        this.tabCompleterMap.put(clazz,tabCompleter);
    }

    public Stack<String> parseArgs(String command, boolean removeFirst){
        return parseArgs(command.split(" "),removeFirst);
    }

    public Stack<String> parseArgs(String[] args, boolean removeFirst){
        Stack<String> stringStack = new Stack<>();
        LinkedList<String> tmp = new LinkedList<>(Arrays.asList(args));
        if(removeFirst)tmp.remove(0);
        stringStack.addAll(tmp);
        return stringStack;
    }

    private StackTraceSanitizer stackTraceSanitizer = StackTraceSanitizer.defaultSanitizer();

    public <T> Optional<T> resolveValue(Class<? extends T> clazz, Stack<String> args, CommandActor actor){
        try{
            return (Optional<T>) Optional.ofNullable(valueResolverMap.get(clazz).resolve(new ValueResolverContext(args,actor)));
        }catch (Throwable t){
//            stackTraceSanitizer.sanitize(t);
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
            if(commandData.getBaseCommand()!=null) commandData.getBaseCommand().execute(actor);
            else if(commandData.getFallbackCommand()!=null) commandData.getFallbackCommand().execute(actor);
            return;
        }
        Optional<SubCommandData> oscd = commandData.getSubCommand(args[0]);
        if(!oscd.isPresent()){
            if(commandData.getBaseCommand()!=null) executeWithParams(actor,commandData.getBaseCommand(),args, false);
            else if(commandData.getFallbackCommand()!=null) commandData.getFallbackCommand().execute(actor);
            return;
        }


        if(commandData.isPlayerOnly()&&!actor.isPlayer()){
            actor.reply("&cOnly players can send this command!");
            return;
        }

        SubCommandData scd = oscd.get();

        if(scd.isPlayerOnly()&&!actor.isPlayer()){
            actor.reply("&cOnly players can send this command!");
            return;
        }

        executeWithParams(actor,scd,args, true);

    }

    private void executeWithParams(CommandActor actor,SubCommandData scd, String[] args, boolean removeFirst){
        Stack<String> argStack = parseArgs(args, removeFirst);
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
//            stackTraceSanitizer.sanitize(e);
            e.printStackTrace();
        }


        return false;
    }

    @Override
    public List onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        Optional<CommandData> ocd = findCommandData(command.getName());

        if(!ocd.isPresent()) return Collections.emptyList();

        CommandData cd = ocd.get();

        if(args.length==1){
            List<String> subCommandList = cd.getSubCommandDataList().stream().map(SubCommandData::getName).collect(Collectors.toList());
            if(cd.getBaseCommand()!=null){
                SubCommandData subCommandData = cd.getBaseCommand();
                Method method = subCommandData.getMethod();
                Class parameterClass = subCommandData.getClassStack().get(0);
                if(tabCompleterMap.containsKey(parameterClass)){
                    subCommandList.addAll(tabCompleterMap.get(parameterClass).complete());
                }
                String tabComplete = parameterClass.getSimpleName();
                Annotation[][] annotations = method.getParameterAnnotations();
                for (Annotation ann: annotations[args.length-1]){
                    if(!(ann instanceof Argument)) continue;
                    Argument argument = (Argument) ann;
                    tabComplete = argument.key();
                    break;
                }
                subCommandList.add(tabComplete);
            }
            return subCommandList;
        }
        else if(args.length>1){
            Optional<SubCommandData> oscd = cd.getSubCommand(args[0]);
            SubCommandData subCommandData = oscd.orElseGet(cd::getBaseCommand);
            Method method = subCommandData.getMethod();
            if(subCommandData.getClassStack().size()<args.length-1) return Collections.emptyList();
            int minus = subCommandData==cd.getBaseCommand() ? 1 : 2;
            Class parameterClass = subCommandData.getClassStack().get(args.length-minus);
            if(tabCompleterMap.containsKey(parameterClass)){
                return tabCompleterMap.get(parameterClass).complete();
            }
            String tabComplete = parameterClass.getSimpleName();
            Annotation[][] annotations = method.getParameterAnnotations();
            for (Annotation ann: annotations[args.length-1]){
                if(!(ann instanceof Argument)) continue;
                Argument argument = (Argument) ann;
                tabComplete = argument.key();
                break;
            }
            return Collections.singletonList(tabComplete);
        }

        return Collections.emptyList();
    }
}
