package gui.service;

import models.Product;
import network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ClientService implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ClientService.class);

    private final ConnectionManager connection;

    private volatile String token;
    private volatile String currentUsername;

    public ClientService(ConnectionManager connection) {
        this.connection = connection;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

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
            this.currentUsername = username;
        }
        return resp;
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
