package com.rishibala.bots;

import com.rishibala.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class DummyBot {

    private static final String API_KEY = config.API_KEY;
    private static final Map<LocalDate, Double> data = parseTempData();

    public static void main(String[] args) {
        for (LocalDate date : data.keySet()) {
            double price = data.get(date);
            System.out.println(date + ": " + price);
        }

//        try {
//            Socket socket = new Socket("localhost", 3000); //change localhost if on different ip
//            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//
//
//
//        } catch(IOException e) {
//            e.printStackTrace();
//        }


    }

    private static Map<LocalDate, Double> getData() {
        String startDate = "2023-01-01";
        String endDate = "2023-06-30";

        Map<LocalDate, Double> data = new TreeMap<>();

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

//                System.out.println(Arrays.toString(dataItems));

                String dateHalf = dataItems[0].substring(12, dataItems[0].lastIndexOf("\""));
                LocalDate date = LocalDate.parse(dateHalf);

                String closeHalf = dataItems[4].substring(9, dataItems[4].lastIndexOf("\""));
                double price = Double.parseDouble(closeHalf);

                data.put(date, price);
//                    closingIndex = jsonResponse.indexOf("\"close\":\"", endIndex);
//                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    private static Map<LocalDate, Double> parseTempData() {
        Map<LocalDate, Double> data = new TreeMap<>();

        try {
            File file = new File("tempData.txt");
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
                LocalDate date = LocalDate.parse(dateHalf);

                String closeHalf = dataItems[4].substring(9, dataItems[4].lastIndexOf("\""));
                double price = Double.parseDouble(closeHalf);

                data.put(date, price);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }
}
