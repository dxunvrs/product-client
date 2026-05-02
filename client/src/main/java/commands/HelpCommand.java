package commands;

import core.CommandManager;
import exceptions.InvalidArgumentException;

public class HelpCommand extends Command {
    @Inject
    private CommandManager commandManager;

    public HelpCommand() {
        super("help", "help - список доступных команд");
    }

    @Override
    public CommandData execute(String[] tokens) {
        if (tokens.length-1 == 0) {
            System.out.println(commandManager.getFormattedCommandsList());
        } else if (tokens.length-1 == 1) {
            String syntax = commandManager.getFormattedCommandSyntax(tokens[1]);
            if (syntax == null) throw new InvalidArgumentException("Нет такой команды");
            System.out.println(syntax);
        } else {
            throw new InvalidArgumentException("Слишком много аргументов для команды help");
        }
        return null;
    }

    @Override
    public String getSyntax() {
        return "help [NONE] | help [COMMAND_NAME]";
    }
}
