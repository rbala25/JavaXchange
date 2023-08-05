package com.rishibala.oldVersions;

import com.rishibala.server.Order;
import com.rishibala.server.OrderBook;
import com.rishibala.server.User;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EWMABot {
    //Estimated weighted moving average calculations

//    private static List<Double> buyData = new ArrayList<>();
//    private static List<Double> sellData = new ArrayList<>();
    private static final List<Double> means = new ArrayList<>();
    private static Order lastBuy;
    private static Order lastSell;
    private static boolean firstCheck = true;
    private static OrderBook last = new OrderBook();
//    private static boolean afterOrder = false;
    private static int shares = 0;
    private static double pnl = 0;
    private static boolean over = false;
    private static Socket socket;
    private static BufferedReader in;
    private static BufferedWriter out;

    public static void main(String[] args) {
        int counter = 1;
//        int oppCounter = 0;
        int botId = 0;
        User user = new User();

        try {
            socket = new Socket("localhost", 5000); //change localhost if on different ip
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
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
                                    tempProf =  shares * lastSell;
                                } else if(shares > 0) {
                                    tempProf =  shares * lastBuy;
                                }

                                System.out.println("-".repeat(30));
                                System.out.println("Bot " + user.getBotId());
                                System.out.println("Final Shares: " + shares);
                                System.out.printf("Total pnl: $%.2f", (pnl + tempProf));
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

//                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                out = new BufferedWriter(new PrintWriter(socket.getOutputStream(), true));

                OrderBook book = new OrderBook();
                try {

//                    out.println("bookReq");
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
                    Order order = buys.get(0);
                    currentBuyPrice = order.price();
                    currentBuyQty = order.quantity();
                    lastBuy = order;
                    buyInit = true;
                }
                for(List<Order> sells : book.getSellOrders().values()) {
                    Order order = sells.get(0);
                    currentSellPrice = order.price();
                    currentSellQty = order.quantity();
                    lastSell = order;
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
                    means.add((currentSellPrice + currentBuyPrice) / 2);
                }

                if(means.size() > 7001) { //period of 7000
                    means.remove(0);
                }

                double ewmaValue = calculateEWMA();

                double temp = currentBuyPrice;
//                double temp1 = currentSellPrice;

                if(shares > 50) {  //more incentive to sell
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
                        out.write(botId + ", BUY" + ", " + currentSellPrice + ", " + currentSellQty);
                        out.newLine();
                        out.flush();
                        System.out.println("NEW ORDER: " + botId + ", BUY" + ", " + currentSellPrice + ", " + currentSellQty);

                        shares++;
                        pnl -= currentSellPrice;
                    }
                } else if ((temp > ewmaValue) && (counter > 100)) { //allows short selling
                    if(buyInit && sellInit) {
//                      out.println(botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
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
                System.out.println("\nEWMA: " + String.format("$%.2f", ewmaValue) + " " + counter);
//                System.out.println("Potential current PNL: " + );
                System.out.println();
                counter++;

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

    private static double calculateEWMA() {
        double alpha = 0.2;
        if(means.size() > 0) {
            double ewma = means.get(0);
            for (int i = 1; i < means.size(); i++) {
                double currentPrice = means.get(i);
                ewma = alpha * currentPrice + (1 - alpha) * ewma;
            }
            return ewma;
        }
        return 0;
    }

}