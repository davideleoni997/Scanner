package classes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GetReleaseInfo {

	private GetReleaseInfo(){
		//Empty
	}

	   protected static Map<LocalDateTime, String> releaseNames;
	   protected static Map<LocalDateTime, String> releaseID;
	   protected static ArrayList<LocalDateTime> releases;
	   protected static Integer numVersions;

	public static int getRelease(String projName) throws IOException, JSONException {
		   

		 //Fills the arraylist with releases dates and orders them
		   //Ignores releases with missing dates
		   releases = new ArrayList<>();
		         int i;
		         String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
		         JSONObject json = readJsonFromUrl(url);
		         JSONArray versions = json.getJSONArray("versions");
		         releaseNames = new HashMap<>();
		         releaseID = new HashMap<> ();
		         for (i = 0; i < versions.length(); i++ ) {

		            String name = "";
		            String id = "";
		            if(versions.getJSONObject(i).has("releaseDate")) {
		               if (versions.getJSONObject(i).has("name"))
		                  name = versions.getJSONObject(i).get("name").toString();
		               if (versions.getJSONObject(i).has("id"))
		                  id = versions.getJSONObject(i).get("id").toString();
		               addRelease(versions.getJSONObject(i).get("releaseDate").toString(),
		                          name,id);
		            }
		         }
		         // order releases by date
		//@Override
		releases.sort(LocalDateTime::compareTo);

		         if (releases.size() < 2) {
					 return 1;
				 }
		String file = Paths.get("").toAbsolutePath().toString() +"\\Reports\\"+ projName + "VersionInfo.csv";

		try(FileWriter fileWriter = new FileWriter(file)) {
				    fileWriter.append("Index,Version ID,Version Name,Date,SHA");
		            fileWriter.append("\n");

		            numVersions = releases.size();
		            for ( i = 0; i < releases.size(); i++) {
		               int index = i + 1;
		               fileWriter.append(String.valueOf(index));
		               fileWriter.append(",");
		               fileWriter.append(releaseID.get(releases.get(i)));
		               fileWriter.append(",");
		               fileWriter.append(releaseNames.get(releases.get(i)));
		               fileWriter.append(",");
		               fileWriter.append(releases.get(i).toString());
		               fileWriter.append(",");
		               fileWriter.append(getSHA(releases.get(i).toString(),projName));
		               fileWriter.append("\n");
		            }

		         } catch (Exception e) {
		            Logger.getGlobal().log(Level.INFO,"Error in csv writer");
		            e.printStackTrace();
		         }

		return numVersions;
		   }

	private static String getSHA(String s,String projName) throws IOException {
		ProcessBuilder builder;

		builder = new ProcessBuilder(
				"cmd.exe", "/c", "cd \""+ Paths.get("").toAbsolutePath().toString() +"\\"+projName+"\\\" && git log --before=" + LocalDate.parse(s,DateTimeFormatter.ISO_LOCAL_DATE_TIME));


		builder.redirectErrorStream(true);
		Process p = builder.start();
		BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String gitLine = r.readLine();
		try(Scanner scan = new Scanner(gitLine)) {
			scan.next();
			return scan.next();
		}
		catch (Exception e){
			Logger.getGlobal().log(Level.WARNING,"Error in the SHA scanner");
			return "";
		}

	}


	public static void addRelease(String strDate, String name, String id) {
		      LocalDate date = LocalDate.parse(strDate);
		      LocalDateTime dateTime = date.atStartOfDay();
		      if (!releases.contains(dateTime))
		         releases.add(dateTime);
		      releaseNames.put(dateTime, name);
		      releaseID.put(dateTime, id);
		   }


	   public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		   try (InputStream is = new URL(url).openStream(); BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

			   String jsonText = readAll(rd);
			   return new JSONObject(jsonText);
		   }
	   }
	   
	   private static String readAll(Reader rd) throws IOException {
		      StringBuilder sb = new StringBuilder();
		      int cp;
		      while ((cp = rd.read()) != -1) {
		         sb.append((char) cp);
		      }
		      return sb.toString();
		   }

	
}