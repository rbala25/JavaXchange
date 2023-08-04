package com.rishibala.tempBots;

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
    private static Socket socket;
    private static BufferedReader in;
    private static BufferedWriter out;
    private static boolean afterOrder = false;

    public static void main(String[] args) {
        int counter = 1;
        int botId = 0;

        try {
           socket = new Socket("localhost", 5000); //change localhost if on different ip
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new PrintWriter(socket.getOutputStream(), true));
            BollingerBandsBot bot = new BollingerBandsBot();

            try {
                String serverMessage = in.readLine();
                if(serverMessage != null) {
                    String[] arg = serverMessage.split(",");
                    botId = Integer.parseInt(arg[0]);
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

                bot.checkEnd(botId);
                if(bot.over) {
                    break;
                }

                bot.getUser(botId);

                OrderBook book = bot.getBook(botId);

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

                if(!afterOrder) {
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
                        out.write(botId + ", BUY" + ", " + currentSellPrice + ", " + currentSellQty);
                        out.newLine();
                        out.flush();
                        System.out.println("NEW ORDER: " + botId + ", BUY" + ", " + currentSellPrice + ", " + currentSellQty);

                        bot.shares++;
                        bot.pnl -= currentSellPrice;
                    }
                } else if ((temp > UpperBand) && (UpperBand != 0d)) { //allows short selling
                    if(buyInit && sellInit) {
                        out.write(botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
                        out.newLine();
                        out.flush();
                        System.out.println("NEW ORDER: " + botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);

                        bot.shares--;
                        bot.pnl += currentBuyPrice;
                    }
                }

                System.out.println("Shares: " + bot.shares + " Current buy: " + currentBuyPrice + " current sell: " + currentSellPrice);
                System.out.printf("PNL: $%.2f", bot.pnl);
                System.out.println("\nBands: " + String.format("$%.2f, $%.2f, $%.2f", UpperBand, MiddleBand, LowerBand) + " " + counter);
                System.out.println();
                counter++;

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

    @Override
    protected void calculate() {
        if (means.size() < 50) {
            return; //do not make any trades until we have a full period (not enough data points)
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
    }

    @Override
    protected void checkEnd(int botId) {
        try {
            if (in.ready()) {
                String serverMessage = in.readLine();
                while (serverMessage != null) {
                    if (serverMessage.contains("SIGNALOVER")) {
                        String[] serverArgs = serverMessage.split(":");
                        double lastBuy = Double.parseDouble(serverArgs[1]);
                        double lastSell = Double.parseDouble(serverArgs[2]);
                        shares = Integer.parseInt(serverArgs[3]);
                        pnl = Double.parseDouble(serverArgs[4]);
                        double tempProf = 0;

                        if (shares < 0) { //for short selling
                            tempProf = shares * lastSell;
                        } else if (shares > 0) {
                            tempProf = shares * lastBuy;
                        }

                        System.out.println("-".repeat(30));
                        System.out.println("Bot " + botId);
                        System.out.println("Final Shares: " + shares);
                        System.out.printf("Total pnl: $%.2f", (pnl + tempProf));
                        System.out.println();
                        over = true;
                        break;
                    }
                    if (in.ready()) {
                        serverMessage = in.readLine();
                    } else {
                        try {
                            TimeUnit.MILLISECONDS.sleep(3);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (!in.ready()) {
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void getUser(int botId) {
        try {
            while(in.ready()) {
                String str = in.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
    }

    @Override
    protected OrderBook getBook(int botId) {
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

        return book;
    }
}