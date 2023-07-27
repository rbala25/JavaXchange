package com.rishibala;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class StockExchange {
    private static final Map<Integer, Socket> bots = new HashMap<>();
    private static final Map<Socket, Integer> ids = new HashMap<>();
    private static final Map<Socket, User> users = new HashMap<>();
//    private static final List<Thread> threads = new ArrayList<>();
    private static final OrderBook book = new OrderBook();
    private static boolean first = true;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(3000);
            System.out.println("server started.");

            int botId = 1;

            while (true) {
                Socket botSocket = serverSocket.accept();
                System.out.println("New connection: " + botSocket.getInetAddress().getHostAddress());

                bots.put(botId++, botSocket);
                ids.put(botSocket, botId);

                User user;
                if(first) {
                    user = new User(botId, 50);
                } else {
                    user = new User(botId, 0);
                }

                Thread botThread = new Thread(new Bot(botSocket, botId, book, user));
//                threads.add(botThread);
                botThread.start();
            }

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    static int getId(Socket socket) {
        return ids.get(socket);
    }

    static User getUser(Socket socket) {
        return users.get(socket);
    }

}
