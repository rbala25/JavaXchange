package com.rishibala;

import com.rishibala.bots.*;

public class start {
    public static void main(String[] args) {

        for(String arg : args) {
            if(arg.toLowerCase().contains("boll")) {
                BollingerBandsBot.main(null);
            } else if(arg.toLowerCase().contains("ewma")) {
                EWMABot.main(null);
            } else if(arg.toLowerCase().contains("fib")) {
                FibonacciRetracementBot.main(null);
            } else if(arg.toLowerCase().contains("rsi")) {
                RSIBot.main(null);
            } else if(arg.toLowerCase().contains("sma")) {
                SMABot.main(null);
            }
        }

    }
}
