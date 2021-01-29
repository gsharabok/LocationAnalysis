/**
 * The program takes url of a JSON file and distance as input, and outputs a CSV file containing
 * the approximate home location of a user, first entry date, UID, time spent outside of home.
 * All the downloaded data is stored in the data folder inside the current directory for quick use.
 * If the folder is empty, the data will be automatically downloaded, otherwise the data inside the folder is used.
 * The data can also be stored directly into the data folder in JSON format other than using a link.
 *
 * MAX_DISTANCE - Distance that will be used to measure home location in meters
 * URL - link from which the user data will be extracted
 */

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    private static final double MAX_DISTANCE = 30;
    private static final String URL = "";

    private static ArrayList<String> LastUpdate = new ArrayList<>();
    private static ArrayList<ArrayList<String>> coordinates = new ArrayList<>();
    private static ArrayList<String> uid = new ArrayList<>();

    private static FileWriter csvWriter;

    private static int curIndex = 0;

    public static void main(String[] args) throws IOException, JSONException, Exception {
        String id, lastUpdate, latitude, longitude;
        String KEY = "NA";
        prepCSV();

        Path dataDir = new File("data").toPath().toAbsolutePath();
        if(isEmptyDir(dataDir)) {
            ArrayList<JSONObject> jsonObjects = new ArrayList<JSONObject>();
            int total = 0;
            try {
                while (true) {
                    JSONObject temp = readJsonFromUrl(URL + KEY);
                    jsonObjects.add(temp);
                    KEY = temp.getJSONObject("LastEvaluatedKey").getJSONObject("id").getString("S");
                    System.out.println(KEY);
                    total++;
                }
            } catch (JSONException jsonException) {
                total *= 1000;
                System.out.println("FINISHED with " + total + " items");
            }

            int j = 0;
            for (JSONObject jsonObject : jsonObjects) {
                writeJsonToFile(jsonObject, "data\\data" + j + ".json");
                j++;
            }
        }

        int numFiles = getFileCount("data");

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
                            LastUpdate.add(curIndex,lastUpdate);
                            coordinates.add(curIndex, new ArrayList<>(Arrays.asList(latitude,longitude)));

                            curIndex++;
                        }
                    }
                    catch (JSONException jsonException){
                        continue;
                    }
                }
            }

            sort();

            ArrayList<String> home = getMostFrequent();
            String firstDate = LastUpdate.get(0);
            int outdoor = calculateOutdoorTime(home);

            System.out.println("UID: " + uid.get(k));
            System.out.println("Home: " + home.get(0) + " " + home.get(1));
            System.out.println("First Date: " + firstDate);
            System.out.println("Time Outdoors (min): " + outdoor);
            System.out.println();

            curIndex = 0;
            LastUpdate = new ArrayList<>();
            coordinates = new ArrayList<>();

            saveDataToCSV(k, firstDate, home, outdoor);
        }
        csvWriter.flush();
        csvWriter.close();
    }

    /**
     * Gets the data into String format
     * @param rd
     * @return String
     */
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /**
     * Writes a JSONObject to a file with the name provided
     * @param json
     * @param file
     */
    public static void writeJsonToFile(JSONObject json, String file) throws Exception {
        Files.write(Paths.get(file), json.toString().getBytes());
    }

    /**
     * Reads data from a JSON file into a JSONObject
     * @param file
     * @return JSONObject
     */
    public static JSONObject readJsonFromFile(String file) throws Exception {
        String jsonText = new String(Files.readAllBytes(Paths.get(file)));
        return new JSONObject(jsonText);
    }

    /**
     * Reads JSON data from a URL (tested on AWS database links) and returns a JSONObject with the data
     * @param url
     * @return JSONObject
     */
    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        } finally {
            is.close();
        }
    }

    /**
     * Sorts the records based on the modification date (from oldest to newest)
     */
    public static void sort() {
        int n = curIndex;
        for (int i = 1; i < n; ++i) {
            long key = Long.parseLong(LastUpdate.get(i));
            String keyLat = coordinates.get(i).get(0);
            String keyLong = coordinates.get(i).get(1);
            int j = i - 1;

            while (j >= 0 && Long.parseLong(LastUpdate.get(j)) > key) {
                LastUpdate.set(j + 1, LastUpdate.get(j));
                coordinates.set(j + 1, new ArrayList<>(Arrays.asList(coordinates.get(j).get(0),coordinates.get(j).get(1))));
                j = j - 1;
            }
            LastUpdate.set(j + 1, key + "");
            coordinates.set(j+1, new ArrayList<>(Arrays.asList(keyLat,keyLong)));
        }
    }

    /**
     * Finds the most frequently repeating location for a given user, the result will be used for finding home location
     * @return ArrayList of size 2 with Latitude and Longitude
     */
    public static ArrayList<String> getMostFrequent() {
        int count = 1, tempCount;
        ArrayList<String> popular = coordinates.get(0);
        ArrayList<String> temp;
        for (int i = 0; i < curIndex - 1; i++)
        {
            temp = coordinates.get(i);
            tempCount = 0;
            for (int j = 1; j < curIndex; j++)
            {
                double lat1, lon1, lat2, lon2;
                lat1 = Double.parseDouble(temp.get(0));
                lon1 = Double.parseDouble(temp.get(1));
                lat2 = Double.parseDouble(coordinates.get(j).get(0));
                lon2 = Double.parseDouble(coordinates.get(j).get(1));
                if (calculateDistance(lat1,lon1,lat2,lon2) < MAX_DISTANCE)
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

    /**
     * Calculates distance in meters between two Latitude and Longitude coordinated
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return double Distance in meters
     */
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

    /**
     * Calculates time spent outdoors by comparing home location to each of the records
     * The time is estimated in 10 minute periods, can be modified to use actual record data
     * @param home
     * @return int Time
     */
    public static int calculateOutdoorTime(ArrayList<String> home){
        int outdoor = 0;
        boolean OUT = false;
        double lat1, lon1, lat2, lon2;
        lat1 = Double.parseDouble(home.get(0));
        lon1 = Double.parseDouble(home.get(1));

        for (int i = 0; i < curIndex; i++) {
            lat2 = Double.parseDouble(coordinates.get(i).get(0));
            lon2 = Double.parseDouble(coordinates.get(i).get(1));
            if (OUT){
                outdoor += 10;
            }
            OUT = calculateDistance(lat1, lon1, lat2, lon2) > MAX_DISTANCE;
        }
        if(OUT){
            outdoor+=10;
        }
        return outdoor;
    }

    /**
     * Checks if a given directory is empty
     * @param path
     * @return true if empty, false if contains files
     */
    public static boolean isEmptyDir(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
                return !directory.iterator().hasNext();
            }
        }

        return false;
    }

    /**
     * Finds the number of files in a given directory
     * @param path
     * @return int Number of Files
     */
    public static int getFileCount(String path){
        File directory = new File(path);
        int fileCount = 0;
        try{
            fileCount = directory.list().length;
        } catch (NullPointerException e){}

        return fileCount;
    }

    /**
     * Opens the file for results and appends the titles for each column
     */
    public static void prepCSV() throws IOException{
        csvWriter = new FileWriter("results.csv");
        csvWriter.append("UID");
        csvWriter.append(",");
        csvWriter.append("First Date");
        csvWriter.append(",");
        csvWriter.append("Home Latitude");
        csvWriter.append(",");
        csvWriter.append("Home Longitude");
        csvWriter.append(",");
        csvWriter.append("Minutes Outdoors");
        csvWriter.append("\n");
    }

    /**
     * Adds a record of a single user to the final file
     * @param i  - index of a user in the list
     * @param firstDate - first record date
     * @param home - home location in latitude and longitude
     * @param outdoor - time spent outdoors
     */
    public static void saveDataToCSV(int i, String firstDate, ArrayList<String> home, int outdoor) throws IOException{
        ArrayList<String> row = new ArrayList<>(Arrays.asList(
                uid.get(i),
                firstDate,
                home.get(0),
                home.get(1),
                outdoor + ""
        ));
        csvWriter.append(String.join(",", row));
        csvWriter.append("\n");
    }
}