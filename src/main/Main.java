package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public static Time time = new Time();
    public static ThreadsExecutor threadsExecutor;

    public static void main(String[] args) {

        //set launch start time
        time.setLaunchStartTime(System.currentTimeMillis());
        System.out.println("App launched");

        //start
        launch(args);

        //test();
/*
        System.out.println("HI");

        long startTime = System.currentTimeMillis();

        ReadFile readFile = new ReadFile();

        ArrayList<File> files = new ArrayList<>();
        readFile.getListOfAllFiles("C:\\corpus\\corpus", files);
        HashSet<String> stopWords = readFile.createStopWordsMap("C:\\corpus\\stop words.txt");
        //Indexer indexer = new Indexer(stopWords);


        // ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        int filesLength = files.size();
        int j = 0;
        File[] tempList = new File[50];
        //Indexer indexer = new Indexer(stopWords);
        for (int i = 0; i < filesLength; i++) {
          /*  for (j = 0; j < 50 && i < filesLength; j++, i++) {
                tempList[j] = files.get(i);
            }*/

        // indexer.indexFile(files.get(i), readFile, true);
        /*    if (j == 50)
                tempList = new File[50];*/

        //indexer.mergeFiles(new File("C:\\corpus\\temp_posting"));
/*
            ArrayList<Pair<String, String>> pairs = readFile.getDocsFromFile(files.get(i));
            for (int j = 0; j < pairs.size(); j++){
                System.out.println(getDocNoFromHash(getHashFromDocNo(pairs.get(j).getKey())));
            }
            */


        //System.out.println("HI");
        //}
        //});
        //}


        //System.out.println("WORDS: " + dictionary.size());
        //  System.out.println("docs:" + docsCounter);

        //executorService.shutdown();
/*
        long stopTime = System.currentTimeMillis();
        System.out.println(stopTime - startTime);
        */
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("MainWindow.fxml"));
        primaryStage.setTitle("Moogle");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
/*
    private static void test() {

        ReadFile readFile = new ReadFile();
        HashSet<String> stopWords = readFile.createStopWordsMap("C:\\corpus\\stop words.txt");
        Indexer indexer = new Indexer();
        long startTime = System.currentTimeMillis();
        createTempPosting();
        System.out.println(System.currentTimeMillis() - startTime);
        indexer.writeDocsToFile();
        indexer.findMostImportantWords();
        indexer.mergeTempPostingFiles(new File("C:\\corpus\\temp_posting"));

        try {
            indexer.splitPostingSortCreateCacheAndUpdateDictionary(new File("C:\\corpus\\temp_posting"));
        } catch (IOException e) {
            e.printStackTrace();
        }


        long stopTime = System.currentTimeMillis();
        System.out.println(stopTime - startTime);
    }

    private static void createTempPosting() {
        DocNameHash docNameHash = new DocNameHash();
        ReadFile readFile = new ReadFile();
        HashSet<String> stopWords = readFile.createStopWordsMap("C:\\corpus\\stop words.txt");
        Parser parser = new Parser(stopWords);
        ArrayList<File> files = new ArrayList<>();
        readFile.getListOfAllFiles("C:\\corpus\\corpus", files);

        //ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        //ExecutorService executorService = Executors.newFixedThreadPool(4);
        int filesLength = files.size();
        //Runnable[] runnables = new Runnable[filesLength];

        //for each file
        for (int i = 0; i < filesLength; i++) {
            //run group of files
            // for (int i = 0; i < filesLength; i = i + 10) {
                /*ArrayList<Pair<String, String>> pairs = new ArrayList<>();
                for(int k = i; k < i + 10 && k < filesLength; k++){
                    pairs.addAll(readFile.getDocsFromFile(files.get(k)));
                }
/*

            ArrayList<Pair<String, String>> pairs = readFile.getDocsFromFile(files.get(i));

            int pairsLength = pairs.size();
            ArrayList<String>[] termsInFile = new ArrayList[pairsLength];
            String[] docsNames = new String[pairsLength];

            //parse terms for each doc
            for (int j = 0; j < pairsLength; j++) {
                Pair doc = pairs.get(j);
                docsNames[j] = (docNameHash.getHashFromDocNo((String) doc.getKey()));
                char[] docArray = ((String) doc.getValue()).toCharArray();
                termsInFile[j] = parser.parse(docArray);
            }
            Indexer indexer = new Indexer(i);
            indexer.indexFile(termsInFile, docsNames, true);
            */
/*
            //System.out.println(i);
            //create runnable
            final ArrayList<String>[] finalTermsInFile = termsInFile;
            final String[] finalDocsNames = docsNames;
            final int finalI = i;
            runnables[i] = new Runnable() {
                @Override
                public void run() {
                    Indexer indexer = new Indexer(finalI);
                    indexer.indexFile(finalTermsInFile, finalDocsNames, true);

                }
            };
            executorService.execute(runnables[i]);
            //if (i % 50 == 0)
                //System.gc();
                */


    // }
    //for (int i = 0; i < runnables.length; i++)
    //    executorService.execute(runnables[i]);
    //executorService.shutdown();
    //try {
    //    executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    //} catch (InterruptedException e) {
    //    e.printStackTrace();
    //}


    // }


}