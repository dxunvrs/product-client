package commands;

import exceptions.InvalidArgumentException;
import exceptions.ScriptExecutionException;
import io.InputManager;

import java.io.IOException;

public class ExecuteScriptCommand extends Command {
    @Inject
    private InputManager inputManager;

    public ExecuteScriptCommand() {
        super("execute_script", "execute_script - выполнение скрипта из файла");
    }

    @Override
    public CommandData execute(String[] tokens) {
        if (tokens.length != 2) {
            throw new InvalidArgumentException("Ожидался 1 аргумент с именем файла, получено: " + (tokens.length-1));
        }
        try {
            inputManager.enqueueScript(tokens[1]); // добавление скрипта в очередь исполнения
            System.out.println("Начало выполнения скрипта: " + tokens[1]);
        } catch (IOException e) {
            throw new ScriptExecutionException("Ошибка чтения " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getSyntax() {
        return "execute_script [FILE_NAME]";
    }
}
