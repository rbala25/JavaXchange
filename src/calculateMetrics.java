import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class calculateMetrics {
    public static void main(String[] args) {
        //mean of data prices (for variance formula) = 152.73744382989653
        //only need to run this once

        Map<LocalDateTime, Double> data = parseConstantData();
        double sum = 0;
        for(double dat : data.values()) {
            sum += dat;
        }

        sum /= data.size();
        System.out.println(sum);


        //min and max prices
        //min = 123.165
        //max = 182.855
        //difference = 59.69

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for(double dat : data.values()) {
            if(dat < min) {
                min = dat;
            }
            if(dat > max) {
                max = dat;
            }
        }

        System.out.println(min);
        System.out.println(max);


        //variance = 176.48421229791134
        //sqrt = 13.284736064292408
        //coefficient of variation = 1.1554744394862437

        double sum1 = 0;
        long counter = 0;
        for(double dat : data.values()) {
            sum1 += ((dat - 152.73744382989653) * (dat - 152.73744382989653));
            counter++;
        }

        System.out.println(sum1);
        System.out.println(sum1 / (counter - 1));
        System.out.println("Sqrt: " + Math.sqrt((sum1 / (counter - 1))));
        System.out.println("Coefficient of Variation: " + (sum1 / (counter - 1)) / 152.73744382989653);

    }

    private static Map<LocalDateTime, Double> parseConstantData() {
        Map<LocalDateTime, Double> data = new TreeMap<>();

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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

}
