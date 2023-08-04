package com.rishibala.bots;

import com.rishibala.server.Order;
import com.rishibala.server.OrderBook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class Bot {

    protected final List<Double> means = new ArrayList<>();
    protected Order lastBuy;
    protected Order lastSell;
    protected boolean firstCheck = true;
    protected OrderBook last = new OrderBook();
    protected int shares = 0;
    protected double pnl = 0;
    protected boolean over = false;
    protected static Bot bot;
    protected int botId;
    protected Socket socket;
    protected BufferedReader in;
    protected BufferedWriter out;
    protected boolean afterOrder = false;


    protected abstract double[] calculate();

    protected void writeSell(double currentBuyPrice, int currentBuyQty) {
        bot.write(bot.botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
        System.out.println("NEW ORDER: " + bot.botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);

        bot.shares--;
        bot.pnl += currentBuyPrice;
    }

    protected void writeBuy(double currentSellPrice, int currentSellQty) {
        bot.write(bot.botId + ", BUY" + ", " + currentSellPrice + ", " + currentSellQty);
        System.out.println("NEW ORDER: " + bot.botId + ", BUY" + ", " + currentSellPrice + ", " + currentSellQty);

        bot.shares++;
        bot.pnl -= currentSellPrice;
    }

    protected void initalCheck() {
        try {
            String serverMessage = bot.in.readLine();
            if(serverMessage != null) {
                String[] arg = serverMessage.split(",");
                botId = Integer.parseInt(arg[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    protected void write(String message) {
        try {
            out.write(message);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    protected void close() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    protected void checkEnd() {
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
    protected void getUser() {
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
    protected OrderBook getBook() {
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
