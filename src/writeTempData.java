import com.rishibala.config;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

class writeTempData {
    private static final String API_KEY = config.API_KEY;

    public static void main(String[] args) {
        try {
            File myObj = new File("tempData.txt");
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }

            FileWriter myWriter = new FileWriter("tempData.txt");
            myWriter.write(getData());
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    private static String getData() {
        String startDate = "2023-01-01";
        String endDate = "2023-06-30";
        String jsonReq = "";

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

            jsonReq = response.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jsonReq;
    }
}
