package me.acablade.bladecommandframework.classes;

import java.util.Stack;

public interface ValueResolver<T>{

    T resolve(Stack<String> args);

}
