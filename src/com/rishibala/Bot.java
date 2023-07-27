package com.rishibala;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Bot implements Runnable{
    private Socket socket;
    private int botId;
    private OrderBook orderBook;
    private User user;
    private PrintWriter out;
    private BufferedReader in;
    private static List<Bot> bots = new ArrayList<>();

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

    public Bot() {
    }

    @Override
    public void run() {
        bots.add(this);
        System.out.println(botId + " connected");

        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            while (true) {
                Object receivedMessage = in.readObject();

                if (receivedMessage instanceof Order) {
                    Order order = (Order) receivedMessage;
                    handleOrder(order);
                } else {
                    //cancel order requests
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println(botId + " disconnected.");
        }

    }

    private void handleOrder(Order order) {

        if(order.type().equals(Order.Type.SELL) && user.getStockAmt() >= order.quantity()) {
            orderBook.addOrder(order);
        } else if (order.type().equals(Order.Type.BUY)) {
            orderBook.addOrder(order);
        } else {
            if(user.getStockAmt() < order.quantity()) {
                orderBook.addOrder(new Order(order.botId(), order.type(),
                        order.price(), user.getStockAmt(), order.orderId()));
            }
        }

        for(Bot bot : bots) {
            bot.checkMatches();
        }
    }

    private void checkMatches() {
        List<Set<Order>> matchedOrders = orderBook.matchOrders();

        for(Set<Order> matches : matchedOrders) {
            for(Order specificOrd : matches) {
                if(specificOrd.botId() == botId) {
                    notifyBot(specificOrd);
                }
            }
        }
    }

    private void notifyBot(Order matchedOrder) {
        out.println("Found match for Order: " + matchedOrder.toString());
        orderBook.removeOrder(matchedOrder);
    }
}
