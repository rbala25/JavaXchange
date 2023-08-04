package com.rishibala.tempBots;

import com.rishibala.server.Order;
import com.rishibala.server.OrderBook;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EWMABot extends Bot{
    //Estimated weighted moving average calculations

//    private static List<Double> buyData = new ArrayList<>();
//    private static List<Double> sellData = new ArrayList<>();
//    private static boolean afterOrder = false;

    public static void main(String[] args) {
        int counter = 1;

        try {
            bot = new EWMABot();
            bot.socket = new Socket("localhost", 5000); //change localhost if on different ip
            bot.in = new BufferedReader(new InputStreamReader(bot.socket.getInputStream()));
//            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            bot.out = new BufferedWriter(new PrintWriter(bot.socket.getOutputStream(), true));

            bot.initalCheck();

            while(true) {
                try {
//                    Thread.sleep(25);
                    TimeUnit.MILLISECONDS.sleep(70);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                bot.checkEnd();
                if(bot.over) {
                    break;
                }

                bot.getUser();

                OrderBook book = bot.getBook();

//                for(List<Order> buys : book.getBuyOrders().values()) {
//                    for(Order order : buys) {
//                        buyData.add(order.price());
//                    }
//                }
//                for(List<Order> sells : book.getSellOrders().values()) {
//                    for(Order order : sells) {
//                        sellData.add(order.price());
//                    }
//                }

                boolean buyInit = false;
                boolean sellInit = false;

                double currentBuyPrice;
                double currentSellPrice;

                if(bot.firstCheck) {
                    currentBuyPrice = Double.MIN_VALUE;
                    currentSellPrice = Double.MAX_VALUE;
                    bot.firstCheck = false;
                } else {
                    currentBuyPrice = bot.lastBuy.price();
                    currentSellPrice = bot.lastSell.price();
                }

                int currentBuyQty = 0;
                int currentSellQty = 0;

                for(List<Order> buys : book.getBuyOrders().values()) {
                    Order order = buys.get(0);
                    currentBuyPrice = order.price();
                    currentBuyQty = order.quantity();
                    bot.lastBuy = order;
                    buyInit = true;
                }
                for(List<Order> sells : book.getSellOrders().values()) {
                    Order order = sells.get(0);
                    currentSellPrice = order.price();
                    currentSellQty = order.quantity();
                    bot.lastSell = order;
                    sellInit = true;
                }

//
//                if(!afterOrder) {
//                    for(List<Order> buys : book.getBuyOrders().values()) {
//                        for(Order order : buys) {
//                            currentBuyPrice = order.price();
//                            currentBuyQty = order.quantity();
//                            lastBuy = order;
//                            buyInit = true;
//                        }
//                    }
//                    for(List<Order> sells : book.getSellOrders().values()) {
//                        for(Order order : sells) {
//                            if(order.price() < 100) {
//                                System.out.println("PROBLEM: " + order);
//                            }
//
//                            currentSellPrice = order.price();
//                            currentSellQty = order.quantity();
//                            lastSell = order;
//                            sellInit = true;
//                        }
//                    }
//                }

                if((currentBuyPrice != Double.MIN_VALUE) && (currentSellPrice != Double.MAX_VALUE)) {
                    bot.means.add((currentSellPrice + currentBuyPrice) / 2);
                }

                if(bot.means.size() > 7001) { //period of 7000
                    bot.means.remove(0);
                }

                double ewmaValue = bot.calculate()[0];

                double temp = currentBuyPrice;
//                double temp1 = currentSellPrice;

                if(bot.shares > 50) {  //more incentive to sell
                    temp += 0.06;
                }
//                if(counter < 500) {
//                    temp1 += 0.08;
//                }

                double temp1 = currentSellPrice - 0.03;
                temp -= 0.08;

                if ((temp1 < ewmaValue) && (counter > 100)) { //only can trade after the first 100 orders (not enough data points)
                    if(buyInit && sellInit) {
//                        out.println(botId + ", BUY" + ", " + currentSellPrice + ", " + currentSellQty);
                        bot.writeBuy(currentSellPrice, currentSellQty);
                    }
                } else if ((temp > ewmaValue) && (counter > 100)) { //allows short selling
                    if(buyInit && sellInit) {
//                      out.println(botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
                        bot.writeSell(currentBuyPrice, currentBuyQty);
                    }
                }

                System.out.println("Shares: " + bot.shares + " Current buy: $" + currentBuyPrice + " current sell: $" + currentSellPrice);
                System.out.printf("PNL: $%.2f", bot.pnl);
                System.out.println("\nEWMA: " + String.format("$%.2f", ewmaValue) + " " + counter);
//                System.out.println("Potential current PNL: " + );
                System.out.println();
                counter++;

            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            bot.close();
        }

    }

    @Override
    protected double[] calculate() {
        double alpha = 0.2;
        if(means.size() > 0) {
            double ewma = means.get(0);
            for (int i = 1; i < means.size(); i++) {
                double currentPrice = means.get(i);
                ewma = alpha * currentPrice + (1 - alpha) * ewma;
            }
            return new double[]{ewma};
        }
        return new double[]{0};
    }
}