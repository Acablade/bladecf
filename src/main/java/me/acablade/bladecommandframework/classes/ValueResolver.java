package me.acablade.bladecommandframework.classes;

import org.bukkit.command.CommandSender;

import java.util.Stack;

public interface ValueResolver<T>{

    T resolve(ValueResolverContext context);

    interface ValueResolverContext {

        Stack<String> arguments();

        String pop();

        int popInt();

        double popDouble();

        byte popByte();

        short popShort();

        float popFloat();

        long popLong();

        CommandActor actor();

    }

}
