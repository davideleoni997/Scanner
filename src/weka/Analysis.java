package weka;

import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.core.neighboursearch.LinearNNSearch;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemoveWithValues;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Analysis {

    private static final String RESULTS = "\nResults\n======\n";
    private static double totDataBook;
    private static double totDataAvro;

    public static void main(String[] args) throws Exception {
        ConverterUtils.DataSource sourceBook = new ConverterUtils.DataSource(Paths.get("").toAbsolutePath().toString() +"\\Reports\\BOOKKEEPERWeka.arff");
        ConverterUtils.DataSource sourceAvro = new ConverterUtils.DataSource(Paths.get("").toAbsolutePath().toString() +"\\Reports\\AVROWeka.arff");

        Instances dataBook = sourceBook.getDataSet();
        Instances dataAvro = sourceAvro.getDataSet();
        totDataBook = dataBook.size();
        totDataAvro = dataAvro.size();

        if(dataBook.classIndex() == -1)
            dataBook.setClassIndex(dataBook.numAttributes() -1);
        if(dataAvro.classIndex() == -1)
            dataAvro.setClassIndex(dataAvro.numAttributes() -1);


        Remove rm = new Remove();
        rm.setAttributeIndicesArray(new int[]{1});

        rm.setInputFormat(dataBook);

        dataAvro = Filter.useFilter(dataAvro,rm);
        dataAvro.setRelationName("AVRO ");
        dataBook = Filter.useFilter(dataBook, rm);
        dataBook.setRelationName("BOOK ");

        ArrayList<Instances> avro = new ArrayList<>();
        avro.add(dataAvro);
        avro.addAll(balance(dataAvro));
        ArrayList<Instances> book = new ArrayList<>();
        book.add(dataBook);
        book.addAll(balance(dataBook));




        ArrayList<Instances> avroTesting;
        avroTesting = new ArrayList<>();
        ArrayList<Instances> avroTraining = new ArrayList<>();
        ArrayList<Instances> bookTesting = new ArrayList<>();
        ArrayList<Instances> bookTraining = new ArrayList<>();
        ArrayList<Instances> avroTestingBestF = new ArrayList<>();
        ArrayList<Instances> avroTrainingBestF;
        avroTrainingBestF = new ArrayList<>();
        ArrayList<Instances> bookTrainingBestF = new ArrayList<>();
        ArrayList<Instances> bookTestingBestF = new ArrayList<>();

        divide(avro,avroTraining,avroTesting,book,bookTraining,bookTesting);
        divide(avro,avroTrainingBestF,avroTestingBestF,book,bookTrainingBestF,bookTestingBestF);

        ArrayList<ArrayList<Instances>> ret;
        ret = selectFeature(avroTrainingBestF,avroTestingBestF);
        avroTrainingBestF = ret.get(0);
        avroTestingBestF = ret.get(1);

        ret = selectFeature(bookTrainingBestF,bookTestingBestF);
        bookTrainingBestF = ret.get(0);
        bookTestingBestF = ret.get(1);

        //Building classifiers


        String[] rfOptions = new String[14];
        rfOptions[0]="-P";
        rfOptions[1]="100";
        rfOptions[2]="-I";
        rfOptions[3]="100";
        rfOptions[4]="-num-slots";
        rfOptions[5]="1";
        rfOptions[6]="-K";
        rfOptions[7]="0";
        rfOptions[8]="-M";
        rfOptions[9]="1.0";
        rfOptions[10]="-V";
        rfOptions[11]="0.001";
        rfOptions[12]="-S";
        rfOptions[13]="1";
        RandomForest rf = new RandomForest();
        rf.setOptions(rfOptions);


        NaiveBayes nb = new NaiveBayes();


        String[] ibkOptions = new String[4];
        ibkOptions[0]="-K";
        ibkOptions[1]="1";
        ibkOptions[2]="-W";
        ibkOptions[3]="0";
        IBk ibk = new IBk();
        ibk.setOptions(ibkOptions);
        ibk.setNearestNeighbourSearchAlgorithm(new LinearNNSearch());

        //Add cost sensitive classifier HERE and use it in evaluate method
        CostSensitiveClassifier c1 = new CostSensitiveClassifier();
        c1.setCostMatrix( createCostMatrix(1, 10));
        c1.setMinimizeExpectedCost(false);

        CostSensitiveClassifier c2 = new CostSensitiveClassifier();
        c2.setCostMatrix(createCostMatrix(1,10));
        c2.setMinimizeExpectedCost(true);

        Classifiers cf = new Classifiers(rf,nb,ibk,c1,c2);

        evaluate(avroTesting,avroTraining,bookTesting,bookTraining,cf);
        evaluate(avroTestingBestF,avroTrainingBestF,bookTestingBestF,bookTrainingBestF,cf);
    }

    private static void divide(ArrayList<Instances> dataAvro,ArrayList<Instances> avroTraining,ArrayList<Instances> avroTesting,ArrayList<Instances> dataBook,ArrayList<Instances> bookTraining,ArrayList<Instances> bookTesting) throws Exception {
        RemoveWithValues rwv = new RemoveWithValues();
        rwv.setAttributeIndex("1");
        for (Instances instances : dataAvro) {
            for (double i = 4.0; i <= 17.0; i = i + 1) {
                split(avroTraining, avroTesting, rwv, instances, i);

            }
        }
        for (Instances instances : dataBook) {
            for (double i = 3.0; i <= 7.0; i = i + 1) {
                split(bookTraining, bookTesting, rwv, instances, i);

            }
        }
    }

    private static CostMatrix createCostMatrix(double weightFalsePositive, double weightFalseNegative) {
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 0, weightFalsePositive);
        costMatrix.setCell(0, 1, weightFalseNegative);
        costMatrix.setCell(1, 1, 0.0);
        return costMatrix;
    }


    private static void split(ArrayList<Instances> avroTraining, ArrayList<Instances> avroTesting, RemoveWithValues rwv, Instances instances, double i) throws Exception {
        //Delete part of the newest data
        rwv.setSplitPoint(i);
        rwv.setInputFormat(instances);
        rwv.setInvertSelection(true);
        Instances train = Filter.useFilter(instances, rwv);
        train.setRelationName(instances.relationName() + "RUN ");
        avroTraining.add(train);
        rwv.setInvertSelection(false);
        Instances tempTest = Filter.useFilter(instances, rwv);
        tempTest.setRelationName(instances.relationName());
        rwv.setInvertSelection(true);
        rwv.setSplitPoint(i + 1);
        Instances test = Filter.useFilter(tempTest, rwv);
        test.setRelationName(tempTest.relationName() + "RUN ");
        avroTesting.add(test);
    }

    private static void evaluate(ArrayList<Instances> actualAvroTesting, ArrayList<Instances> actualAvroTraining, ArrayList<Instances> actualBookTesting, ArrayList<Instances> actualBookTraining, Classifiers cf) {
        try (FileWriter fw = new FileWriter(Paths.get("").toAbsolutePath().toString() + "\\Reports\\WekaAnalysis.csv", true)) {
            fw.write("DATASET,%TRAININGDATA,%DEFECTTRAIN,%DEFECTTEST,CLASSIFIER,TP,FP,TN,FN,PRECISION,RECALL,ROC,KAPPA\n");

            for (int i = 0; i < actualAvroTesting.size(); i++) {
                eval(actualAvroTesting, actualAvroTraining, cf, i,fw);


            }

            for (int i = 0; i < actualBookTesting.size(); i++) {
                eval(actualBookTesting, actualBookTraining, cf, i,fw);


            }
        } catch (Exception e) {
            Logger.getGlobal().log(Level.INFO,e.getMessage());
    }
    }

    private static void eval(ArrayList<Instances> actualBookTesting, ArrayList<Instances> actualBookTraining, Classifiers cf, int i, FileWriter fw) throws Exception {

            double nonbuggyTrain;
            double buggyTrain;
            double nonbuggyTest;
            double buggyTest;
            AttributeStats statsTrain;
            AttributeStats statsTest;
            statsTrain = actualBookTraining.get(i).attributeStats(actualBookTraining.get(i).numAttributes()-1);
            statsTest = actualBookTesting.get(i).attributeStats(actualBookTesting.get(i).numAttributes()-1);

            nonbuggyTrain = statsTrain.nominalCounts[0];
            buggyTrain = statsTrain.nominalCounts[1];
            nonbuggyTest = statsTest.nominalCounts[0];
            buggyTest = statsTest.nominalCounts[1];

            Evaluation rfeval = new Evaluation(actualBookTesting.get(i));
            cf.getRf().buildClassifier(actualBookTraining.get(i));
            rfeval.evaluateModel(cf.getRf(), actualBookTesting.get(i));
            Logger.getGlobal().log(Level.INFO, "Res RandFor:{0} \n {1}\n", new String[]{rfeval.toSummaryString(RESULTS, false), actualBookTesting.get(i).relationName()});
        accoda(actualBookTesting, actualBookTraining, i, fw);

        String percentTrain = String.valueOf(buggyTrain / (nonbuggyTrain + buggyTrain));
            fw.append(percentTrain).append(",");
            String percentTest = String.valueOf(buggyTest / (nonbuggyTest + buggyTest));
            fw.append(percentTest).append(",");
            fw.append("Random Forest,");
            toCsv(fw, rfeval);

            Evaluation nbeval = new Evaluation(actualBookTesting.get(i));
            cf.getNb().buildClassifier(actualBookTraining.get(i));
            nbeval.evaluateModel(cf.getNb(), actualBookTesting.get(i));
            Logger.getGlobal().log(Level.INFO, "Res NaiveB:{0} \n {1}\n", new String[]{nbeval.toSummaryString(RESULTS, false), actualBookTesting.get(i).relationName()});
        accoda(actualBookTesting, actualBookTraining, i, fw);


        fw.append(percentTrain).append(",");
            fw.append(percentTest).append(",");
            fw.append("NaiveBayes,");
            toCsv(fw, nbeval);

            Evaluation ieval = new Evaluation(actualBookTesting.get(i));
            cf.getIbk().buildClassifier(actualBookTraining.get(i));
            ieval.evaluateModel(cf.getIbk(), actualBookTesting.get(i));
            Logger.getGlobal().log(Level.INFO, "Res ibk:{0} \n {1}\n", new String[]{ieval.toSummaryString(RESULTS, false), actualBookTesting.get(i).relationName()});
        accoda(actualBookTesting, actualBookTraining, i, fw);


        fw.append(percentTrain).append(",");
            fw.append(percentTest).append(",");
            fw.append("IBK,");
            toCsv(fw, ieval);

        for(int j =0; j<3;j++) {
            switch(j) {
                case 0:
                    cf.getC1().setClassifier(cf.getRf());
                    cf.getC2().setClassifier(cf.getRf());
                    break;
                case 1:
                    cf.getC1().setClassifier(cf.getNb());
                    cf.getC2().setClassifier(cf.getNb());
                    break;
                case 2:
                    cf.getC1().setClassifier(cf.getIbk());
                    cf.getC2().setClassifier(cf.getIbk());
                    break;
                default:
            }
            Evaluation ceval = new Evaluation(actualBookTesting.get(i));
            cf.getC1().buildClassifier(actualBookTraining.get(i));
            ceval.evaluateModel(cf.getC1(), actualBookTesting.get(i));
            Logger.getGlobal().log(Level.INFO, "Res costsensLearn:{0} \n {1}\n", new String[]{ceval.toSummaryString(RESULTS, false), actualBookTesting.get(i).relationName()});
            accoda(actualBookTesting, actualBookTraining, i, fw);

            fw.append(percentTrain).append(",");
            fw.append(percentTest).append(",");
            switch(j) {
                case 0:
                    fw.append("CostSensitiveLearning RandomForest,");
                    break;
                case 1:
                    fw.append("CostSensitiveLearning NaiveBayes,");
                    break;
                case 2:
                    fw.append("CostSensitiveLearning IBK,");
                    break;
                default :
            }
            toCsv(fw, ceval);

            Evaluation c2eval = new Evaluation(actualBookTesting.get(i));
            cf.getC2().buildClassifier(actualBookTraining.get(i));
            c2eval.evaluateModel(cf.getC2(), actualBookTesting.get(i));
            Logger.getGlobal().log(Level.INFO, "Res costsensTresh:{0} \n {1}\n", new String[]{c2eval.toSummaryString(RESULTS, false), actualBookTesting.get(i).relationName()});
            accoda(actualBookTesting, actualBookTraining, i, fw);

            fw.append(percentTrain).append(",");
            fw.append(percentTest).append(",");
            if(j==0)
                fw.append("CostSensitiveThreshold RandomForest,");
            else if(j==1)
                fw.append("CostSensitiveThreshold NaiveBayes,");
            else if(j==2)
                fw.append("CostSensitiveThreshold IBK,");

            toCsv(fw, c2eval);
        }

    }

    private static void accoda(ArrayList<Instances> actualBookTesting, ArrayList<Instances> actualBookTraining, int i, FileWriter fw) throws IOException {

        fw.append(actualBookTesting.get(i).relationName()).append(",");

        if(actualBookTesting.get(i).relationName().contains("BOOK"))
            fw.append(String.valueOf(actualBookTraining.get(i).size()/totDataBook)).append(",");
        else
            fw.append(String.valueOf(actualBookTraining.get(i).size()/totDataAvro)).append(",");
    }

    private static void toCsv(FileWriter fw, Evaluation ieval) throws IOException {
        fw.append(String.valueOf(ieval.numTruePositives(1))).append(",");
        fw.append(String.valueOf(ieval.numFalsePositives(1))).append(",");
        fw.append(String.valueOf(ieval.numTrueNegatives(1))).append(",");
        fw.append(String.valueOf(ieval.numFalseNegatives(1))).append(",");
        fw.append(String.valueOf(ieval.precision(1))).append(",");
        fw.append(String.valueOf(ieval.recall(1))).append(",");
        fw.append(String.valueOf(ieval.areaUnderROC(1))).append(",");
        fw.append(String.valueOf(ieval.kappa()));
        fw.append("\n");
    }

    private static ArrayList<Instances> balance(Instances data) throws Exception {
        //Balancing
        ArrayList<Instances> ret = new ArrayList<>();
        String[] underSampling = new String[2];
        underSampling[0]="-M";
        underSampling[1]="1.0";

            double nb = 1;
            double b = 1;
            try (Scanner scan = new Scanner(data.attributeStats(data.numAttributes() - 1).toString())) {
                scan.nextLine();
                scan.next();
                scan.next();
                scan.next();
                scan.next();
                scan.next();
                scan.next();
                scan.next();
                scan.next();
                scan.next();
                scan.next();
                scan.next();
                nb = scan.nextInt();
                b = scan.nextInt();
            } catch (Exception e) {
                Logger.getGlobal().log(Level.INFO,e.getMessage());
            }

            String[] overSampling = new String[4];
            overSampling[0] = "-B";
            overSampling[1] = "1.0";
            overSampling[2] = "-Z";
            if (nb < b)
                overSampling[3] = String.valueOf((b / (nb + b))*200);
            else
                overSampling[3] = String.valueOf((nb / (nb + b))*200);

            SpreadSubsample spread = new SpreadSubsample();
            spread.setInputFormat(data);
            spread.setOptions(underSampling);
            Instances bookUndersampled = Filter.useFilter(data, spread);
            bookUndersampled.setRelationName(data.relationName()+ "UNDERSAMPLED ");
            ret.add(bookUndersampled);


            Resample rsa = new Resample();
            rsa.setNoReplacement(false);
            rsa.setOptions(overSampling);
            rsa.setInputFormat(data);
            Instances oversampled = Filter.useFilter(data, rsa);
            oversampled.setRelationName(data.relationName()+"OVERSAMPLED ");

            ret.add(oversampled);


            SMOTE smote = new SMOTE();
            smote.setInputFormat(data);
            Instances bookSmote = Filter.useFilter(data,smote);
            bookSmote.setRelationName(data.relationName() + "SMOTE ");

            ret.add(bookSmote);

            return ret;
    }

    private static ArrayList<ArrayList<Instances>> selectFeature(ArrayList<Instances> data, ArrayList<Instances> testing) throws Exception {
        //Feature Selection
        ArrayList<Instances> ret = new ArrayList<>();
        ArrayList<Instances> testret = new ArrayList<>();
        weka.filters.supervised.attribute.AttributeSelection attsel = new weka.filters.supervised.attribute.AttributeSelection();
        CfsSubsetEval attrEval = new CfsSubsetEval();
        BestFirst bf = new BestFirst();
        attsel.setEvaluator(attrEval);
        attsel.setSearch(bf);
        for(Instances in : data) {
            attsel.setInputFormat(in);
            Instances dataBookBF = Filter.useFilter(in, attsel);
            Instances testBF = Filter.useFilter(testing.get(data.indexOf(in)),attsel);
            dataBookBF.setRelationName(in.relationName() + "BEST FIRST ");
            testBF.setRelationName(in.relationName() + "BEST FIRST ");
            ret.add(dataBookBF);
            testret.add(testBF);
        }
       ArrayList<ArrayList<Instances>> ritorno = new ArrayList<>() ;
        ritorno.add(ret);
        ritorno.add(testret);
        return ritorno;

    }

    private static class Classifiers{
        private final RandomForest rf;
        private final NaiveBayes nb;
        private final IBk ibk;
        private final CostSensitiveClassifier c1;
        private final CostSensitiveClassifier c2;


        public Classifiers(RandomForest rf, NaiveBayes nb, IBk ibk, CostSensitiveClassifier c1, CostSensitiveClassifier c2) {
            this.rf = rf;
            this.nb = nb;
            this.ibk = ibk;
            this.c1 = c1;
            this.c2 = c2;
        }

        public RandomForest getRf() {
            return rf;
        }

        public NaiveBayes getNb() {
            return nb;
        }

        public IBk getIbk() {
            return ibk;
        }

        public CostSensitiveClassifier getC1() {
            return c1;
        }

        public CostSensitiveClassifier getC2() {return  c2; }
    }

}
