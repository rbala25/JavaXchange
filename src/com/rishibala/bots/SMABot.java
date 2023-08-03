package com.rishibala.bots;

import com.rishibala.server.Order;
import com.rishibala.server.OrderBook;
import com.rishibala.server.User;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SMABot {
    //Simple moving average calculations

    private static List<Double> periodBuy = new ArrayList<>();
    private static List<Double> periodSell = new ArrayList<>();
    private static List<Double> means = new ArrayList<>();
    private static Order lastBuy;
    private static Order lastSell;
    private static boolean firstCheck = true;
    private static OrderBook last;
    private static boolean afterOrder = false;
    private static int shares = 0;
    private static double pnl = 0;

    public static void main(String[] args) {
        int counter = 1;
        int oppCounter = 0;
        int botId = 0;
        User user = new User();

        try {
            Socket socket = new Socket("localhost", 5000); //change localhost if on different ip
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
                    TimeUnit.MILLISECONDS.sleep(30);
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
                                System.out.printf("Total pnl: $%.2f", pnl);
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
                            System.out.println("millis = 10");
                            book = last;
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for(List<Order> buys : book.getBuyOrders().values()) {
                    for(Order order : buys) {
                        if(periodBuy.size() > 750) {
                            periodBuy.add(order.price());
                            periodBuy.remove(0);
                        } else {
                            periodBuy.add(order.price());
                        }
                    }
                }
                for(List<Order> sells : book.getSellOrders().values()) {
                    for(Order order : sells) {
                        if(periodSell.size() > 750) {
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
                    means.add((currentSellPrice + currentBuyPrice) / 2);
                }

                double smaValue = calculateSMA();

                if ((currentSellPrice < smaValue) && (counter < 100)) { //only can trade after the first 100 orders (not enough data points)
                    if(buyInit && sellInit) {
                        out.write(botId + ", BUY" + ", " + currentSellPrice + ", " + currentSellQty);
                        out.newLine();
                        out.flush();
                        System.out.println("NEW ORDER: " + botId + ", BUY" + ", " + currentSellPrice + ", " + currentSellQty);

                        shares++;
                        pnl -= currentSellPrice;
                    }
                } else if ((currentBuyPrice > smaValue) && (counter < 100)) { //allows short selling
                    if(buyInit && sellInit) {
                        if(user.getStockAmt() <= 0) {
                            out.write(botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
                            out.newLine();
                            out.flush();
                            System.out.println("NEW ORDER: " + botId + ", SELL" + ", " + currentBuyPrice + ", " + currentBuyQty);
                        }
                        if(user.getStockAmt() >= 1) {
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
                System.out.println("\nSMA: " + String.format("$%.2f", smaValue) + " " + counter);
                System.out.println();
                counter++;

                afterOrder = false;
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    private static double calculateSMA() {
        double sum = 0;
        for(double data : means) {
            sum += data;
        }

        return (sum /(means.size()));
    }

}
