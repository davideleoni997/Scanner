package classes;


import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class CreateDataSet {
    public static final String CMD_EXE = "cmd.exe";
    public static final String CD = "cd \"";
    public static final String REPORTS = "\\Reports\\";
    public static final String FILENAME = "-- \"";
    private static int actualVersion;

    private CreateDataSet(){
        //Empty
    }

    public static void createSet(int version,String projName) throws IOException {
        actualVersion = version;

        VersionChange.checkVers(version,projName);

        String dataFile = Paths.get("").toAbsolutePath().toString() + REPORTS + projName + "DataSet.csv ";
        try{
            if(actualVersion == 1) {
                try(FileWriter fw = new FileWriter(dataFile)){
                    fw.append("Version,File Name,Size,NR,LOC_touched,LOC_added,MAX_LOC_added,Churn,MAX_Churn,AVG_LOC_added,AVG_Churn,Buggy");
                    fw.append("\n");
                }

            }
        } catch(Exception e){
            e.printStackTrace();
        }
        String projPath = Paths.get("").toAbsolutePath().toString() + File.separator + projName + File.separator;
        try (Stream<Path> paths = Files.walk(Paths.get(projPath)); FileWriter finalFw = new FileWriter(dataFile,true)){ //

            paths
                    .filter(Files::isRegularFile)
                    .filter(f-> f.toString().endsWith(".java"))
                    .forEach(line ->{
                        try {
                            finalFw.append(String.valueOf(version));
                            finalFw.append(",");
                            finalFw.append(line.toString());
                            finalFw.append(",");
                            //Calculate LOC
                            String loc = calculateLOC(line);
                            finalFw.append(loc);
                            finalFw.append(",");
                            //Usa git log per ogni riga per vedere alcune metriche
                            String[] ghm = gitHubMetrics(line,projName);
                            for (String s : ghm) {
                                finalFw.append(s);
                                finalFw.append(",");
                            }
                            finalFw.append("0");
                            finalFw.append("\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                                }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static String calculateLOC(Path line) {
        int count = 0;
        try(Scanner scan = new Scanner(new File(line.toString()))){

            while(scan.hasNextLine()){
                scan.nextLine();
                count++;
            }
        }
        catch(Exception e){
            Logger.getGlobal().log(Level.WARNING,e.toString());
        }
        return String.valueOf(count);
    }

    private static String[] gitHubMetrics(Path line,String projName) throws IOException {
        BufferedReader r = getGitLogBuffer(line,projName);

        String gitLine;

        int numberRevisions = 0;
        int touched =0;
        int added =0;
        int maxAdded =0;
        int churn = 0;
        int maxChurn =0;

        while ((gitLine = r.readLine())!=null) {

            if (gitLine.contains("file changed")) {

                //Each commit is a revision

                numberRevisions++;
                int currentChurn = 0;
                //count LOC_TOUCHED = added+deleted
                try(Scanner scan = new Scanner(gitLine)) {
                    scan.nextInt();
                    scan.next();
                    scan.next();
                    int actualScan = scan.nextInt();
                    String temp = scan.next();
                    //LOC_ADDED
                    if (temp.contains("insertion")) {
                        added += actualScan;
                        churn += added;
                        currentChurn += added;
                        //MAX_LOC_added
                        if (added > maxAdded)
                            maxAdded = added;
                    } else if (temp.contains("deletion")) {
                        churn -= actualScan;
                        currentChurn -= actualScan;
                    }
                    touched += actualScan;
                    //Confronto max current churn
                    if (currentChurn > maxChurn)
                        maxChurn = currentChurn;
                }


            }
        }

        float avgAdded = 0;
        float avgChurn = 0;
        if(numberRevisions != 0) {
             avgAdded = (float) added / numberRevisions;
             avgChurn = (float) churn / numberRevisions;
        }

        //Parsare le linee

        String[] ret =  new String[8];
        ret[0] = Integer.toString(numberRevisions);
        ret[1] = Integer.toString(touched);
        ret[2] = Integer.toString(added);
        ret[3] = Integer.toString(maxAdded);
        ret[4] = Integer.toString(churn);
        ret[5] = Integer.toString(maxChurn);
        ret[6] = Float.toString(avgAdded);
        ret[7] = Float.toString(avgChurn);
        return ret;
    }

    private static BufferedReader getGitLogBuffer(Path line,String projName) throws IOException {
        //spawn CMD
        // "git log --stat --since="+ dataPart + " --before="+ dataArr + " --" + line
        String file = Paths.get("").toAbsolutePath().toString()+ REPORTS + projName + "VersionInfo.csv";
        String lastDate = null;
        String actualDate = null;
        try(BufferedReader br = new BufferedReader(new FileReader(file)) ) {
            lastDate = br.readLine();
            for (int i = 0; i < actualVersion; i++) {
                lastDate = actualDate;
                String[] splitted = br.readLine().split(",");
                actualDate = splitted[3];
            }
        }
        catch (Exception e){
            Logger.getGlobal().log(Level.WARNING,"Error in versionInfo read");
        }

        LocalDate dataPart = LocalDate.of(1999,12,31);
        if(actualVersion!=1){
            assert lastDate != null;
            dataPart = LocalDate.parse(lastDate,DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        assert actualDate != null;
        LocalDate dataArr = LocalDate.parse(actualDate,DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        ProcessBuilder builder;

        if(actualVersion==1) {
            builder = new ProcessBuilder( //data Part = data release, dataArr = data next release
                    CMD_EXE, "/c", CD + Paths.get("").toAbsolutePath().toString() +"\\"+ projName +"\\\" && git log --stat --before=" + dataArr.format(DateTimeFormatter.ISO_LOCAL_DATE) + FILENAME + line.toString() + "\"");
        }
        else
        {
            builder = new ProcessBuilder( //data Part = data release, dataArr = data next release
                    CMD_EXE, "/c", CD + Paths.get("").toAbsolutePath().toString() +"\\"+projName +"\\\" && git log --stat --before=" + dataArr.format(DateTimeFormatter.ISO_LOCAL_DATE) + "--after="+ dataPart.format(DateTimeFormatter.ISO_LOCAL_DATE) + FILENAME + line.toString() + "\"");

        }
        builder.redirectErrorStream(true);
        Process p = builder.start();
        return new BufferedReader(new InputStreamReader(p.getInputStream()));
    }

    public static void createAll(String projName, int numvers) throws IOException {

        int totalvers = (int) Math.ceil((float) numvers / 2);
        Logger.getGlobal().log(Level.INFO,"NumOfVersions: {0}",String.valueOf(totalvers));

        for(int i = 1; i< totalvers +1 ; i++)
            createSet(i,projName);

        VersionChange.checkVers(numvers,projName);
        //proportion
        proportion(projName);
        buggyness(projName);
    }

    private static void proportion(String projName) {
        String dataFile = Paths.get("").toAbsolutePath().toString() + REPORTS +projName+"BugList.csv";
        String prop = Paths.get("").toAbsolutePath().toString() + REPORTS +projName+"Proportion.csv";
        try(CSVReader reader = new CSVReaderBuilder(new FileReader(dataFile)).withSkipLines(1).build();
            BufferedWriter bw = new BufferedWriter(new FileWriter(prop))
            ){
            calculateProportion(reader, bw);
        }catch(CsvException | IOException e){
            Logger.getGlobal().log(Level.INFO,e.toString());
        }
    }

    private static void calculateProportion(CSVReader reader, BufferedWriter bw) throws IOException, CsvException {
        bw.write("Version, ProportionValue\n");
        bw.append("1,1\n");
        bw.append("2,1\n");
        List<String[]> all = reader.readAll();
        all.sort(Comparator.comparing(o -> Integer.parseInt(o[4])));
        int total=0;
        int bugs=0;
        int actual=3;
        for (String[] line:all
             ) {
            boolean invalid = false;
            for(int i=2;i<4;i++){
                if (line[i].equals("")) {
                    invalid = true;
                    break;
                }
            }


            if(invalid || (Integer.parseInt(line[4])<=2))
                continue;
            String iv = line[2].split(";")[0];
            iv = iv.split("]")[0].substring(1);
            if(!line[4].equals(iv)) {
                bugs++;
                total = total + ((Integer.parseInt(line[3]) - Integer.parseInt(iv)) / (Integer.parseInt(line[4]) - Integer.parseInt(iv)));
            }
            else{
                bugs++;
            }
            actual = writeToFIle(bw, total, bugs, actual, line);
        }
        if(bugs!=0)
            bw.append(String.valueOf(actual)).append(",").append(Double.toString ((double)total/bugs));
    }

    private static int writeToFIle(BufferedWriter bw, int total, int bugs, int actual, String[] line) throws IOException {
        if(Integer.parseInt(line[4])!= actual){
            if(bugs !=0)
                bw.append(String.valueOf(actual)).append(",").append(Double.toString ((double)total/bugs)).append("\n");
            actual = Integer.parseInt(line[4]);
        }
        return actual;
    }

    public static void buggyness(String projName) throws IOException {
        String dataFile = Paths.get("").toAbsolutePath().toString() + REPORTS +projName+"DataSet.csv";
        String newData = Paths.get("").toAbsolutePath().toString()+ REPORTS +projName+"DataSetBuggy.csv";
        try(BufferedReader file = new BufferedReader(new FileReader(dataFile));  FileWriter buggyWrite = new FileWriter(newData)) {

            String actual = file.readLine();
            buggyWrite.write(actual + "\n");

            while (actual != null) {
                String filename;
                String[] splitted;
                actual = file.readLine();
                if (actual == null)
                    break;
                splitted = actual.split(",");
                filename = splitted[1];
                for (int i = 0; i < 11; i++) {
                    buggyWrite.append(splitted[i]).append(",");
                }
                buggyWrite.append(isBuggy(filename,projName,splitted[0])).append("\n");
            }
        }

    }

    private static char isBuggy(String filename, String projName, String version) {
        String file = Paths.get("").toAbsolutePath().toString() + REPORTS +projName+"BugList.csv";
        String prop = Paths.get("").toAbsolutePath().toString() + REPORTS +projName+"Proportion.csv";
        try(CSVReader reader = new CSVReaderBuilder(new FileReader(prop)).withSkipLines(1).build()) {
            List<String[]> all = reader.readAll();
            ProcessBuilder builder;

            builder = new ProcessBuilder(
                    CMD_EXE, "/c", CD + Paths.get("").toAbsolutePath().toString() + "\\" + projName + "\\\" && git log " + FILENAME + filename + "\" ");

            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String gitLine;
            while ((gitLine = r.readLine()) != null) {
                if (gitLine.contains(projName + "-") || gitLine.contains("Issue #")) {

                    Character x = getBuggy(version, file, gitLine, all,projName);
                    if (x != null) return x;
                }
            }
            r.close();
            return '0';
        }catch(Exception e) {
            Logger.getGlobal().log(Level.INFO,e.toString());
            return '0';
        }
    }

    private static Character getBuggy(String version, String file, String gitLine, List<String[]> all, String projName){
        String list;

        gitLine = gitLine.replace('#', ' ');
        gitLine = gitLine.replace(':', ' ');
        gitLine = gitLine.replace('-', ' ');
        gitLine = gitLine.replace('.', ' ');
        gitLine = gitLine.replace(',',' ');
        gitLine = gitLine.replace('>', ' ');

        try(BufferedReader br = new BufferedReader(new FileReader(file));Scanner scan = new Scanner(gitLine)) {
                String number = scan.next();
                Logger.getGlobal().log(Level.INFO,number);

                number = String.valueOf(scan.nextInt());

                while ((list = br.readLine()) != null ) {
                    Character x = evaluateVersionsBuggyness(version, list, all, number, projName);
                    if (x != null) return x;
                }

                }catch(InputMismatchException e){
                return null;
                }
                catch(IOException e){
                 Logger.getGlobal().log(Level.INFO,e.toString());
                  }
        return null;
    }

    private static Character evaluateVersionsBuggyness(String version, String list, List<String[]> all, String number, String projName) {
        String[] versions= list.split(",",-1);

        if(versions.length >=4 && !versions[2].equals("") && !versions[3].equals("")){


            if (versions[0].contains(number)  && (Integer.parseInt(version)<Integer.parseInt(versions[3]))){

                    return '1';}
            }
             else{
                //usa proportion e calcola IV se IV < attuale ritorna 1
                if(!projName.equalsIgnoreCase("AVRO") && versions.length>=5 && !versions[3].equals("") && Integer.parseInt(versions[3])>=Integer.parseInt(versions[4])){
                    int fv = Integer.parseInt(versions[3]);
                    //Sembra che math ceil su fv-integer * P possa andare.
                    int iv = (int) (fv-((fv-Integer.parseInt(versions[4]))*Math.floor(Double.parseDouble(all.get(Integer.parseInt(version) - 1)[1]))));
                    if(Integer.parseInt(version)>iv && Integer.parseInt(version)<fv) {
                        return '1';
                    }
                }

            }
        return null;
    }
}
