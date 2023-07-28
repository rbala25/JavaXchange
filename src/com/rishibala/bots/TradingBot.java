package com.rishibala.bots;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TradingBot {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 3000); //change localhost if on different ip
//            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner s = new Scanner(System.in);


            while(true) {



            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
