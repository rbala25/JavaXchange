package com.rishibala.bots;

import com.rishibala.server.Order;
import com.rishibala.server.OrderBook;
import com.rishibala.server.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EWMABot {

    private static List<Double> buyData = new ArrayList<>();
    private static List<Double> sellData = new ArrayList<>();
    public static void main(String[] args) {
        int botId = 0;
        User user = new User();

        try {
            Socket socket = new Socket("localhost", 3000); //change localhost if on different ip
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            try {
                String serverMessage = in.readLine();
                if(serverMessage != null) {
                    String[] arg = serverMessage.split(",");
                    botId = Integer.parseInt(arg[0]);
                    user = User.unString(arg[1]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            while(true) {
                out.flush();
                while(in.ready()) {
                    String str = in.readLine();
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                out.println("bookReq");
                OrderBook book = new OrderBook();
                try {
                    String serverMessage = in.readLine();
                    while (serverMessage != null) {
                        if(serverMessage.contains("~")) {
                            try {
                                book = OrderBook.unserialize(serverMessage);
                            } catch(ArrayIndexOutOfBoundsException e) {
                                System.out.println("error");
                            }
                            break;
                        }
                        serverMessage = in.readLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                out.flush();

                for(List<Order> buys : book.getBuyOrders().values()) {
                    for(Order order : buys) {
                        buyData.add(order.price());
                    }
                }
                for(List<Order> sells : book.getSellOrders().values()) {
                    for(Order order : sells) {
                        sellData.add(order.price());
                    }
                }

                double ewmaValue = calculateEWMA(buyData);
                double currentBuyPrice = Double.MIN_VALUE;
                double currentSellPrice = Double.MAX_VALUE;
                int currentBuyQty = 0;
                int currentSellQty = 0;

                for(List<Order> buys : book.getBuyOrders().values()) {
                    for(Order order : buys) {
                        currentBuyPrice = order.price();
                        currentBuyQty = order.quantity();
                    }
                }
                for(List<Order> sells : book.getSellOrders().values()) {
                    for(Order order : sells) {
                        currentSellPrice = order.price();
                        currentSellQty = order.quantity();
                    }
                }

                if (currentSellPrice < ewmaValue) {
                    out.println(botId + ", BUY" + ", " + currentBuyPrice + ", " + currentBuyQty);
                    System.out.println("NEW ORDER: " + botId + ", BUY" + ", " + currentBuyPrice + ", " + currentBuyQty);
                } else if (currentBuyPrice > ewmaValue && user.getStockAmt() > 0) {
                    if(user.getStockAmt() >= currentSellQty) {
                        out.println(botId + ", SELL" + ", " + currentSellPrice + ", " + currentSellQty);
                        System.out.println("NEW ORDER: " + botId + ", SELL" + ", " + currentSellPrice + ", " + currentSellQty);
                    } else {
                        out.println(botId + ", SELL" + ", " + currentSellPrice + ", " + user.getStockAmt());
                        System.out.println("NEW ORDER: " + botId + ", SELL" + ", " + currentSellPrice + ", " + user.getStockAmt());
                    }
                }

                System.out.println("Shares: " + user.getStockAmt() + " Current buy: " + currentBuyPrice + " current sell: " + currentSellPrice);
                System.out.println("PNL: " + user.getProfit());
                System.out.println("EWMA: " + ewmaValue);
                System.out.println();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    private static double calculateEWMA(List<Double> data) {
        double alpha = 0.2;
        if(data.size() > 0) {
            double ewma = data.get(0);
            for (int i = 1; i < data.size(); i++) {
                double currentPrice = data.get(i);
                ewma = alpha * currentPrice + (1 - alpha) * ewma;
            }
            return ewma;
        }
        return 0;
    }

}
