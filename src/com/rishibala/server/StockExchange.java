package com.rishibala.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class StockExchange {
    private static final Map<Integer, Socket> bots = new HashMap<>();
    private static final Map<String, Integer> ids = new HashMap<>();
    private static final Map<String, User> users = new HashMap<>();
//    private static final List<Thread> threads = new ArrayList<>();
    private static final OrderBook book = new OrderBook();
//    private static final List<Socket> sockets = new ArrayList<>();
    private static boolean first = true;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(3000);
            System.out.println("server started.");

            int botId = 0;

            while (true) {
                Socket botSocket = serverSocket.accept();
                System.out.println("New connection: " + botSocket.getInetAddress().getHostAddress());

//                sockets.add(botSocket);
                bots.put(botId++, botSocket);

                String ide = getIdentifier(botSocket);
                ids.put(ide, botId);

                User user;
                if(first) {
                    user = new User(botId, 100);
                } else {
                    user = new User(botId, 0);
                }

                users.put(ide, user);
                Thread botThread = new Thread(new Bot(botSocket, botId, book, user));
//                threads.add(botThread);
                botThread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getId(Socket socket) {
        synchronized (ids) {
            System.out.println(ids);
            String sock = getIdentifier(socket);

//        System.out.println(sock);
//        for(String str : ids.keySet()) {
//            System.out.println(str);
//        }

            return ids.get(sock);
        }
    }

    public static User getUser(Socket socket) {
        synchronized (users) {
            System.out.println(users);
            String sock = getIdentifier(socket);

            return users.get(sock);
        }
    }

    public static OrderBook getBook() {
        return book;
    }

//    public static boolean compareSockets(Socket socket1, Socket socket2) {
//        if (socket1.getInetAddress().equals(socket2.getInetAddress()) &&
//                socket1.getPort() == socket2.getPort()) {
//            return true;
//        }
//        return false;
//    }

    private static String getIdentifier(Socket socket) {
        String localIp = "localhost"; // Use the server's IP (you can change this if needed)
        int localPort = 3000;
        String remoteIp = socket.getInetAddress().getHostAddress();
        int remotePort = socket.getPort();

        // unique identifier
        return remoteIp + ":" + remotePort + "-" + localIp + ":" + localPort;
    }

}
