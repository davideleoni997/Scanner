package classes;


import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args){
        String projName = "AVRO";
       try{
          VersionChange.cloneMaster(projName);
          int numvers = GetReleaseInfo.getRelease(projName);
          RetrieveTickets.retrieveTickets(projName);
          ListOfBugTickets.createBugList(projName);
          CreateDataSet.createAll(projName,numvers);


          String original = Paths.get("").toAbsolutePath().toString() + "\\Reports\\" + projName + "DataSetBuggy.csv";
          String dest = Paths.get("").toAbsolutePath().toString()+ "\\Reports\\" + projName +"Weka.arff";
          ArffConverter.convert(original,dest,projName);

       }
       catch(Exception e){
           Logger.getGlobal().log(Level.INFO,e.getMessage());
           Thread.currentThread().interrupt();
       }

    }
}
