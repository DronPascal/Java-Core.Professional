package hw6.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MyServer {
    public static final int PORT = 8081;

    private List<ClientHandler> clients;
    private AuthService authService;
    private DBManager dbManager;
    public final static Logger LOG = LogManager.getLogger(MyServer.class);

    public DBManager getDbManager() {
        return dbManager;
    }

    public MyServer() {

        dbManager = new DBManager();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            authService = new BaseAuthService(dbManager);
            authService.start();
            clients = new ArrayList<>();
            while (true) {
                LOG.trace("Ожидаем поключение клиентов");
                Socket socket = serverSocket.accept();
                LOG.trace("Клиент подключился");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (authService != null) {
                authService.stop();
            }
        }
    }

    public synchronized void broadcastClientsList() {
        StringBuilder sb = new StringBuilder("/clients ");
        for (ClientHandler client : clients) {
            sb.append(client.getNick()).append(" ");
        }
        Message message = new Message();
        message.setMessage(sb.toString());
        broadcastMessage(message);
    }

    public synchronized void sendMsgToClient(ClientHandler from, String nickTo, String msg) {
        for (ClientHandler client : clients) {
            if (client.getNick().equals(nickTo)) {
                LOG.trace("Отправляем личное сообщение от " + from.getNick() + ", кому " + nickTo);
                Message message = new Message();
                message.setNick(from.getNick());
                message.setMessage(msg);
                client.sendMessage(message);
                return;
            }
        }
        LOG.trace("Клиент с ником " + nickTo + " не подюклчен к чату");
        Message message = new Message();
        message.setMessage("Клиент с этим ником не подключен к чату");
        from.sendMessage(message);
    }

    public synchronized void broadcastMessage(Message message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public synchronized boolean isNickBusy(String nick) {
        for (ClientHandler client : clients) {
            if (nick.equals(client.getNick())) {
                return true;
            }
        }
        return false;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientsList();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientsList();
    }
}
