import com.rishibala.config;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

class writeData {
    private static final String API_KEY = config.API_KEY;

    public static void main(String[] args) {
        try {
            File file = new File("data.txt");
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
            } else {
                System.out.println("File alr exists.");
            }

            FileWriter myWriter = new FileWriter("data.txt");
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
        String startDate = "2022-07-01";
        String endDate = "2023-07-01";
        String jsonReq = "";

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
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jsonReq;
    }
}
