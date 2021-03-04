package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    public Server() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        int PORT = 8888;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started");
            while (true) {
                executorService.execute(new ClientHandler(serverSocket.accept()));
                System.out.println("Connection accepted");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static void main(String[] args) {
        new Server();
    }

}
