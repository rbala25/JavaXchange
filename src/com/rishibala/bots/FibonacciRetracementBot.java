package com.rishibala.bots;

import com.rishibala.server.Order;
import com.rishibala.server.OrderBook;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FibonacciRetracementBot extends Bot{
    //Fibonacci Retracement calculations

    private static double swingHigh = 0;
    private static double swingLow = 0;

    public static void main(String[] args) {
        int counter = 1;

        try {
            bot = new FibonacciRetracementBot();
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
                    double mean = ((currentSellPrice + currentBuyPrice) / 2);
                    bot.means.add(mean);

                    FibonacciRetracementBot fibBot = (FibonacciRetracementBot) bot;
                    fibBot.updateSwingHigh(mean);
                    fibBot.updateSwingLow(mean);
                }

                if(bot.means.size() > 26) {
                    bot.means.remove(0);
                }

                double[] levels = bot.calculate();
                if(counter > 25 && levels != null) {
                    double buyLevel = levels[0]; // uses the 23.6% fibonacci retracement level
                    double sellLevel = levels[4]; // uses the 78.6% fibonacci retracement level

                    double temp = buyLevel - 0.6;
                    double temp1 = sellLevel + 0.9;

                    if (currentSellPrice < temp) {
                        if(buyInit && (sellInit)) {
                            bot.writeBuy(currentSellPrice, currentSellQty);
                        }
                    } else if (currentBuyPrice > temp1) { //allows short selling
                        if(buyInit && sellInit) {
                            bot.writeSell(currentBuyPrice, currentBuyQty);
                        }
                    }

                    System.out.println("Shares: " + bot.shares + " Current buy: $" + currentBuyPrice + " current sell: $" + currentSellPrice);
                    System.out.printf("PNL: $%.2f", bot.pnl);
                    System.out.println("\nFib Retracement Levels: " + Arrays.toString(levels) + " " + counter);
                    System.out.println();
                    counter++;
                } else {
                    System.out.println("Shares: " + bot.shares + " Current buy: $" + currentBuyPrice + " current sell: $" + currentSellPrice);
                    System.out.printf("PNL: $%.2f", bot.pnl);
                    System.out.println("\nFib Retracement Levels: N/A" + " " + counter);
                    System.out.println();
                    counter++;
                }

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
        if (means.size() < 25) {
            return null; // no trades until after we have 25 full points (not enough data points)
        }

        if ((swingHigh == 0) || (swingLow == 0)) {
            return null;
        }

        double[] retracementLevels = new double[5];
        double priceRange = swingHigh - swingLow;

        retracementLevels[0] = swingHigh - (0.236 * priceRange);
        retracementLevels[1] = swingHigh - (0.382 * priceRange);
        retracementLevels[2] = swingHigh - (0.5 * priceRange);
        retracementLevels[3] = swingHigh - (0.618 * priceRange);
        retracementLevels[4] = swingHigh - (0.786 * priceRange);

        return retracementLevels;
    }

    private void updateSwingHigh(double point) {
        List<Double> periodData;
        if(means.size() >= 250) {
            periodData = means.subList((means.size() - 250), means.size() - 1);
        } else if(means.size() == 1) {
            swingHigh = means.get(0);
            return;
        } else {
            periodData = means.subList(0, means.size() - 1);
        }

        double max = Double.MIN_VALUE;
        for(double price : periodData) {
            if(price > max) {
                max = price;
            }
        }

        if(point > max) {
            swingHigh = point;
        }
    }

    private void updateSwingLow(double point) {
        List<Double> periodData;
        if(means.size() >= 250) {
            periodData = means.subList((means.size() - 250), means.size() - 1);
        } else if(means.size() == 1) {
            swingLow = means.get(0);
            return;
        } else {
            periodData = means.subList(0, means.size() - 1);
        }

        double min = Double.MAX_VALUE;
        for(double price : periodData) {
            if(price < min) {
                min = price;
            }
        }

        if(point < min) {
            swingLow = point;
        }
    }

}