// todo: Write JSON data to a file and read from it
// todo: Count outdoor time
// todo: Repeat the process for all the users
// todo: Save all the resulting data into a CSV file(or any other file)

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class Main {
    private static final int MAX_ENTRIES = 10000;
    private static final double MAX_DISTANCE = 30;

    private static DecimalFormat df2 = new DecimalFormat("#.##");

    private static String[] LastUpdate = new String[MAX_ENTRIES];
    private static String[][] coordinates = new String[MAX_ENTRIES][2];
    //private static ArrayList<String> LastUpdate = new ArrayList<>();
    //private static ArrayList<ArrayList<String>> coordinates = new ArrayList<>();

    private static ArrayList<String> uid = new ArrayList<>();

    private static int curIndex = 0;

    public static void main(String[] args) throws IOException, JSONException, Exception {
        String id, lastUpdate, latitude, longitude;
        String KEY = "NA";

        /*ArrayList<JSONObject> jsonObjects = new ArrayList<JSONObject>();
        int total = 0;
        try{
            while (true){
                JSONObject temp = readJsonFromUrl("https://noiyg61bui.execute-api.ap-southeast-1.amazonaws.com/STICKuDashboardStage/stickudashboardgetalluserlocationresource?LastEvaluatedKey=" + KEY);
                jsonObjects.add(temp);
                KEY = temp.getJSONObject("LastEvaluatedKey").getJSONObject("id").getString("S");
                System.out.println(KEY);
                total++;
            }
        }
        catch (JSONException jsonException){
            total*=1000;
            System.out.println("FINISHED with " + total + " items");
        }

        int j = 0;
        for (JSONObject jsonObject: jsonObjects) {
            writeJsonToFile(jsonObject, "data\\data" + j + ".json");
            j++;
        }*/

        int numFiles = 274;

        for (int j = 0; j < numFiles; j++) {
            JSONObject json = readJsonFromFile("data\\data" + j + ".json");
            JSONArray items = json.getJSONArray("Items");
            for (int i = 0; i < items.length(); i++) {
                try{
                    JSONObject item = items.getJSONObject(i);
                    id = item.getJSONObject("UID").getString("S");

                    if(!uid.contains(id)){
                        uid.add(id);
                    }
                }
                catch (JSONException jsonException){
                    continue;
                }
            }
        }

        for (int k = 0; k < uid.size(); k++) {
            for (int j = 0; j < numFiles; j++) {
                JSONObject json = readJsonFromFile("data\\data" + j + ".json");

                JSONArray items = json.getJSONArray("Items");
                for (int i=0; i< items.length(); i++) {
                    try{
                        JSONObject item = items.getJSONObject(i);
                        id = item.getJSONObject("UID").getString("S");
                        lastUpdate = item.getJSONObject("LastUpdate").getString("S");
                        latitude = item.getJSONObject("Latitude").getString("S");
                        longitude = item.getJSONObject("Longitude").getString("S");

                        if(uid.get(k).equals(id)){
                            LastUpdate[curIndex] = lastUpdate;
                            coordinates[curIndex][0] = latitude;
                            coordinates[curIndex][1] = longitude;

                            curIndex++;
                        }
                    }
                    catch (JSONException jsonException){
                        continue;
                    }
                }
            }

            sort();

            //Results
            String[] home = getMostFrequent();
            String firstDate = LastUpdate[0];
            int outdoor = calculateOutdoorTime(home);

            System.out.println("Home: " + home[0] + " " + home[1]);
            System.out.println("First Date: " + firstDate);
            System.out.println("Time Outdoors (min): " + outdoor);
            System.out.println("Time Outdoors (hrs): " + outdoor/60);

            curIndex = 0;
            LastUpdate = new String[MAX_ENTRIES];
            coordinates = new String[MAX_ENTRIES][2];
        }

        //System.out.println(df2.format(calculateDistance(Double.parseDouble(coordinates[0][0]), Double.parseDouble(coordinates[0][1]), Double.parseDouble(coordinates[1][0]), Double.parseDouble(coordinates[1][1]))));

    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static void writeJsonToFile(JSONObject json, String file) throws Exception {
        Files.write(Paths.get(file), json.toString().getBytes());
    }

    public static JSONObject readJsonFromFile(String file) throws Exception {
        String jsonText = new String(Files.readAllBytes(Paths.get(file)));
        JSONObject json = new JSONObject(jsonText);
        return json;
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    public static void sort() {
        int n = curIndex;
        for (int i = 1; i < n; ++i) {
            long key = Long.parseLong(LastUpdate[i]);
            String keyLat = coordinates[i][0];
            String keyLong = coordinates[i][1];
            int j = i - 1;

            while (j >= 0 && Long.parseLong(LastUpdate[j]) > key) {
                LastUpdate[j + 1] = LastUpdate[j];
                coordinates[j + 1][0] = coordinates[j][0];
                coordinates[j + 1][1] = coordinates[j][1];
                j = j - 1;
            }
            LastUpdate[j + 1] = key + "";
            coordinates[j + 1][0] = keyLat;
            coordinates[j + 1][1] = keyLong;
        }
    }

    public static String[] getMostFrequent() {
        int count = 1, tempCount;
        String[] popular = coordinates[0];
        String[] temp;
        for (int i = 0; i < curIndex - 1; i++)
        {
            temp = coordinates[i];
            tempCount = 0;
            for (int j = 1; j < curIndex; j++)
            {
                double lat1, lon1, lat2, lon2;
                lat1 = Double.parseDouble(temp[0]);
                lon1 = Double.parseDouble(temp[1]);
                lat2 = Double.parseDouble(coordinates[j][0]);
                lon2 = Double.parseDouble(coordinates[j][1]);
                if (calculateDistance(lat1,lon1,lat2,lon2) < MAX_DISTANCE)  // temp == coordinates[j]
                    tempCount++;
            }
            if (tempCount > count)
            {
                popular = temp;
                count = tempCount;
            }
        }
        return popular;
    }

    public static double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2){
        double R, phi1, phi2, deltaphi, deltalambda, a, c, d;

        R = 6371e3; // metres
        phi1 = lat1 * Math.PI/180; // φ, λ in radians
        phi2 = lat2 * Math.PI/180;
        deltaphi = (lat2-lat1) * Math.PI/180;
        deltalambda = (lon2-lon1) * Math.PI/180;

        a = Math.sin(deltaphi/2) * Math.sin(deltaphi/2) +
                 Math.cos(phi1) * Math.cos(phi2) *
                         Math.sin(deltalambda/2) * Math.sin(deltalambda/2);
        c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        d = R * c;

        return d;
    }

    public static int calculateOutdoorTime(String[] home){
        int outdoor = 0;
        boolean OUT = false;
        double lat1, lon1, lat2, lon2;
        lat1 = Double.parseDouble(home[0]);
        lon1 = Double.parseDouble(home[1]);

        for (int i = 0; i < curIndex; i++) {
            lat2 = Double.parseDouble(coordinates[i][0]);
            lon2 = Double.parseDouble(coordinates[i][1]);
            if (OUT){
                outdoor += 10;
            }
            if (calculateDistance(lat1,lon1,lat2,lon2) > MAX_DISTANCE)
                OUT = true;
            else{
                OUT = false;
            }

            //System.out.print(LastUpdate[i]);
            //System.out.print(" " + coordinates[i][0] + " " + coordinates[i][1]);
            //System.out.println();
        }
        if(OUT){
            outdoor+=10;
        }
        return outdoor;
    }
}
