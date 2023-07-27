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
                    notifyBot(matches);
                }
            }
        }
    }

    private void notifyBot(Set<Order> match) {
        Object[] match1 = match.toArray();
        Order p1 = (Order) match1[0];
        Order p2 = (Order) match1[1];

        Order bot1;
        Order bot2;
        Order buy;
        Order sell;

        if(p1.botId() == this.botId) {
            bot1 = p1;
            bot2 = p2;
        } else {
            bot1 = p2;
            bot2 = p1;
        }

        if(p1.type().equals(Order.Type.BUY)) {
            buy = p1;
            sell = p2;
        } else {
            buy = p2;
            sell = p1;
        }

        out.println("Found match for Order: " + bot1.toString());
        out.println("Trading with Order: " + bot2.toString());

        if(bot1.equals(buy)) {
            user.updateStockAmt(sell.quantity());
        } else {
            user.updateStockAmt(sell.quantity() * -1);
        }

        orderBook.removeOrder(this.botId);
    }
}
