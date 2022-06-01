package classes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArffConverter {

    private ArffConverter(){
        //empty
    }
    public static void convert(String filename, String newFile, String projName){
        try (BufferedReader csv = new BufferedReader(new FileReader(filename)); FileWriter arff = new FileWriter(newFile)){
            String line;
            line = csv.readLine();
            Logger.getGlobal().log(Level.INFO,line);

            arff.write("@RELATION " + projName + "\n" +
                    "@ATTRIBUTE Version Numeric \n" +
                    "@ATTRIBUTE FileName String \n" +
                    "@ATTRIBUTE Size Numeric \n" +
                    "@ATTRIBUTE NR Numeric \n" +
                    "@ATTRIBUTE LOC_TOUCH Numeric \n" +
                    "@ATTRIBUTE LOC_ADD Numeric \n" +
                    "@ATTRIBUTE MAX_LOC_ADD Numeric \n" +
                    "@ATTRIBUTE Churn Numeric \n" +
                    "@ATTRIBUTE MAX_Churn Numeric \n" +
                    "@ATTRIBUTE AVG_LOC_ADDED Numeric \n" +
                    "@ATTRIBUTE AVG_CHURN Numeric \n" +
                    "@ATTRIBUTE Buggy {0,1} \n\n" +
                    "@DATA \n");

           while((line = csv.readLine())!=null){
                String[] splitted = line.split(",");
                for(int i =0;i<splitted.length-1;i++) {
                    arff.append(splitted[i]).append(",");
                }
                    arff.append(splitted[splitted.length-1]).append("\n");
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
