package com.rishibala.bots;

import com.rishibala.config;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MarketMakingBot {

    private static final String API_KEY = config.API_KEY;
    private static Socket socket;
    private static PrintWriter out;

    public static void main(String[] args) {
        Map<LocalDateTime, Double> data = parseConstantData();
//        for (LocalDateTime date : data.keySet()) {
//            double price = data.get(date);
//            System.out.println(date + ": " + price);
//         }

        int size = data.size();
        System.out.println("Size: " + size);

        try {
            socket = new Socket("localhost", 5000); //change localhost if on different ip
//            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Map<LocalDateTime, Double> data1 = new TreeMap<>(data);
            var keys = data1.keySet().toArray();

            double lastSell = 0;
            double lastBuy = 0;

            for(int i=0; i<keys.length; i++) {
                LocalDateTime key = (LocalDateTime) keys[i];

                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                double price = data.get(key);

//                out.println("checkerTrueListed");

                //0.11554744394 is 1/10th of the Coefficient of Variation in calculateMetrics.java
                out.println("0, " + "BUY" + ", " + (price - 0.11554744394) + ", 1");
                out.println("0, " + "SELL" + ", " + (price + 0.11554744394) + ", 1");

                out.println("close");
                out.flush();

                lastSell = price + 0.11554744394;
                lastBuy = price - 0.11554744394;
            }
            out.println("MMBOT_OVER:" + lastBuy + ":" + lastSell);

        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Map<LocalDateTime, Double> getData() {
        String startDate = "2023-01-01";
        String endDate = "2023-06-30";

        Map<LocalDateTime, Double> data = new TreeMap<>();

        try {
            String url = "https://api.twelvedata.com/time_series?symbol=AAPL" +
                    "&interval=1day" +
                    "&start_date=" + startDate +
                    "&end_date=" + endDate +
                    "&apikey=" + API_KEY;

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            String jsonResponse = response.toString();
//            System.out.println(jsonResponse);

            List<Integer> indexes = new ArrayList<>();
            int temp = jsonResponse.indexOf("\"datetime\":\"");
            while (temp >= 0) {
                indexes.add(temp);
                temp = jsonResponse.indexOf("\"datetime\":\"", temp + 1);
            }

            for (int index : indexes) {
//                int closingIndex = jsonResponse.indexOf("\"close\":\"", index);

//                while (closingIndex != -1) {
                int endIndex = jsonResponse.indexOf("}", index);

                if (endIndex == -1) {
                    break;
                }

                String data1 = jsonResponse.substring(index, endIndex);
                String[] dataItems = data1.split(",");

                String dateHalf = dataItems[0].substring(12, dataItems[0].lastIndexOf("\""));
                LocalDateTime dateTime = LocalDateTime.of(LocalDate.parse(dateHalf.substring(0, dateHalf.indexOf(" "))),
                        LocalTime.parse(dateHalf.substring(dateHalf.indexOf(" ") + 1)));

                String closeHalf = dataItems[4].substring(9, dataItems[4].lastIndexOf("\""));
                double price = Double.parseDouble(closeHalf);

                data.put(dateTime, price);
//                    closingIndex = jsonResponse.indexOf("\"close\":\"", endIndex);
//                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    private static Map<LocalDateTime, Double> parseConstantData() {
        Map<LocalDateTime, Double> data = new TreeMap<>(); //first 12000 data points

        try {

            File file = new File("/Users/rishibala/IdeaProjects/StockExchange/src/data.txt");
            Scanner s = new Scanner(file);
            String jsonResponse = s.nextLine();

            List<Integer> indexes = new ArrayList<>();
            int temp = jsonResponse.indexOf("\"datetime\":\"");
            while (temp >= 0) {
                indexes.add(temp);
                temp = jsonResponse.indexOf("\"datetime\":\"", temp + 1);
            }

            for (int index : indexes) {
                int endIndex = jsonResponse.indexOf("}", index);

                if (endIndex == -1) {
                    break;
                }

                String data1 = jsonResponse.substring(index, endIndex);
                String[] dataItems = data1.split(",");

                String dateHalf = dataItems[0].substring(12, dataItems[0].lastIndexOf("\""));
                LocalDateTime dateTime = LocalDateTime.of(LocalDate.parse(dateHalf.substring(0, dateHalf.indexOf(" "))),
                        LocalTime.parse(dateHalf.substring(dateHalf.indexOf(" ") + 1)));

                String closeHalf = dataItems[4].substring(9, dataItems[4].lastIndexOf("\""));
                double price = Double.parseDouble(closeHalf);

                data.put(dateTime, price);

                if(data.size() >= 300) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }
}