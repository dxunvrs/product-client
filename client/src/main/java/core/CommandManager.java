package core;

import commands.*;
import exceptions.InvalidArgumentException;
import exceptions.InvalidAuthorizeException;
import exceptions.ScriptExecutionException;
import io.InputManager;
import network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CommandManager {
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);

    private final Map<String, Command> commands = new HashMap<>();
    private final List<String> commandsHistory = new LinkedList<>();

    private final InputManager inputManager;
    private final ConnectionManager connectionManager;

    private String token;

    public CommandManager(ConnectionManager connectionManager, InputManager inputManager) {
        this.connectionManager = connectionManager;
        this.inputManager = inputManager;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void syncCommands() {
        commands.clear();
        addCommand(new HelpCommand());
        addCommand(new ExitCommand());
        addCommand(new HistoryCommand());
        addCommand(new ExecuteScriptCommand());

        logger.debug("Запущена синхронизация команд");
        Response response = connectionManager.sendAndReceive(new Request.Builder().type(RequestType.SYNC).build());
        if (response.getType() != ResponseType.SYNC_DATA) {
            System.out.println("Не удалось синхронизировать команды с сервером");
            return;
        }

        response.getSyncData().forEach((name, commandDef) ->
                addCommand(new ServerCommand(name, commandDef.description(), commandDef.expectedArgs())));
        logger.info("Команды синхронизированы");
    }

    public void executeCommand(String line) {
        String[] tokens = line.split(" ");
        Command command = commands.get(tokens[0]);
        if (command == null) { // если команда не найдена, то попытка синхронизации
            syncCommands();
            command = commands.get(tokens[0]);
            if (command == null) {
                logger.warn("Пользователь ввел некорректную команду {}", tokens[0]);
                System.out.println("Команда " + tokens[0] + " не найдена");
                return;
            }
        }
        try {
            logger.debug("Начало выполнения команды {}", command.getName());

            CommandData commandData = command.execute(tokens);

            if (commandData == null) return; // для клиентских команд

            Request request = new Request.Builder().type(RequestType.SERVER_COMMAND)
                            .commandName(command.getName())
                            .intArgs(commandData.getIntArgs())
                            .stringArgs(commandData.getStringArgs())
                            .objectArgs(commandData.getObjectArgs())
                            .token(token)
                            .build();
            Response response = connectionManager.sendAndReceive(request);
            System.out.println(response.getMessage());

            if (response.getType() == ResponseType.OUTDATED) { // если команда больше не поддерживается
                syncCommands();
                return;
            } else if (response.getType() == ResponseType.AUTH_REQUIRED) { // нужен вход
                throw new InvalidAuthorizeException(response.getMessage());
            }

            addCommandToHistory(command.getName());
        } catch (InvalidArgumentException e) {
            notifyError("Ошибка в аргументах: " + e.getMessage(), e);
        } catch (ScriptExecutionException e) {
            notifyError("Ошибка выполнения скрипта: " + e.getMessage(), e);
        }
    }

    private void notifyError(String message, Exception e) {
        logger.error(message, e);
        System.out.println(message);
    }

    private void addCommandToHistory(String commandName) {
        commandsHistory.add(commandName);
        if (commandsHistory.size() > 15) {
            commandsHistory.remove(0);
        }
        logger.info("Команда {} добавлена в историю", commandName);
    }

    public String getFormattedCommandSyntax(String commandName) {
        Command command = commands.get(commandName);
        if (command != null) return command.getSyntax();
        return null;
    }

    public String getFormattedCommandsList() {
        String result = commands.values().stream()
                .map(Command::getDescription).map(s -> "  " + s).collect(Collectors.joining("\n"));
        return "Список команд и их описание:" + "\n" + result;
    }

    public String getFormattedHistory() {
        AtomicInteger index = new AtomicInteger(1);
        String result = commandsHistory.stream().map(command -> "  " + index.getAndIncrement() + ". " + command)
                .collect(Collectors.joining("\n"));
        return "Последние 15 команд:" + "\n" + result;
    }

    public Set<String> getCommandNames() {
        return commands.keySet();
    }

    public void addCommand(Command command) {
        logger.debug("Регистрация новой команды: {}", command.getName());
        Field[] fields = command.getClass().getDeclaredFields();

        for (Field field: fields) {
            if (!field.isAnnotationPresent(Inject.class)) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object toInject = resolveDependency(field.getType());
                if (toInject == null) {
                    continue;
                }
                field.set(command, toInject);
                logger.debug("В команду {} внедрен {}", command.getName(), field.getType().getSimpleName());

            } catch (IllegalAccessException e) {
                logger.error("Не удалось внедрить зависимость в поле {}", field.getName(), e);
            }
        }
        commands.put(command.getName(), command);
        logger.info("Команда {} зарегистрирована", command.getName());
    }

    private Object resolveDependency(Class<?> type) {
        return switch (type.getSimpleName()) {
            case "CommandManager" -> this;
            case "InputManager" -> inputManager;
            default -> null;
        };
    }
}