package core;

import exceptions.EndOfExecutionException;
import exceptions.EndOfInputException;
import exceptions.InvalidAuthorizeException;
import io.InputManager;
import network.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utility.AuthClient;

import java.net.SocketException;

public class ConsoleApp {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleApp.class);

    private CommandManager commandManager;
    private InputManager inputManager;
    private AuthClient authClient;

    private boolean isWorking = true;
    private boolean isAuthorized = false;

    private final String host;
    private final int port;

    public ConsoleApp(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        try (ConnectionManager connectionManager = new ConnectionManager(host, port)) {
            inputManager = new InputManager();
            authClient = new AuthClient(connectionManager, inputManager);
            commandManager = new CommandManager(connectionManager, inputManager);

            inputManager.initReaders(commandManager::getCommandNames);
            authClient.initAuth(commandManager::setToken);
            commandManager.syncCommands();

            runMainLoop();
        } catch (SocketException e) {
            logger.error("Ошибка сокета: {}", e.getMessage(), e);
            System.out.println("Ошибка сокета: " + e.getMessage());
        }
    }

    private void runMainLoop() {
        System.out.println("Добро пожаловать! Чтобы войти, введите login/register или exit для выхода");
        while (isWorking) {
            try {
                if (isAuthorized) {
                    interactive();
                } else {
                    isAuthorized = authClient.authorize();
                }
            } catch (InvalidAuthorizeException e) {
                logger.error("Ошибка авторизации", e);
                isAuthorized = false;
            } catch (EndOfExecutionException e) {
                logger.error("Завершение программы...", e);
                System.out.println(e.getMessage());
                isWorking = false;
            } catch (EndOfInputException e) {
                logger.error("Конец ввода", e);
                System.out.println(e.getMessage());
                isWorking = false;
            }
        }
    }

    private void interactive() {
        String line = inputManager.readCommand("> ");
        String formattedLine = line.trim().replaceAll("\\s+", " ");

        if (inputManager.isScriptMode()) System.out.println(formattedLine); // для режима скрипта

        if (formattedLine.isEmpty()) return;

        commandManager.executeCommand(formattedLine);
    }
}