package com.rishibala;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Start {
    public static void main(String[] args) {

        if(args.length > 0) {
            try {
                openExchange();
                TimeUnit.MILLISECONDS.sleep(100);
                openTerminal("MarketMaker");
                TimeUnit.MILLISECONDS.sleep(2100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for(String arg : args) {
            if(arg.toLowerCase().contains("boll")) {
                openTerminal("BollingerBandsBot");
            } else if(arg.toLowerCase().contains("ewma")) {
                openTerminal("EWMABot");
            } else if(arg.toLowerCase().contains("fib")) {
                openTerminal("FibonacciRetracementBot");
            } else if(arg.toLowerCase().contains("rsi")) {
                openTerminal("RSIBot");
            } else if(arg.toLowerCase().contains("sma")) {
                openTerminal("SMABot");
            }

            try {
                TimeUnit.MILLISECONDS.sleep(700);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    public static void openTerminal(String fileName) {
        try { //only for Mac
            String path = "IdeaProjects/StockExchange/src";
            String[] command = {"osascript", "-e", "tell application \"Terminal\" to do script \"cd " + path + " && java com.rishibala.bots." + fileName + "\""};

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File("."));
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void openExchange() {
        try {
            String path = "IdeaProjects/StockExchange/src";
            String[] command = {"osascript", "-e", "tell application \"Terminal\" to do script \"cd " + path + " && java com.rishibala.server.StockExchange\""};

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File("."));
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
