package com.rishibala.server;

import java.io.*;
import java.net.Socket;
import java.util.*;

class BotHandler implements Runnable{
    private final Socket socket;
    private final int botId;
    private final OrderBook orderBook;
    private final User user;
    private BufferedWriter out;
    private BufferedReader in;
    private static final List<BotHandler> bots = new ArrayList<>();
    private boolean over = false;

    BotHandler(Socket socket, int botId, OrderBook orderBook, User user) {
        this.socket = socket;
        this.botId = botId;
        this.orderBook = orderBook;
        this.user = user;

        try {
            out = new BufferedWriter(new PrintWriter(socket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            in = new Scanner(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

//        int counter = 1;

        try {
            bots.add(this);
            System.out.println("Bot " + botId + " connected");

           out.write(botId + "," + user.toString());
           out.newLine();
           out.flush();

            String recievedMessage;
            while ((recievedMessage = in.readLine()) != null) {
//                if(botId != 1) {
//                    System.out.println(recievedMessage + " " + counter);
//                    counter++;
//                }

                if(recievedMessage.contains("bookReq")) {
                    List<Order> orders = OrderBook.getListedOrders(botId, orderBook);
                    for(Order thisOrder : orders) {
                        orderBook.removeOrder(thisOrder.orderId());
                    }

                    out.write(orderBook.serialize().toString());
                    out.newLine();
                    out.flush();
                    continue;
                }

                if(recievedMessage.contains("EWMAReReq")) {
                   out.write(user.getStockAmt() + ":" + user.getProfit());
                   out.newLine();
                   out.flush();
                   continue;
                }

                if (recievedMessage.toLowerCase().contains("cancel")) {
                    String[] args = recievedMessage.split(",");

                    int orderId = Integer.parseInt(args[args.length - 1]);
                    orderBook.removeOrder(orderId);

                    System.out.println("Cancelled Order " + orderId);
                } else if (recievedMessage.toLowerCase().contains("listedmatches")) {
                    StringBuilder builder = OrderBook.getListedMatches(botId, orderBook, false);
                   out.write(String.valueOf(builder));
                    out.newLine();
                    out.flush();
                } else if (recievedMessage.toLowerCase().contains("haveorder")) {
                    String orderNo = recievedMessage.split("-")[1];
                    boolean haveOrder = OrderBook.haveOrder(botId, orderBook, Integer.parseInt(orderNo));
                   out.write(String.valueOf(haveOrder));
                    out.newLine();
                    out.flush();
                } else if (recievedMessage.contains("userRequest")) {
                   out.write(String.valueOf(user));
                    out.newLine();
                    out.flush();
                   out.write("Total Balance: " + user.getProfit());
                    out.newLine();
                    out.flush();
                } else if (recievedMessage.contains("close")) {
                   out.write("close");
                    out.newLine();
                    out.flush();
                } else if(recievedMessage.contains("quitting")) {
                    System.out.println("Bot " + botId + " disconnected.");
                } else if(recievedMessage.equals("checkerTrueListed")) {
                    StringBuilder builder = OrderBook.getListedMatches(0, orderBook, true);
                    System.out.println(builder.toString().split("\n").length);
                } else if(recievedMessage.contains("MMBOT_OVER")) {
                    String[] args = recievedMessage.split(":");

                    for(BotHandler bot : bots) {
//                        if(bot.botId != 1) {
                        try {

                            bot.out.write("SIGNALOVER:" + args[1] + ":" + args[2] + ":" + bot.user.getStockAmt() + ":" + bot.user.getProfit());
                            bot.out.newLine();
                            bot.out.flush();
                            over = true;

                        } catch(IOException e) {
                            System.out.println("FAIL: ID: " + bot.botId);
                            e.printStackTrace();
                        }
//                        }
                    }

                    if(over) {
                        break;
                    }
                } else {
                    if(over) {
                        break;
                    }

                    Order order = Order.toOrder(recievedMessage);
                    handleOrder(order);

                }
            }
        } catch (IOException e) {
           e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void handleOrder(Order order) {
        if(order.botId() == 1) {
            if(order.type().equals(Order.Type.BUY)) {
                List<Order> orders = OrderBook.getListedOrders(1, orderBook);
                for(Order thisOrder : orders) {
                    orderBook.removeOrder(thisOrder.orderId());
                }

            }

            orderBook.addOrder(order);
        } else {
            orderBook.addOrder(order);
        }

        for(BotHandler bot : bots) {
            bot.checkMatches();
        }
    }

    private void checkMatches() {
//        System.out.println(botId + " bot id");
        List<Set<Order>> matchedOrders = orderBook.matchOrders();

        for(Set<Order> matches : matchedOrders) {
            boolean first = true;
            for(Order specificOrd : matches) {
//                System.out.println(specificOrd.botId() + " vs " + botId);
                if((specificOrd.botId() == botId) && first) {
                    notifyBot(matches);
                    first = false;
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

        if(p1.botId() == this.botId) {
            bot1 = p1;
            bot2 = p2;
        } else {
            bot1 = p2;
            bot2 = p1;
        }

        if(botId == 1) {
            if(bot1.type().equals(Order.Type.BUY)) {
                user.updateStockAmt(1);
                user.updateProfit(bot1.price() * -1);
            } else {
                user.updateStockAmt(-1);
                user.updateProfit(bot2.price());
            }

            return;
        }

        if(bot1.type().equals(Order.Type.BUY)) {
            user.updateStockAmt(bot1.quantity());
            user.updateProfit(bot1.price() * -1);

            if(bot2.quantity() > bot1.quantity()) {
                orderBook.removeOrder(bot1.orderId());
                orderBook.removeOrder(bot2.orderId());
                int qty = bot2.quantity() - bot1.quantity();

                orderBook.addOrder(new Order(bot2.botId(), Order.Type.SELL, (bot2.pricePerQuantity() * qty), qty, bot2.orderId()));
            } else {
                orderBook.removeOrder(bot1.orderId());
                orderBook.removeOrder(bot2.orderId());
            }
        } else {
            user.updateStockAmt(bot2.quantity() * -1); //can have negative shares for short selling
            user.updateProfit(bot2.price());

            if(bot1.quantity() > bot2.quantity()) {
                orderBook.removeOrder(bot1.orderId());
                orderBook.removeOrder(bot2.orderId());
                int qty = bot1.quantity() - bot2.quantity();

                orderBook.addOrder(new Order(bot1.botId(), Order.Type.SELL, (bot1.pricePerQuantity() * qty), qty, bot1.orderId()));
            } else {
                orderBook.removeOrder(bot1.orderId());
                orderBook.removeOrder(bot2.orderId());
            }
        }

        String str;
        String alt;
        if(bot1.type().equals(Order.Type.SELL)) {
            str = String.format("Selling %d shares to Client #%d for $%.2f", bot2.quantity(), bot2.botId(), bot2.price());
            alt = String.format("Buying %d shares from Client #%d for $%.2f", bot2.quantity(), bot1.botId(), bot2.price());
        } else {
            str = String.format("Buying %d shares from Client #%d for $%.2f", bot1.quantity(), bot2.botId(), bot1.price());
            alt = String.format("Selling %d shares to Client #%d for $%.2f", bot1.quantity(), bot1.botId(), bot1.price());
        }

     try {
         out.write(str);
         out.newLine();
         out.flush();
     } catch (IOException e) {
         e.printStackTrace();
     }

        if(bot2.botId() != 1) {
            getBot(bot2.botId()).notifyBot2(alt, bot2, bot1);
        }

        System.out.println("Successfully handled matching orders");
    }

    private void notifyBot2(String str, Order order, Order alt) {
        if(order.type().equals(Order.Type.BUY)) {
            user.updateStockAmt(order.quantity());
            user.updateProfit(order.price() * -1);
        } else {
            user.updateStockAmt(alt.quantity() * -1); //can have negative shares for short selling
            user.updateProfit(alt.price());
        }

        try {
            out.write(str);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BotHandler getBot(int botId) {
        for(BotHandler bot : bots) {
            if(bot.botId == botId) {
                return bot;
            }
        }
        return null;
    }

}