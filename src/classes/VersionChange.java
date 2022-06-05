package classes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VersionChange {

    private VersionChange(){
        //Empty
    }

    public static void cloneMaster(String projName) throws IOException, InterruptedException {
        ProcessBuilder builder;

            builder = new ProcessBuilder(
                    "cmd.exe", "/c", "git clone https://github.com/apache/" + projName);

        Process proc = builder.start();
        proc.waitFor();
    }


    public static void checkVers(int version,String projName) {

        String file = Paths.get("").toAbsolutePath().toString() + "\\Reports\\" + projName+ "VersionInfo.csv";
        try (BufferedReader bufRead = new BufferedReader(new FileReader(file)))
        {
            String line;
            line = bufRead.readLine();
            for(int i=0; i<version; i++)
                line = bufRead.readLine();

            assert line != null;
            String[] splitted = line.split(",");
            ProcessBuilder builder;

            builder = new ProcessBuilder(
                    "cmd.exe", "/c", "cd \""+ Paths.get("").toAbsolutePath().toString() +"\\"+ projName +"\\\" && git checkout " + splitted[4]);

            builder.redirectErrorStream(true);
            Process proc = builder.start();
            proc.waitFor();
        }
        catch (Exception e){
            Logger.getGlobal().log(Level.INFO,e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
