package commands;

import core.CommandManager;
import exceptions.InvalidArgumentException;

public class HistoryCommand extends Command {
    @Inject
    private CommandManager commandManager;

    public HistoryCommand() {
        super("history", "history - последние 15 команд");
    }

    @Override
    public CommandData execute(String[] tokens) {
        if (tokens.length != 1) {
            throw new InvalidArgumentException("Слишком много аргументов для команды history");
        }
        System.out.println(commandManager.getFormattedHistory());
        return null;
    }

    @Override
    public String getSyntax() {
        return "history [NONE]";
    }
}
