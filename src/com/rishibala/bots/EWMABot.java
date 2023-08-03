package com.rishibala.bots;

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

    public static void main(String[] args) {
        int counter = 1;
//        int oppCounter = 0;
        int botId = 0;
        User user = new User();

        try {
            Socket socket = new Socket("localhost", 5000); //change localhost if on different ip
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedWriter out = new BufferedWriter(new PrintWriter(socket.getOutputStream(), true));

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
                    TimeUnit.MILLISECONDS.sleep(60);
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
                                int tempProf = 0;

                                if(shares < 0) { //for short selling
                                    for(int i=1; i<=(shares * -1); i++) {
                                        tempProf -= lastBuy;
                                    }
                                } else if(shares > 0) {
                                    for(int i=1; i<=shares; i++) {
                                        tempProf += lastSell;
                                    }
                                }

                                System.out.println("Bot " + user.getBotId());
                                System.out.printf("Total pnl: $%.2f", pnl + tempProf);
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
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while(in.ready()) {
                    String str = in.readLine();
                }

//                out.println("EWMAReReq");
//                out.flush();
//                try {
//
//                    String serverMessage = "";
//                    int counter3 = 0;
//                    boolean counter3b = false;
//
//                    while(!in.ready()) {
//                        try {
//                            TimeUnit.MILLISECONDS.sleep(1);
//                            if(in.ready()) {
//                                break;
//                            }
//                            counter3++;
//
//                            if(counter3 == 10) {
//                                System.out.println("counter 3 = 10");
//                                counter3b = true;
//                                break;
//                            }
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    if(!counter3b) {
//                        serverMessage = in.readLine();
//                    }
//
//                    while(serverMessage != "") {
//                        if(serverMessage.contains(":")) {
//                            user = User.unStringWithProfit(serverMessage);
//                            break;
//                        }
//
//                        int counter2 = 0;
//                        while(!in.ready()) {
//                            try {
//                                TimeUnit.MILLISECONDS.sleep(1);
//                                if(in.ready()) {
//                                    break;
//                                }
//                                counter2++;
//
//                                if(counter2 == 10) {
//                                    break;
//                                }
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        if(in.ready()) {
//                            serverMessage = in.readLine();
//                        } else {
//                            break;
//                        }
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

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

                if(means.size() > 5000) { //period of 5000
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

                currentSellPrice -= 0.02;
                temp -= 0.02;
                if ((currentSellPrice < ewmaValue) && (counter > 100)) { //only can trade after the first 100 orders (not enough data points)
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
                        if(user.getStockAmt() <= 0) {
//                            out.println(botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
                            out.write(botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
                            out.newLine();
                            out.flush();
                            System.out.println("NEW ORDER: " + botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
                        }
                        if(user.getStockAmt() >= 1) {
//                            out.println(botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
                            out.write(botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
                            out.newLine();
                            out.flush();
                            System.out.println("NEW ORDER: " + botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
                        }

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