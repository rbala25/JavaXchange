package com.rishibala;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

class writeData {
    //You only need to run this class once in order to generate the data.txt file
    private static final String API_KEY = config.API_KEY;

    public static void main(String[] args) {
        try {
            File file = new File("/Users/rishibala/IdeaProjects/StockExchange/src/com/rishibala/data.txt");
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
            } else {
                System.out.println("File alr exists.");
            }

            FileWriter myWriter = new FileWriter("/Users/rishibala/IdeaProjects/StockExchange/src/com/rishibala/data.txt");
            myWriter.flush();
            myWriter.write(getData());
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    private static String getData() {
        StringBuilder all = new StringBuilder();

        int startYear = 2021;
        int startMonth = 6;
        int startDay = 1;

        int endYear = 2021;
        int endMonth = 6;
        int endDay = 15;


        int i = 0;
        while(endYear != 2023) {
            i++;
            if(i != 1) {
                try {
                    TimeUnit.SECONDS.sleep(62);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }

            String startDate = String.format("%d-%02d-%02d", startYear, startMonth, startDay);
            String endDate = String.format("%d-%02d-%02d", endYear, endMonth, endDay);

            startDay += 12;
            endDay+=12;

            if((startMonth == 2 && startDay > 28) || (((startMonth == 9) || (startMonth == 04) || (startMonth == 06) || (startMonth == 11)) && startDay > 30) || (startDay > 31)) {
                if(startMonth == 2) {
                    startDay %= 28;
                } else if ((startMonth == 9) || (startMonth == 04) || (startMonth == 06) || (startMonth == 11)) {
                    startDay %= 30;
                } else {
                    startDay %= 31;
                }
                startMonth++;
            }
            if(startMonth > 12) {
                startYear++;
                startMonth = 1;
            }

            if((endMonth == 2 && endDay > 28) || (((endMonth == 9) || (endMonth == 04) || (endMonth == 06) || (endMonth == 11)) && endDay > 30) || (endDay > 31)) {
                if(endMonth == 2) {
                    endDay %= 28;
                } else if ((endMonth == 9) || (endMonth == 04) || (endMonth == 06) || (endMonth == 11)) {
                    endDay %= 30;
                } else {
                    endDay %= 31;
                }
                endMonth++;
            }
            if(endMonth > 12) {
                endYear++;
                endMonth = 1;
            }


            String jsonReq = "";

            System.out.println(i + " " + startDate + " " + endDate);

            try {
                String url = "https://api.twelvedata.com/time_series?symbol=AAPL" +
                        "&interval=1min" +
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

                jsonReq = response.toString();
                all.append(jsonReq);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return all.toString();
    }
}
