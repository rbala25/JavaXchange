package com.rishibala.oldVersions;

import com.rishibala.server.Order;
import com.rishibala.server.OrderBook;
import com.rishibala.server.User;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FibonacciRetracementBot {
    //Fibonacci Retracement calculations

    private static final List<Double> means = new ArrayList<>();
    private static double swingHigh = 0;
    private static double swingLow = 0;
    private static Order lastBuy;
    private static Order lastSell;
    private static boolean firstCheck = true;
    private static OrderBook last;
    private static boolean afterOrder = false;
    private static int shares = 0;
    private static double pnl = 0;
    private static boolean over = false;
    private static Socket socket;
    private static BufferedReader in;
    private static BufferedWriter out;

    public static void main(String[] args) {
        int counter = 1;
        int oppCounter = 0;
        int botId = 0;
        User user = new User();

        try {
            socket = new Socket("localhost", 5000); //change localhost if on different ip
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new PrintWriter(socket.getOutputStream(), true));

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

                try {
//                    Thread.sleep(25);
                    TimeUnit.MILLISECONDS.sleep(70);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    if(in.ready()) {
                        String serverMessage = in.readLine();
                        while(serverMessage != null) {
                            if(serverMessage.contains("SIGNALOVER")) {
                                String[] serverArgs = serverMessage.split(":");
                                double lastBuy = Double.parseDouble(serverArgs[1]);
                                double lastSell = Double.parseDouble(serverArgs[2]);
                                shares = Integer.parseInt(serverArgs[3]);
                                pnl = Double.parseDouble(serverArgs[4]);
                                double tempProf = 0;

                                if(shares < 0) { //for short selling
                                    tempProf =  shares * lastBuy;
                                } else if(shares > 0) {
                                    tempProf =  shares * lastSell;
                                }

                                System.out.println("-".repeat(30));
                                System.out.println("Bot " + user.getBotId());
                                System.out.println("Final Shares: " + shares);
                                System.out.printf("Total pnl: $%.2f", pnl + tempProf);
                                System.out.println();
                                over = true;
                                break;
                            }
                            if(in.ready()) {
                                serverMessage = in.readLine();
                            } else {
                                try {
                                    TimeUnit.MILLISECONDS.sleep(3);
                                } catch(InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(!in.ready()) {
                                    break;
                                }
                            }
                        }

                        if(over) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while(in.ready()) {
                    String str = in.readLine();
                }

                try {
                    out.write("EWMAReReq");
                    out.newLine();
                    out.flush();

                    String serverMessage = "";
                    int millis = 0;
                    while (true) {

                        if (in.ready()) {
                            serverMessage = in.readLine();

                            if (serverMessage != null) {
                                if (serverMessage.contains(":")) {
                                    String[] argus = serverMessage.split(":");
                                    shares = Integer.parseInt(argus[0]);
                                    pnl = Double.parseDouble(argus[1]);
                                    break;
                                }
                            }
                        } else {
                            try {
                                millis += 2;
                                TimeUnit.MILLISECONDS.sleep(2);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        if (millis == 10) {
                            System.out.println("millis = 10 -> fail");
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                OrderBook book = new OrderBook();
                try {

                    out.write("bookReq");
                    out.newLine();
                    out.flush();

                    String serverMessage = "";
                    int millis = 0;
                    while (true) {

                        if (in.ready()) {
                            serverMessage = in.readLine();

                            if (serverMessage != null) {
                                if (serverMessage.contains("~")) {
                                    try {
                                        book = OrderBook.unserialize(serverMessage);
                                        last = book;
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        book = last;
                                        System.out.println("After order error");
                                        afterOrder = true;
                                    }
                                    break;
                                }
                            }
                        } else {
                            try {
                                millis += 2;
                                TimeUnit.MILLISECONDS.sleep(2);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        if (millis == 10) {
                            System.out.println("millis = 10 -> fail");
                            book = last;
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

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

                if(!afterOrder) {
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
                            if(order.price() < 100) {
                                System.out.println("PROBLEM: " + order);
                            }

                            currentSellPrice = order.price();
                            currentSellQty = order.quantity();
                            lastSell = order;
                            sellInit = true;
                        }
                    }
                }

                if((currentBuyPrice != Double.MIN_VALUE) && (currentSellPrice != Double.MAX_VALUE)) {
                    double mean = ((currentSellPrice + currentBuyPrice) / 2);
                    means.add(mean);
                    updateSwingHigh(mean);
                    updateSwingLow(mean);
                }

                if(means.size() > 26) {
                    means.remove(0);
                }

                double[] levels = calculateFibonacciRetracement();
                if(counter > 25 && levels != null) {
                    double buyLevel = levels[0]; // uses the 23.6% fibonacci retracement level
                    double sellLevel = levels[4]; // uses the 78.6% fibonacci retracement level

                    double temp = buyLevel - 0.6;
                    double temp1 = sellLevel + 0.9;

                    if (currentSellPrice < temp) {
                        if(buyInit && (sellInit)) {
                            out.write(botId + ", BUY" + ", " + currentSellPrice + ", " + currentSellQty);
                            out.newLine();
                            out.flush();
                            System.out.println("NEW ORDER: " + botId + ", BUY" + ", " + currentSellPrice + ", " + currentSellQty);

                            shares++;
                            pnl -= currentSellPrice;
                        }
                    } else if (currentBuyPrice > temp1) { //allows short selling
                        if(buyInit && sellInit) {
                            out.write(botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
                            out.newLine();
                            out.flush();
                            System.out.println("NEW ORDER: " + botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);

                            shares--;
                            pnl += currentBuyPrice;
                        }
                    }

                    System.out.println("Shares: " + shares + " Current buy: $" + currentBuyPrice + " current sell: $" + currentSellPrice);
                    System.out.printf("PNL: $%.2f", pnl);
                    System.out.println("\nFib Retracement Levels: " + Arrays.toString(levels) + " " + counter);
                    System.out.println();
                    counter++;
                } else {
                    System.out.println("Shares: " + shares + " Current buy: $" + currentBuyPrice + " current sell: $" + currentSellPrice);
                    System.out.printf("PNL: $%.2f", pnl);
                    System.out.println("\nFib Retracement Levels: N/A" + " " + counter);
                    System.out.println();
                    counter++;
                }

                afterOrder = false;
            }
        } catch(IOException e) {
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

    private static double[] calculateFibonacciRetracement() {
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

    private static void updateSwingHigh(double point) {
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

    private static void updateSwingLow(double point) {
        List<Double> periodData;
        if(means.size() >= 250) {
            periodData = means.subList((means.size() - 250), means.size() - 1);
        } else if(means.size() == 1) {
            swingHigh = means.get(0);
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