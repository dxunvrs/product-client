package gui.service;

import models.Product;
import network.ConnectionManager;
import network.Request;
import network.RequestType;
import network.Response;
import network.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.util.List;

/**
 * Тонкая обёртка над {@link ConnectionManager}, синхронизированная для использования
 * из GUI-потока и фонового поллера. Хранит токен и id текущего пользователя.
 */
public class ClientService implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ClientService.class);

    private final String host;
    private final int port;
    private final ConnectionManager connection;

    private volatile String token;
    private volatile Integer currentUserId;
    private volatile String currentUsername;

    public ClientService(String host, int port) throws SocketException {
        this.host = host;
        this.port = port;
        this.connection = new ConnectionManager(host, port);
    }

    public String getHost() { return host; }
    public int getPort() { return port; }

    public String getToken() { return token; }
    public Integer getCurrentUserId() { return currentUserId; }
    public String getCurrentUsername() { return currentUsername; }

    public synchronized Response send(Request request) {
        return connection.sendAndReceive(request);
    }

    public Response login(String username, String password) {
        Request req = new Request.Builder()
                .type(RequestType.LOGIN)
                .username(username).password(password).build();
        Response resp = send(req);
        if (resp.getType() == ResponseType.AUTH_SUCCESS) {
            this.token = resp.getToken();
            this.currentUserId = resp.getUserId();
            this.currentUsername = username;
        }
        return resp;
    }

    public Response register(String username, String password) {
        Request req = new Request.Builder()
                .type(RequestType.REGISTER)
                .username(username).password(password).build();
        Response resp = send(req);
        if (resp.getType() == ResponseType.AUTH_SUCCESS) {
            this.token = resp.getToken();
            this.currentUserId = resp.getUserId();
            this.currentUsername = username;
        }
        return resp;
    }

    public void logout() {
        this.token = null;
        this.currentUserId = null;
        this.currentUsername = null;
    }

    public Response sync() {
        return send(new Request.Builder().type(RequestType.SYNC).build());
    }

    public Response sendCommand(String name, List<String> stringArgs, List<Integer> intArgs, List<Product> objects) {
        Request req = new Request.Builder()
                .type(RequestType.SERVER_COMMAND)
                .commandName(name)
                .stringArgs(stringArgs)
                .intArgs(intArgs)
                .objectArgs(objects)
                .token(token)
                .build();
        return send(req);
    }

    @Override
    public void close() {
        connection.close();
    }
}
