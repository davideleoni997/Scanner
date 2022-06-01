package classes;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ListOfBugTickets {

    public static final String FIELDS = "fields";

    private ListOfBugTickets(){
        //Empty
    }

    public static void createBugList(String projName){
       String file = Paths.get("").toAbsolutePath().toString() + "\\Reports\\" + projName + "BugList.csv";

        try(FileWriter listWriter = new FileWriter(file)) {
            int j;
            int i = 0;
            int total;
            //Get JSON API for closed bugs w/ AV in the project
            do {
                //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
                j = i + 1000;
                String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                        + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                        + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,affectedVersions,fixVersions,created&startAt="
                        + i + "&maxResults=" + j;
                JSONObject json = RetrieveTickets.readJsonFromUrl(url);
                JSONArray issues = json.getJSONArray("issues");
                total = json.getInt("total");
                for (; i < total && i < j; i++) {
                    //Iterate through each bug

                    String key = issues.getJSONObject(i % 1000).get("key").toString();
                    listWriter.append(key).append(",");
                    //append date of ticket
                    String data = issues.getJSONObject(i%1000).getJSONObject(FIELDS).get("created").toString();
                    data = data.substring(0,10);
                    listWriter.append(data).append(",");

                    appendVersions(listWriter, i, issues, projName,data);

                    if(i+1 != total)
                        listWriter.append("\n");
                }
            } while (i < total);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private static void appendVersions(FileWriter listWriter, int i, JSONArray issues, String projName, String data) throws IOException {

        String file = Paths.get("").toAbsolutePath().toString() + "\\Reports\\" + projName + "VersionInfo.csv";


            //affected version

            String version;
            JSONArray versions = issues.getJSONObject(i % 1000).getJSONObject(FIELDS).getJSONArray("versions");
            if (!versions.isEmpty()) {
                listWriter.append("[");
                for (int z = 0; z < versions.length(); z++) {

                    version = getVersion(file, versions, z);
                    listWriter.append(version);
                        if ((z + 1) != versions.length())
                            listWriter.append(";");

                }
                listWriter.append("]");
            }
            //Fixed Version
            listWriter.append(",");
            String fixversion;
            JSONArray fixversions = issues.getJSONObject(i % 1000).getJSONObject(FIELDS).getJSONArray("fixVersions");
            if (!fixversions.isEmpty()) {
                fixversion = getFixversion(file, fixversions);
                listWriter.append(fixversion);
            }
            listWriter.append(",");
            try(BufferedReader br = new BufferedReader(new FileReader(file))){

                String line = br.readLine();
                Logger.getGlobal().log(Level.INFO,line);
            while((line=br.readLine())!= null){
                if(line.split(",")[3].compareTo(data)>0) {
                    listWriter.append(line.split(",")[0]);
                    break;
                }
            }
            if(line==null)
                listWriter.append("0");

            }catch(IOException e ){
                Logger.getGlobal().log(Level.INFO,e.toString());
            }
        }

    private static String getVersion(String file, JSONArray versions, int z) throws IOException {
        String version;
        version = versions.getJSONObject(z).get("name").toString();

        version = compareVersion(file, version);
        return version;
    }

    private static String getFixversion(String file, JSONArray fixversions) throws IOException {
        String fixversion;
        fixversion = fixversions.getJSONObject(0).get("name").toString();
        fixversion = compareVersion(file, fixversion);
        return fixversion;
    }

    private static String compareVersion(String file, String fixversion) throws IOException {
        String fixedversion = "";
        try (BufferedReader versionReader = new BufferedReader(new FileReader(file))) {
            String line = versionReader.readLine();
            Logger.getGlobal().log(Level.INFO,line);
            while ((line = versionReader.readLine()) != null){
                fixedversion = getVersion(fixversion, line);
                if (!fixedversion.equals("")) break;
            }
        }
        return fixedversion;
    }

    private static String getVersion(String fixversion, String line) {
        String[] linevers = line.split(",")[2].split("\\.");
        String[] fixversionsep = fixversion.split("\\.");
        boolean out = false;
        if(Integer.parseInt(linevers[0])==Integer.parseInt(fixversionsep[0])) {
            if(Integer.parseInt(linevers[1])==Integer.parseInt(fixversionsep[1])){
                if(Integer.parseInt(linevers[2])>=Integer.parseInt(fixversionsep[2])) {
                    fixversion = line.split(",")[0];
                    out = true;
                }
                }
            else{
                if(Integer.parseInt(linevers[1])>Integer.parseInt(fixversionsep[1])){
                    fixversion = line.split(",")[0];
                    out = true;
                }
            }
        }
        else
            if(Integer.parseInt(linevers[0])>Integer.parseInt(fixversionsep[0])){
                fixversion = line.split(",")[0];
                out = true;
            }
        if(out)
            return fixversion;
        return "";
    }

}
