package main;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class MainWindow {

    @FXML
    private Button playButton;
    @FXML
    private Button resetButton;
    @FXML
    private TextField pathTextField;
    @FXML
    private TextField workingDirectoryTextField;
    @FXML
    private CheckBox stemmingCheckBox;
    @FXML
    private Button showDictionaryButton;
    @FXML
    private Button showCacheButton;
    @FXML
    private Button saveCacheAndDictionaryButton;
    @FXML
    private Button pathBrowseButton;
    @FXML
    private Button directoryBrowseButton;
    @FXML
    private Button loadCacheDictionaryButton;
    @FXML
    private Button runQueryStringButton;
    @FXML
    private TextField queryStringText;
    @FXML
    private Button loadQueriesFileButton;
    @FXML
    private CheckBox mostImportantLinesCheckBox;

    private static String corpusPath;
    private static String workingDirectoryPath;
    private static String pathToSaveDictionaryAndCache = null;
    private static String pathToLoadDictionaryAndCache = null;
    private static Searcher searcher = null;
    private static HashSet<String> stopWords;
    private static boolean engineCreated = false;
    private ArrayList<String> rankedDocsForQuery = null;
    private HashMap<String,List<String>> rankedDocsForQueriesFile = null;
    private static int queryNumber = 100;
    private static File resultsFileNames;
    private static BufferedWriter resultsFileNamesWriter = null;


    public void initialize() {
        System.out.println("GUI launched");
    }

    //private void updateLog(String string){
    //    Main.threadsExecutor.execute(() -> logTextArea.appendText(string));
    //}

    /**
     * choose the corpus directory
     */
    public void browseCorpusPath() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);

        if (selectedDirectory != null) {
            pathTextField.setText(selectedDirectory.getAbsolutePath());
            corpusPath = selectedDirectory.getAbsolutePath();
        }
        pathTextFieldKeyReleased();

        System.out.println("User chose corpus path by browsing");
    }

    /**
     * choose the working directory
     *
     * @throws IOException
     */
    public void browseWorkingDirectory() throws IOException {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);

        if (selectedDirectory != null) {
            workingDirectoryTextField.setText(selectedDirectory.getAbsolutePath());
            workingDirectoryPath = selectedDirectory.getAbsolutePath();
        }
        pathTextFieldKeyReleased();

        System.out.println("User chose output folder by browsing");
    }

    /**
     * if path not null, update corpus path
     */
    public void pathTextFieldKeyReleased() {
        if (checkIfEnteredPath()) {
            corpusPath = pathTextField.getText();
        }
    }

    /**
     * if path not null, update the working directory path
     */
    public void workingDirectoryTextFieldKeyReleased() {
        if (!checkIfEnteredPath())
            workingDirectoryPath = workingDirectoryTextField.getText();
    }

    /**
     * check if the user entered corpus path and working directory path
     *
     * @return true if the user enter path
     */
    private boolean checkIfEnteredPath() {
        //check if text was entered
        if (pathTextField.getText().length() != 0 && workingDirectoryTextField.getText().length() != 0) {
            playButton.setDisable(false);
            resetButton.setDisable(false);
            return false;
        } else {
            playButton.setDisable(true);
            resetButton.setDisable(true);
            return true;
        }
    }

    /**
     * play the inverted index creation.
     * check first if the folder exist
     */
    public void playButtonPressed() {

        System.out.println("User pressed play button");

        //check if the path is valid
        if (!checkIfFolderExist())
            return;

        try {

            //all good let's run
            System.out.println("Stemming method: " + stemmingCheckBox.isSelected());

            //disable button
            //playButton.disableProperty().setValue(true);

            //create folders for the posting files
            createTempPostingDirectory();

            //log
            System.out.println("Indexer starts");
            Main.time.setPlayStartTime(System.currentTimeMillis());

            //start indexer process
            Indexer indexer = new Indexer(workingDirectoryPath, stemmingCheckBox.isSelected());
            long startTime = System.currentTimeMillis();

            //set posting time
            Main.time.setTempPostingStartTime(System.currentTimeMillis());
            System.out.println("Creating temp posting files");

            createTempPosting();

            //log
            System.out.println("Finished creating temp posting files in " + (System.currentTimeMillis() - Main.time.getTempPostingStartTime()) + "ms (" + ((System.currentTimeMillis() - Main.time.getTempPostingStartTime()) / 1000.0 / 60.0) + " minutes)");

            indexer.mergeTempPostingFiles();
            System.out.println("Merging temp posting files and creating final posting file");

            System.out.println("Finding 10,000 most important words");
            indexer.findMostImportantWords();

            //set final posting start time
            Main.time.setFinalPostingStartTime(System.currentTimeMillis());

            System.out.println("Splitting posting file to A-Z and 0-9");
            try {
                indexer.splitPostingSortCreateCacheAndUpdateDictionary();
                //indexer.splitPostingThreads();
            } catch (IOException e) {
                e.printStackTrace();
            }

            indexer.writeDictionaryToFile(workingDirectoryPath + "\\dictionary.txt");

            System.out.println("Finished merging, splitting and creating final posting file in " + (System.currentTimeMillis() - Main.time.getFinalPostingStartTime()) + "ms (" + ((System.currentTimeMillis() - Main.time.getFinalPostingStartTime()) / 1000.0 / 60.0) + " minutes)");

            int numOfDocs = indexer.writeDocsToFile(workingDirectoryPath + "\\docs.txt");
            System.out.println("Amount of docs: " + numOfDocs);

            //System.out.println("Writing cache to file");
            //indexer.writeCacheToFile(workingDirectoryPath + "\\cache.txt");

            System.out.println("Creating information message");
            //create information message
            long endTime = System.currentTimeMillis();
            //File posting = new File(workingDirectoryPath + "\\posting_files");
            File dictionary = new File(workingDirectoryPath + "\\dictionary.txt");
            //File cache = new File(workingDirectoryPath + "\\cache.txt");
            long indexSize;
            if (stemmingCheckBox.isSelected())
                indexSize = (getFolderSize(new File(workingDirectoryPath + "\\posting_files\\Stemming")) + dictionary.length()) / (long) Math.pow(2, 20);
            else
                indexSize = (getFolderSize(new File(workingDirectoryPath + "\\posting_files\\No_Stemming")) + dictionary.length()) / (long) Math.pow(2, 20);
            //long cacheSize = cache.length() / (long) Math.pow(2, 20);

            showAlert("Documents: " + numOfDocs + "\n" +
                    "Index Size: " + indexSize + " MB\n" +
                    //"Cache Size: " + cacheSize + " MB\n" +
                    "Running Time: " + (endTime - startTime) / 60000.0 + "minutes");

            showCacheButton.setDisable(false);
            showDictionaryButton.setDisable(false);
            saveCacheAndDictionaryButton.setDisable(false);
            engineCreated = true;
            loadQueriesFileButton.setDisable(false);

            System.out.println("Finished everything in " + (System.currentTimeMillis() - Main.time.getPlayStartTime()) + "ms (" + ((System.currentTimeMillis() - Main.time.getPlayStartTime()) / 1000.0 / 60.0) + " minutes)");

        } catch (Exception e) {
            e.printStackTrace();

        }

    }

    /**
     * check if the folders exists
     *
     * @return true if the folder exists
     */
    private boolean checkIfFolderExist() {
        File corpus = new File(corpusPath);
        File workingDirectory = new File(workingDirectoryPath);
        if (!corpus.exists() || !workingDirectory.exists()) {
            showAlert("Folder does not exist");
            pathTextField.clear();
            workingDirectoryTextField.clear();
            playButton.setDisable(true);
            resetButton.setDisable(true);
            return false;
        }
        return true;
    }

    /**
     * create folders for the indexer output
     */
    private void createTempPostingDirectory() {

        System.out.println("Creating temp posting directories");

        File posting = new File(workingDirectoryPath + "\\posting_files");
        File stemming = new File(workingDirectoryPath + "\\posting_files\\Stemming");
        File NoStemming = new File(workingDirectoryPath + "\\posting_files\\No_Stemming");
        if (stemmingCheckBox.isSelected())
            deleteFolder(stemming);
        else
            deleteFolder(NoStemming);

        posting.mkdir();
        stemming.mkdir();
        NoStemming.mkdir();
    }

    /**
     * get the total size of directory
     *
     * @return size of the directory
     */
    private long getFolderSize(File folder) {
        long length = 0;
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            return length;
        }
        for (File file : files) {
            if (file.isFile()) {
                length += file.length();
            } else {
                length += getFolderSize(file);
            }
        }
        return length;
    }

    /**
     * create temp posting files
     */
    private void createTempPosting() {
        ReadFile readFile = new ReadFile();
        stopWords = readFile.createStopWordsMap(corpusPath + "\\stop_words.txt");
        Parser parser = new Parser(stopWords);
        ArrayList<File> files = new ArrayList<>();
        readFile.getListOfAllFiles(corpusPath + "\\corpus", files);
        int filesLength = files.size();

        //for each file
        for (int i = 0; i < filesLength; i++) {
            ArrayList<Doc> docs = readFile.getDocsFromFile(files.get(i));
            Indexer indexer = new Indexer(i, workingDirectoryPath, stemmingCheckBox.isSelected());
            indexer.indexFile(docs);
        }

        //write hash
        try {
            DocNameHash.writeHashToFile(new File(corpusPath + "\\hash.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * delete files in the working directory
     * and restart the memory
     */
    public void resetButtonPressed() {

        System.out.println("User pressed reset button");

        //delete posting files
        File directory = new File(workingDirectoryPath);
        for (File f : directory.listFiles()) {
            if (f.isFile())
                f.delete();
            else
                deleteFolder(f);
        }

        //delete main memory
        Indexer indexer = new Indexer();
        indexer.resetStaticFields();

        showCacheButton.setDisable(true);
        showDictionaryButton.setDisable(true);
        saveCacheAndDictionaryButton.setDisable(true);

        showAlert("Program Restarted Successfully");
    }

    /**
     * delete all files inside directory
     *
     * @param folder directory to delete
     */
    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    /**
     * load dictionary,cache and docs from file to memory
     */
    public void loadDictionaryAndCacheButtonPressed() {

        System.out.println("User pressed load dictionary and cache button");

        //choose directory to load dictionary and cache
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);

        if (selectedDirectory != null) {
            pathToLoadDictionaryAndCache = selectedDirectory.getAbsolutePath();
        }
        try {
            BufferedReader dictionaryFile = new BufferedReader(new FileReader(pathToLoadDictionaryAndCache + "\\dictionary.txt"));
            BufferedReader cacheFile = new BufferedReader(new FileReader(pathToLoadDictionaryAndCache + "\\cache.txt"));
            BufferedReader docsFile = new BufferedReader(new FileReader(pathToLoadDictionaryAndCache + "\\docs.txt"));
            HashMap<String, String> dictionary = loadDictionary(dictionaryFile);
            HashMap<String, Pair<ArrayList<String>, Integer>> cache = loadCache(cacheFile);
            HashMap<String, String> docs = loadDocs(docsFile);
            Indexer indexer = new Indexer(dictionary, cache, docs);

            //load hash
            try {
                DocNameHash docNameHash = new DocNameHash();
                docNameHash.readHashToArray(new File(pathToLoadDictionaryAndCache + "\\hash.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            showDictionaryButton.setDisable(false);
            showCacheButton.setDisable(false);
            if (stopWords == null)
                stopWords = new ReadFile().createStopWordsMap(pathToLoadDictionaryAndCache + "\\stop_words.txt");
            engineCreated = true;
            loadQueriesFileButton.setDisable(false);
            showAlert("Dictionary and Cache Loaded Successfully");
        } catch (FileNotFoundException e) {
            showAlert("No Dictionary and Cache in the Directory");
        }

    }

    /**
     * load terms dictionary from file
     *
     * @return dictionary
     */
    private HashMap<String, String> loadDictionary(BufferedReader dictionaryFile) {
        HashMap<String, String> termsDictionary = new HashMap<>();

        try {
            String line;
            while ((line = dictionaryFile.readLine()) != null) {
                String[] termLine = line.split("\t");
                termsDictionary.put(termLine[0], new Term(line).toString());
            }
            dictionaryFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return termsDictionary;
    }

    private HashMap<String, String> loadDocs(BufferedReader docsFile) {
        HashMap<String, String> docDictionary = new HashMap<>();

        try {
            String line;
            while ((line = docsFile.readLine()) != null) {
                char[] lineArray = line.toCharArray();
                StringBuilder name = new StringBuilder();
                int index = 0;
                while (lineArray[index] != '\t')
                    name.append(lineArray[index++]);

                docDictionary.put(name.toString(), line.substring(name.length() + 1));
            }
            docsFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return docDictionary;
    }

    /**
     * load cache from file
     *
     * @return cache
     */
    private HashMap<String, Pair<ArrayList<String>, Integer>> loadCache(BufferedReader cacheFile) {
        HashMap<String, Pair<ArrayList<String>, Integer>> cache = new HashMap<>();

        try {
            String line;
            while ((line = cacheFile.readLine()) != null) {
                char[] termLine = line.toCharArray();
                ArrayList<String> docs = new ArrayList<>();
                int index = 0;
                StringBuilder term = new StringBuilder();
                StringBuilder lineInPosting = new StringBuilder();

                //read term name
                while (termLine[index] != '\t')
                    term.append(termLine[index++]);
                index++;

                //read term line in posting file
                while (termLine[index] != '\t')
                    lineInPosting.append(termLine[index++]);
                index++;

                while (index < termLine.length) {
                    StringBuilder docName = new StringBuilder();
                    StringBuilder frequency = new StringBuilder();
                    StringBuilder indexInDoc = new StringBuilder();
                    StringBuilder tf = new StringBuilder();

                    //read doc name
                    for (int i = 0; i < 3; i++)
                        docName.append(termLine[index++]);

                    //read term frequency in doc
                    while (termLine[index] != '*')
                        frequency.append(termLine[index++]);
                    index++;

                    //read term index in doc
                    while (termLine[index] != '*')
                        indexInDoc.append(termLine[index++]);
                    index++;

                    //read tf
                    while (index < termLine.length && termLine[index] != '\t')
                        tf.append(termLine[index++]);
                    index++;

                    //TermInDocCache doc = new TermInDocCache(docName.toString(), Integer.parseInt(frequency.toString()), Integer.parseInt(indexInDoc.toString()));
                    //docs.add(doc);
                    docs.add(new TermInDocCache(docName.toString(),
                            Integer.parseInt(frequency.toString()),
                            Double.parseDouble(indexInDoc.toString()),
                            Double.parseDouble(tf.toString())).toString());
                }

                cache.put(term.toString(), new Pair<>(docs, Integer.parseInt(lineInPosting.toString())));
            }
            cacheFile.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
        return cache;

    }

    /**
     * save dictionary and cache to file
     */
    public void saveDictionaryAndCacheButtonPressed() {

        System.out.println("User pressed save dictionary and cache button");

        //choose directory to save dictionary and cache
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);

        //save dictionary and cache
        if (selectedDirectory != null) {
            pathToSaveDictionaryAndCache = selectedDirectory.getAbsolutePath();
            Indexer indexer = new Indexer();
            indexer.writeDictionaryToFile(pathToSaveDictionaryAndCache + "\\dictionary.txt");
            indexer.writeCacheToFile(pathToSaveDictionaryAndCache + "\\cache.txt");
            indexer.writeDocsToFile(pathToLoadDictionaryAndCache + "\\docs.txt");
        }

        showAlert("Dictionary and Cache Saved Successfully");


    }

    /**
     * show alert to the user
     *
     * @param alertMessage message to the user
     */
    private void showAlert(String alertMessage) {

        System.out.println("<alert>\n" + alertMessage + "\n</alert>");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(alertMessage);
        alert.show();
    }

    /**
     * loads the cache from file, sorts it and display
     *
     * @param actionEvent action event
     */
    public void showCacheButtonPressed(ActionEvent actionEvent) {
        Indexer indexer = new Indexer();
        showData("Cache", indexer.getCacheSorted());
    }

    /**
     * loads the dictionary from file, sorts it and display
     *
     * @param actionEvent action event
     */
    public void showDictionaryButtonPressed() {
        Indexer indexer = new Indexer();
        showData("Dictionary", indexer.getDictionarySorted());
    }

    /**
     * open new window and show data
     *
     * @param type      type of the data
     * @param strings   data to show
     */
    private void showData(String type, ArrayList<String> strings) {
        ObservableList<String> lines = FXCollections.observableArrayList();
        ListView<String> listView = new ListView<>(lines);
        listView.setPrefHeight(1000);
        listView.setPrefWidth(1000);
        listView.getItems().clear();

        //run through the list
        int stringsSize = strings.size();
        for (int i = 0; i < stringsSize; i++)
            listView.getItems().add(strings.get(i));

        VBox pane = new VBox();
        pane.getChildren().addAll(listView);

        //if query, add button to save
        if (type.compareTo("Query") == 0) {
            Button button = new Button();
            button.setDefaultButton(true);
            button.setText("Save Results");
            button.setOnAction(event -> saveQueryResultButtonPressed());
            button.setPrefWidth(1000);
            pane.getChildren().add(button);
        }

        Stage stage = new Stage();
        stage.setTitle(type);
        stage.setScene(new Scene(pane, 1000, 800));
        stage.show();
    }

    /**
     * run query
     */
    public void runQueryStringButtonClick() {
        if (searcher == null)
            searcher = new Searcher(stopWords);
        if (!mostImportantLinesCheckBox.isSelected()) {
            String query = queryStringText.getText();
            if(workingDirectoryPath == null)
                rankedDocsForQuery = searcher.search(query, stemmingCheckBox.isSelected(), pathToLoadDictionaryAndCache);
            else
                rankedDocsForQuery = searcher.search(query, stemmingCheckBox.isSelected(), corpusPath);

            //case of the query doesn't have results
            if(rankedDocsForQuery.size() == 0)
                showAlert("No match results for the query");
            else
                showData("Query", rankedDocsForQuery);



        } else {
            ArrayList<Pair<String, Integer>> rankedSentences = searcher.find5MostImportantSentences(queryStringText.getText(), pathToLoadDictionaryAndCache + "\\corpus");
            ArrayList<String> sentencesToPrint = new ArrayList<>();
            for(int i = 0; i < rankedSentences.size(); i++)
                sentencesToPrint.add(rankedSentences.get(i).getKey());
            showData("5 Most Important Sentences", sentencesToPrint);
        }
    }

    /**
     * reset results and delete results files
     */
    public void resetQueriesDataButtonClick() {
        //initial fields
        rankedDocsForQuery = null;
        rankedDocsForQueriesFile = null;

        //delete all results files
        try {
            if (resultsFileNamesWriter != null) {
                resultsFileNamesWriter.close();
                BufferedReader br = new BufferedReader(new FileReader(resultsFileNames));
                String line;
                while ((line = br.readLine()) != null) {
                    File file = new File(line);
                    file.delete();
                }
                //delete file content
                deleteFileContent(resultsFileNames);
                resultsFileNamesWriter = null;

            }
        }

        catch (IOException e){
            e.printStackTrace();
        }

        showAlert("Program Restarted Successfully");
    }

    /**
     * delete file content
     * @param file file to delete
     */
    private void deleteFileContent(File file){
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        writer.print("");
        writer.close();
    }

    /**
     * user entered query or deleted query
     */
    public void queryTextFieldKeyReleased() {
        if (queryStringText.getText().length() > 0 && engineCreated)
            runQueryStringButton.setDisable(false);
        if (queryStringText.getText().length() == 0)
            runQueryStringButton.setDisable(true);

    }

    /**
     * load queries file and run each query
     */
    public void loadAndRunQueriesFileButtonPressed() {
        String pathForQueriesFile;
        //choose directory to load queries file
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(null);

        if (selectedDirectory != null) {
            pathForQueriesFile = selectedDirectory.getAbsolutePath();
            List<Pair<String, String>> queries = new ReadFile().readQueriesFile(pathForQueriesFile + "\\queries.txt");
            if (searcher == null)
                searcher = new Searcher(stopWords);
            rankedDocsForQueriesFile = searcher.search(queries, stemmingCheckBox.isSelected(), pathToLoadDictionaryAndCache);

            showData("Query", new ArrayList<>()); //TODO:need to change!!


            //TODO: show the results

        }

    }

    /**
     * save quert results
     */
    public void saveQueryResultButtonPressed(){
        if(rankedDocsForQueriesFile == null && rankedDocsForQuery == null)
            showAlert("No query results to save");
        System.out.println("User pressed save results button");
        try {
            if (resultsFileNamesWriter == null) {
                resultsFileNames = new File(pathToLoadDictionaryAndCache + "\\resultsFileNames.txt");
                resultsFileNamesWriter = new BufferedWriter(new FileWriter(resultsFileNames));
            }


            //choose file to save results
            FileChooser fileChooser = new FileChooser();
            File selectedDirectory = fileChooser.showSaveDialog(null);

            //save dictionary and cache
            if (selectedDirectory != null) {
                writeQueriesResultsToFile(selectedDirectory.getAbsolutePath());
                //write to file the chosen file path in order to delete in reset
                resultsFileNamesWriter.write(selectedDirectory.getAbsolutePath() + '\n');
                showAlert("Results Saved Successfully");

            }
        }

        catch(IOException e){
            e.printStackTrace();
        }

    }

    /**
     * write results of the queries searches to file
     *
     * @param path path to the file
     */
    private void writeQueriesResultsToFile(String path){
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(path));

            //save results for queries file search
            if(rankedDocsForQueriesFile != null) {
                for (String query : rankedDocsForQueriesFile.keySet()) {
                    List<String> queryResults = rankedDocsForQueriesFile.get(query);
                    for (int i = 0; i < queryResults.size(); i++) {
                        String[] splitLine = queryResults.get(i).split("\t");
                        bw.write(query + " " + 0 + " " + splitLine[1] + " 1 42.38 mt" + '\n');
                    }
                }
            }

            //save results for query search
            if(rankedDocsForQuery != null){
                for(int i = 0; i < rankedDocsForQuery.size(); i++){
                    String[] splitLine = rankedDocsForQuery.get(i).split("\t");
                    bw.write(queryNumber + " " + 0 + " " + splitLine[1] + " 1 42.38 mt" + '\n');
                    queryNumber++;
                }
            }

            bw.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

}

