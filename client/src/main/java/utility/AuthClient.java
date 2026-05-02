package utility;

import exceptions.EndOfExecutionException;
import io.InputManager;
import network.*;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class AuthClient {
    private final ConnectionManager connectionManager;
    private final InputManager inputManager;

    private Consumer<String> tokenConsumer;

    public AuthClient(ConnectionManager connectionManager, InputManager inputManager) {
        this.connectionManager = connectionManager;
        this.inputManager = inputManager;
    }

    public void initAuth(Consumer<String> tokenConsumer) {
        this.tokenConsumer = tokenConsumer;
    }

    public boolean authorize() {
        String line = inputManager.readData("> ", Set.of("login", "register", "exit"));
        String formattedLine = line.trim().replaceAll("\\s+", " ");

        switch (formattedLine) {
            case "login" -> {
                return processLogin();
            }
            case "register" -> {
                return processRegister();
            }
            case "exit" -> throw new EndOfExecutionException("Выход из программы");
            default -> {
                System.out.println("Не поддерживается");
                return false;
            }
        }
    }

    private boolean processLogin() {
        String username = inputManager.readData("Введите имя пользователя: ", null).trim();
        String password = inputManager.readPassword("Введите пароль: ").trim();

        Request request = new Request.Builder().type(RequestType.LOGIN).username(username).password(password).build();

        Response response = connectionManager.sendAndReceive(request);
        System.out.println(response.getMessage());

        if (response.getType() == ResponseType.AUTH_SUCCESS) {
            tokenConsumer.accept(response.getToken());
            return true;
        }
        return false;
    }

    private boolean processRegister() {
        String username = inputManager.readData("Введите имя пользователя: ", null).trim();
        String password = inputManager.readPassword("Введите пароль: ").trim();
        String repeatPassword = inputManager.readPassword("Повторите введенный пароль: ").trim();

        if (!Objects.equals(password, repeatPassword)) {
            System.out.println("Пароли не совпадают");
            return false;
        }

        Request request = new Request.Builder().type(RequestType.REGISTER).username(username).password(password).build();

        Response response = connectionManager.sendAndReceive(request);
        System.out.println(response.getMessage());

        if (response.getType() == ResponseType.AUTH_SUCCESS) {
            tokenConsumer.accept(response.getToken());
            return true;
        }
        return false;
    }
}
