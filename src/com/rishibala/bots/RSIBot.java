package com.rishibala.bots;

import com.rishibala.server.Order;
import com.rishibala.server.OrderBook;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RSIBot extends Bot{
    //Relative Strength Index Calculations
    public static void main(String[] args) {

        Thread bot = new Thread(new RSIBot());
        bot.start();

    }

    @Override
    protected double[] calculate() {
        int dataSize = means.size();
        if (dataSize <= 250) {
            return new double[]{0d};
        }

        double[] priceChanges = new double[dataSize - 1];
        for (int i=1; i<dataSize; i++) {
            priceChanges[i - 1] = means.get(i) - means.get(i - 1);
        }

        //gains and losses
        double avgGain = 0;
        double avgLoss = 0;
        for (int i = 0; i < 250; i++) {
            if (priceChanges[i] > 0) {
                avgGain += priceChanges[i];
            } else {
                avgLoss += Math.abs(priceChanges[i]);
            }
        }
        avgGain /= 250;
        avgLoss /= 250;

        // relative strength rs
        double rs;
        if (avgLoss == 0) {
            rs = 100;
        } else {
            rs = avgGain / avgLoss;
        }

        return new double[]{(100 - (100 / (1 + rs)))};
    }

    @Override
    public void run() {
        trade();
    }

    @Override
    protected void trade() {
        int counter = 1;

        try {
            bot = new RSIBot();
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

                if(bot.means.size() > 251) { //period of 251 at max
                    bot.means.remove(0);
                }

                double rsiValue = bot.calculate()[0];

                if(rsiValue != 0) {
                    if ((rsiValue < 44.5) ) {
                        if(buyInit && sellInit) {
                            bot.writeBuy(currentSellPrice, currentSellQty);
                        }
                    } else if ((rsiValue > 62.5)) { //allows short selling
                        if(buyInit && sellInit) {
                            bot.writeSell(currentBuyPrice, currentBuyQty);
                        }
                    }
                }

                System.out.println("Shares: " + bot.shares + " Current buy: $" + currentBuyPrice + " current sell: $" + currentSellPrice);
                System.out.printf("PNL: $%.2f", bot.pnl);
                System.out.println("\nRSI: " + String.format("%.2f", rsiValue) + " " + counter);
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