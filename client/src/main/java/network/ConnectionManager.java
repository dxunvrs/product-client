package network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;

public class ConnectionManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final String host;
    private final int port;
    private final DatagramSocket socket;
    private final ObjectMapper mapper;

    private static final int MAX_ATTEMPTS = 3;
    private static final int TIMEOUT = 3000;
    private static final int PACKET_SIZE = 65535;

    public ConnectionManager(String host, int port) throws SocketException {
        this.host = host;
        this.port = port;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(TIMEOUT);
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public Response sendAndReceive(Request request) {
        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            try {
                byte[] sendData = mapper.writeValueAsBytes(request);
                InetAddress address = InetAddress.getByName(host);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);

                socket.send(sendPacket);
                logger.info("Запрос на сервер по адресу {}, порт: {} отправлен. Содержание: {}",
                        address, port, new String(sendData));

                byte[] receiveBuffer = new byte[PACKET_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                socket.receive(receivePacket);
                logger.info("Получен ответ от сервера по адресу {}, порт: {}. Содержание: {}",
                        address, port, new String(receivePacket.getData()));

                return mapper.readValue(receivePacket.getData(), Response.class);
            } catch (SocketTimeoutException e) {
                attempts++;
                logger.warn("Сервер не отвечает, попытка подключения: {} из {}", attempts, MAX_ATTEMPTS);
                System.out.println("Сервер не отвечает, попытка подключения: " + attempts + " из " + MAX_ATTEMPTS);
                if (attempts < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                }
            } catch (IOException e) {
                logger.error("Ошибка", e);
                System.out.println("Ошибка: " + e.getMessage());
                break;
            }
        }
        return new Response.Builder().type(ResponseType.ERROR).message("Запрос не доставлен").build();
    }

    @Override
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
