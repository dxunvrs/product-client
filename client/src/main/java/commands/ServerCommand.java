package commands;

import exceptions.InvalidArgumentException;
import io.InputManager;
import utility.ProductForm;

import java.util.List;
import java.util.stream.Collectors;

public class ServerCommand extends Command {
    private final List<ArgType> expectedArgs;

    @Inject
    private InputManager inputManager;

    public ServerCommand(String name, String description, List<ArgType> expectedArgs) {
        super(name, description);
        this.expectedArgs = expectedArgs;
    }

    @Override
    public CommandData execute(String[] tokens) {
        CommandData commandData = new CommandData();
        if (tokens.length-1 != getExpectedArgsLength()) {
            throw new InvalidArgumentException("Передано неверное количество элементов, получено: " + (tokens.length-1) + ", ожидалось: " + getExpectedArgsLength());
        }
        int index = 1;
        try {
            for (ArgType argType: expectedArgs) {
                switch (argType) {
                    case STR -> commandData.addStringArg(tokens[index]);
                    case INT -> commandData.addIntArg(Integer.parseInt(tokens[index]));
                    case OBJECT ->  {
                        index--;
                        commandData.addObjectArg(new ProductForm(inputManager).getProduct());
                    }
                    case NONE -> index--;
                }
                index++;
            }
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException("Неверный формат числа у " + index + " аргумента");
        } catch(IndexOutOfBoundsException e) {
            throw new InvalidArgumentException("Передано неверное количество аргументов, ошибка на " + index + " аргументе");
        }
        return commandData;
    }

    private int getExpectedArgsLength() {
        return expectedArgs.stream().mapToInt(ArgType::getWeight).sum();
    }

    @Override
    public String getSyntax() {
        return getName() + " " + expectedArgs.stream().map(Enum::name).collect(Collectors.joining(", ", "[", "]"));
    }
}
