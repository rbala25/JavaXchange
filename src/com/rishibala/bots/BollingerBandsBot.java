package com.rishibala.bots;

import com.rishibala.server.Order;
import com.rishibala.server.OrderBook;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BollingerBandsBot extends Bot{
    //Bollinger Bands Calculations

    private static double UpperBand;
    private static double MiddleBand;
    private static double LowerBand;

    public static void main(String[] args) {
        int counter = 1;

        try {
            bot = new BollingerBandsBot();
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

                if(bot.means.size() > 50) { //period of 50 at max
                    bot.means.remove(0);
                }

                bot.calculate();
                double temp1 = currentSellPrice - 0.15;
                double temp = currentBuyPrice + 0.07;

                if ((temp1 < LowerBand) && (LowerBand != 0d)) {
                    if(buyInit && sellInit) {
                        bot.writeBuy(currentSellPrice, currentSellQty);
                    }
                } else if ((temp > UpperBand) && (UpperBand != 0d)) { //allows short selling
                    if(buyInit && sellInit) {
                        bot.writeSell(currentBuyPrice, currentBuyQty);
                    }
                }

                System.out.println("Shares: " + bot.shares + " Current buy: " + currentBuyPrice + " current sell: " + currentSellPrice);
                System.out.printf("PNL: $%.2f", bot.pnl);
                System.out.println("\nBands: " + String.format("$%.2f, $%.2f, $%.2f", UpperBand, MiddleBand, LowerBand) + " " + counter);
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

    @Override
    protected double[] calculate() {
        if (means.size() < 50) {
            return null; //do not make any trades until we have a full period (not enough data points)
        }

        // SMA is middle band
        double sum = 0;
        for (int i = means.size() - 50; i < means.size(); i++) {
            sum += means.get(i);
        }
        MiddleBand = sum / 50;

        // standard deviation
        double sumSquaredDifference = 0;
        for (int i = means.size() - 50; i < means.size(); i++) {
            double difference = means.get(i) - MiddleBand;
            sumSquaredDifference += difference * difference;
        }
        double standardDeviation = Math.sqrt(sumSquaredDifference / (50 - 1));

        // upper and lower
        UpperBand = MiddleBand + (2 * standardDeviation);
        LowerBand = MiddleBand - (2 * standardDeviation);
        
        return null;
    }
}