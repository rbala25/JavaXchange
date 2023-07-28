package com.rishibala.bots;

import com.rishibala.server.OrderBook;
import com.rishibala.server.StockExchange;
import com.rishibala.server.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class TradingBot {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 3000); //change localhost if on different ip
//            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner s = new Scanner(System.in);

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            OrderBook book = StockExchange.getBook();
            int botId = StockExchange.getId(socket);
            User user = StockExchange.getUser(socket);

            if(botId == -1 || user == null) {
                System.out.println(botId);
                System.out.println(user);
                System.out.println("error");
                return;
            }

            System.out.println("here");

            while(true) {
                System.out.println("""
                       Current Orders (o)
                       New Order (n)
                       Cancel order (c)
                       Portfolio (p)
                       Quit (q)
                        """);
                System.out.println("-".repeat(20));
                String choice = s.nextLine().substring(0, 1).toLowerCase();

                if(choice.equals("o")) {
                    System.out.println(OrderBook.getListedMatches(botId, book));
                    System.out.println("-".repeat(30) + "\n");
                } else if (choice.equals("n")) {
                    System.out.println();
                    String buySell = "";
                    do {
                        System.out.println("Buy or sell: ");
                        buySell = s.nextLine().substring(0, 1).toLowerCase();

                    } while (!buySell.equalsIgnoreCase("b") && !buySell.equalsIgnoreCase("s"));

                    if(buySell.equalsIgnoreCase("b")) buySell = "BUY";
                    if(buySell.equalsIgnoreCase("s")) buySell = "SELL";

                    double price = 0;
                    while(!(price > 0)) {
                        System.out.println("Price: ");
                        try {
                            double temp = Double.parseDouble(s.nextLine());
                            price = temp;
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }

                    int qty = 0;
                    while(!(qty > 0)) {
                        System.out.println("Quantity: ");
                        try {
                            int temp = Integer.parseInt(s.nextLine());
                            qty = temp;
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }

                    out.println(botId + ", " + buySell + ", " + price + ", " + qty);

                } else if(choice.equals("c")) {
                    System.out.println();
                    int order = 0;
                    while(!(order > 0)) {
                        System.out.println("Order Number: ");
                        try {
                            int temp = Integer.parseInt(s.nextLine());
                            order = temp;
                            boolean check = OrderBook.haveOrder(botId, book, order);
                            if(!check) {
                                order = 0;
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }

                    out.println("cancel, " + order);
                } else if(choice.equals("p")) {
                    System.out.println();
                    System.out.println(user.getStockAmt() + " shares.");
                } else if(choice.equals("q")) {
                    break;
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
