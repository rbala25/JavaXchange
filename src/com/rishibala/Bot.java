package com.rishibala;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Bot implements Runnable{
    private Socket socket;
    private int botId;
    private OrderBook orderBook;
    private User user;
    private PrintWriter out;
    private BufferedReader in;

    public Bot(Socket socket, int botId, OrderBook orderBook, User user) {
        this.socket = socket;
        this.botId = botId;
        this.orderBook = orderBook;
        this.user = user;

        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println(botId + " connected");


    }

    private void handleOrder(Order order) {
        orderBook.addOrder(order);
//        List<Set<Order>> matchedOrders = orderBook.matchOrders();
//
//        for(Set<Order> matches : matchedOrders) {
//            for(Order specificOrd : matches) {
//                if(specificOrd.botId() == botId) {
//                    notifyBot(specificOrd);
//                }
//            }
//        }
    }

    private void notifyBot(Order matchedOrder) {
        out.println("Found match for Order: " + matchedOrder.toString());
        orderBook.removeOrder(matchedOrder);
    }
}
