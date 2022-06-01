package classes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetrieveTickets {

private RetrieveTickets(){
    //Empty
}


   private static String readAll(Reader rd) throws IOException {
	      StringBuilder sb = new StringBuilder();
	      int cp;
	      while ((cp = rd.read()) != -1) {
	         sb.append((char) cp);
	      }
	      return sb.toString();
	   }

   public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
      try (InputStream is = new URL(url).openStream()) {
         BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
         String jsonText = readAll(rd);
         return new JSONObject(jsonText);
      }
   }


  
   public static void retrieveTickets(String projName) throws IOException, JSONException {
		   

	   int j;
	   int i = 0;
       int total;
      //Get JSON API for closed bugs w/ AV in the project
      ArrayList<String> keyArray = new ArrayList<>();
      ArrayList<String> resArray = new ArrayList<>();
      ArrayList<String> createArray = new ArrayList<>();
      do {
         //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
         j = i + 1000;
         String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                + projName + "%22AND(%22status%22=%22closed%22OR"
                + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,affectedVersion,created&startAt="
                + i + "&maxResults=" + j;
         JSONObject json = readJsonFromUrl(url);
         JSONArray issues = json.getJSONArray("issues");

         total = json.getInt("total");
         for (; i < total && i < j; i++) {
            //Iterate through each bug
            String key = issues.getJSONObject(i%1000).get("key").toString();
            keyArray.add(key);
            String date = issues.getJSONObject(i%1000).getJSONObject("fields").get("resolutiondate").toString().substring(0,7);
            resArray.add(date);
            String create = issues.getJSONObject(i%1000).getJSONObject("fields").get("created").toString().substring(0,7);
            createArray.add(create);
         }  
      } while (i < total);
      //Iterate on date to calculate # of fixed tickets per month
      int iter = 0;
      while(iter < keyArray.size()){
         Logger.getGlobal().log(Level.INFO, keyArray.get(iter));
         Logger.getGlobal().log(Level.INFO, createArray.get(iter));
         iter++;
      }
      //FIleWriter

        String file = Paths.get("").toAbsolutePath().toString() +"\\Reports\\" +projName + ".csv ";
       try(FileWriter fileWriter = new FileWriter(file)) { //Open file
           fileWriter.append("Month,NumberOfFixes");
           fileWriter.append("\n");
        //For each month calculate number of fixes
        for (LocalDate date = LocalDate.of(2004, 4, 1); date.isBefore(LocalDate.now()); date = date.plusMonths(1) ) {
            fileWriter.append(date.toString().substring(0, 7));
            fileWriter.append(",");
            int numOfFix = Collections.frequency(resArray, date.toString().substring(0, 7));
            Logger.getGlobal().log(Level.INFO, "Fixes in {0} : {1}", new Object[]{date.toString().substring(0, 7), numOfFix});
            fileWriter.append(String.valueOf(numOfFix));
            fileWriter.append("\n");
            }
            //Exception catching
          } catch (Exception e) {
           Logger.getGlobal().log(Level.WARNING, "Error in csv writer");
           e.printStackTrace();
       }
      } //deliberable 1 fare grafico

   }
