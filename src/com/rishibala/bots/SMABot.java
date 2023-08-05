package com.rishibala.bots;

import com.rishibala.server.Order;
import com.rishibala.server.OrderBook;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SMABot extends Bot {
    //Simple moving average calculations

    private static final List<Double> periodBuy = new ArrayList<>();
    private static final List<Double> periodSell = new ArrayList<>();

    public static void main(String[] args) {

        Thread bot = new Thread(new SMABot());
        bot.start();

    }

    @Override
    protected double[] calculate() {
        double sum = 0;
        for(double data : means) {
            sum += data;
        }

        return new double[]{(sum /(means.size()))};
    }

    @Override
    public void run() {
        trade();
    }

    @Override
    protected void trade() {
        int counter = 1;

        try {
            bot = new SMABot();
            bot.socket = new Socket("localhost", 5000); //change localhost if on different ip
            bot.in = new BufferedReader(new InputStreamReader(bot.socket.getInputStream()));
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

                for(List<Order> buys : book.getBuyOrders().values()) {
                    for(Order order : buys) {
                        if(periodBuy.size() > 2000) {
                            periodBuy.add(order.price());
                            periodBuy.remove(0);
                        } else {
                            periodBuy.add(order.price());
                        }
                    }
                }
                for(List<Order> sells : book.getSellOrders().values()) {
                    for(Order order : sells) {
                        if(periodSell.size() > 2000) {
                            periodSell.add(order.price());
                            periodSell.remove(0);
                        } else {
                            periodSell.add(order.price());
                        }
                    }
                }

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

                if(!bot.afterOrder) {
                    for(List<Order> buys : book.getBuyOrders().values()) {
                        for(Order order : buys) {
                            currentBuyPrice = order.price();
                            currentBuyQty = order.quantity();
                            bot.lastBuy = order;
                            buyInit = true;
                        }
                    }
                    for(List<Order> sells : book.getSellOrders().values()) {
                        for(Order order : sells) {
                            if(order.price() < 100) {
                                System.out.println("PROBLEM: " + order);
                            }

                            currentSellPrice = order.price();
                            currentSellQty = order.quantity();
                            bot.lastSell = order;
                            sellInit = true;
                        }
                    }
                }

                if((currentBuyPrice != Double.MIN_VALUE) && (currentSellPrice != Double.MAX_VALUE)) {
                    bot.means.add((currentSellPrice + currentBuyPrice) / 2);
                }

                if(bot.means.size() > 101) {  //period of 100 at max
                    bot.means.remove(0);
                }

                double smaValue = bot.calculate()[0];
                double temp1 = currentSellPrice + 0.2;
                double temp = currentBuyPrice - 0.65;


                if ((temp1 < smaValue) && (counter > 100)) { //only can trade after the first 100 orders (not enough data points)
                    if(buyInit && sellInit) {
                        bot.writeBuy(currentSellPrice, currentSellQty);
                    }
                } else if ((temp > smaValue) && (counter > 100)) { //allows short selling
                    if(buyInit && sellInit) {
                        bot.writeSell(currentBuyPrice, currentBuyQty);
                    }
                }

                System.out.println("Shares: " + bot.shares + " Current buy: $" + currentBuyPrice + " current sell: $" + currentSellPrice);
                System.out.printf("PNL: $%.2f", bot.pnl);
                System.out.println("\nSMA: " + String.format("$%.2f", smaValue) + " " + counter);
                System.out.println();
                counter++;

                bot.afterOrder = false;
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            bot.close();
        }
    }
}