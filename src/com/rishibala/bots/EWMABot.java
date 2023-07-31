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

    private static Order lastBuy;
    private static Order lastSell;
    private static boolean firstCheck = true;

    public static void main(String[] args) {
        int counter = 1;
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

                out.println("EWMAReReq");
                try {
                    String serverMessage = in.readLine();
                    while(serverMessage != null) {
                        if(serverMessage.contains(":")) {
                            user = User.unString(serverMessage);
                            break;
                        }
                        serverMessage = in.readLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                out.flush();

                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while(in.ready()) {
                    String str = in.readLine();
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

                List<Double> means = new ArrayList<>();
                for(int i=0; (i<buyData.size() && i<sellData.size()); i++) {
                    double buyPrice = buyData.get(i);
                    double sellPrice = sellData.get(i);

                    means.add((buyPrice + sellPrice) / 2);
                }


                double ewmaValue = calculateEWMA(means);
                boolean buyInit = false;
                boolean sellInit = false;

                double currentBuyPrice;
                double currentSellPrice;

                if(firstCheck) {
                    currentBuyPrice = Double.MIN_VALUE;
                    currentSellPrice = Double.MAX_VALUE;
                    firstCheck = false;
                } else {
                    currentBuyPrice = lastBuy.price();
                    currentSellPrice = lastSell.price();
                }

                int currentBuyQty = 0;
                int currentSellQty = 0;

                for(List<Order> buys : book.getBuyOrders().values()) {
                    for(Order order : buys) {
                        currentBuyPrice = order.price();
                        currentBuyQty = order.quantity();
                        lastBuy = order;
                        buyInit = true;
                    }
                }
                for(List<Order> sells : book.getSellOrders().values()) {
                    for(Order order : sells) {
                        currentSellPrice = order.price();
                        currentSellQty = order.quantity();
                        lastSell = order;
                        sellInit = true;
                    }
                }

                if (currentSellPrice < ewmaValue) {
                    if(!buyInit && !sellInit) {
                        out.println(botId + ", BUY" + ", " + currentSellPrice + ", " + currentSellQty);
                        System.out.println("NEW ORDER: " + botId + ", BUY" + ", " + currentSellPrice + ", " + currentBuyQty);
                    }
                } else if (currentBuyPrice > ewmaValue && user.getStockAmt() > 0) {
                    if(!buyInit && !sellInit) {
                        if(user.getStockAmt() >= currentBuyPrice) {
                            out.println(botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
                            System.out.println("NEW ORDER: " + botId + ", SELL" + ", " + currentBuyPrice + ", " + currentSellQty);
                        } else {
                            out.println(botId + ", SELL" + ", " + currentBuyPrice + ", " + user.getStockAmt());
                            System.out.println("NEW ORDER: " + botId + ", SELL" + ", " + currentBuyPrice + ", " + user.getStockAmt());
                        }
                    }
                }

                System.out.println("Shares: " + user.getStockAmt() + " Current buy: " + currentBuyPrice + " current sell: " + currentSellPrice);
                System.out.println("PNL: " + user.getProfit());
                System.out.println("EWMA: " + ewmaValue + " " + counter);
                System.out.println();
                counter++;
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    private static double calculateEWMA(List<Double> data) {
        double alpha = 0.4;
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