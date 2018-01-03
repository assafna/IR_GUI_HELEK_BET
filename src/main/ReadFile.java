package main;

import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Used to read the corpus files
 */
public class ReadFile {

    private DocNameHash docNameHash;
    static int counter = 0;

    ReadFile() {
        docNameHash = new DocNameHash();
    }


    private static int numOfDocs;

    /**
     * get a list of all files in a directory into an array list
     *
     * @param directoryName directory path
     * @param files         array to fill in with values
     */
    public void getListOfAllFiles(String directoryName, ArrayList<File> files) {

        File directory = new File(directoryName);

        //get all the files from a directory in a recursive way
        File[] fList = directory.listFiles();
        assert fList != null;
        for (int i = 0; i < fList.length; i++) {
            if (fList[i].isFile()) {
                files.add(fList[i]);
            } else if (fList[i].isDirectory()) {
                getListOfAllFiles(fList[i].getAbsolutePath(), files);
            }
        }
    }


    /**
     * get a list of all docs in a file
     *
     * @param file file to read from
     * @return list of docs
     */
    public ArrayList<Doc> getDocsFromFile(File file) {

        ArrayList<Doc> pairs = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = br.readLine()) != null) {

                StringBuilder docNo = new StringBuilder();
                StringBuilder date = new StringBuilder();
                StringBuilder text = new StringBuilder();

                //check if DOCNO
                if (line.contains("<DOCNO>")) {
                    char[] lineCharArray = line.toCharArray();
                    for (int i = 7; i < 100; i++) { //number 100 is not relevant
                        char c;
                        if ((c = lineCharArray[i]) == 60) //<
                            break;
                        if (c != 32) //*SPACE*
                            docNo.append(lineCharArray[i]);
                    }
                    //new doc
                    line = br.readLine();

                    while (line != null) {

                        //find date of files that start with LA
                        if (line.contains("<DATE")) {
                            if (file.getName().startsWith("LA") && line.contains("<DATE>")) {
                                line = br.readLine();
                                line = br.readLine();
                                lineCharArray = line.toCharArray();
                                int countComma = 0;
                                int index = 0;
                                while (index < lineCharArray.length) {
                                    if (lineCharArray[index] == ',') {
                                        countComma++;
                                        index++;
                                        continue;
                                    }
                                    if (countComma == 2)
                                        break;
                                    else
                                        date.append(lineCharArray[index]);
                                    index++;
                                }
                            } else
                                //find date for files that start with FB or FT
                                date = parseDateForFBorFTfiles(line, file.getName());
                        }


                        //looking for text
                        if (line.contains("<TEXT>")) {

                            line = br.readLine();

                            //reading all the text
                            while (line != null && !line.contains("</TEXT>")) {

                                text.append(line);
                                text.append(" ");
                                line = br.readLine();
                            }

                            break;

                        }
                        if (line.contains("</DOC>"))
                            break;

                        line = br.readLine();

                    }

                    String convertedDate;
                    if (date.toString().length() == 0) {
                        date.append("-");
                        convertedDate = date.toString();
                    }
                    else
                        convertedDate = convertDateToUniformFormat(date.toString(), file.getName());
                    pairs.add(new Doc(docNo.toString(), docNameHash.getHashFromDocNo(docNo.toString()),text.toString(), convertedDate ,file.getName()));
                }

            }
            counter++;

        } catch (IOException e) {
            e.printStackTrace();
        }
        numOfDocs += pairs.size();

        return pairs;

    }

    /**
     * creates a hash set of stop words by reading them from a file
     *
     * @param path path to stop words file
     * @return hash set of stop words
     */
    public HashSet<String> createStopWordsMap(String path) {
        HashSet<String> stopWords = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            while ((line = br.readLine()) != null) {
                stopWords.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stopWords;
    }

    /**
     * parse date for files that start with "FB" or "FT"
     *
     * @param line     line to parse
     * @param fileName name of the file
     * @return date
     */
    private StringBuilder parseDateForFBorFTfiles(String line, String fileName) {
        StringBuilder date = new StringBuilder();

        char[] lineCharArray;

        //for files that start with "FB"
        if (fileName.startsWith("FB") && line.contains("<DATE1>")) {
            lineCharArray = line.toCharArray();
            int i = 7;
            while (lineCharArray[i] == 32)
                i++;

            while (lineCharArray[i] != '<') {
                if (lineCharArray[i] == 32 && lineCharArray[i + 1] == '<')
                    break;
                if (lineCharArray[i] == 32 && lineCharArray[i + 1] == 32) {
                    i++;
                    continue;
                }
                date.append(lineCharArray[i]);
                i++;

            }
            return date;
        }

        //for files that start with "FT"
        if (fileName.startsWith("FT") && line.contains("<DATE>")) {
            lineCharArray = line.toCharArray();
            date.append(lineCharArray[10]);
            date.append(lineCharArray[11]);
            date.append("/");
            date.append(lineCharArray[8]);
            date.append(lineCharArray[9]);
            date.append("/19");
            date.append(lineCharArray[6]);
            date.append(lineCharArray[7]);

            return date;
        }
        return date;
    }

    /**
     * convert date according to the format DD/NN/YYYY
     * @param date date to convert
     * @param fileName file name
     * @return converted date
     */
    private String convertDateToUniformFormat(String date, String fileName) {
        char[] nameArray = fileName.toCharArray();
        if (nameArray[0] == 'F' && nameArray[1] == 'T')
            return date;

        char[] dateArray = date.toCharArray();
        StringBuilder newDate = new StringBuilder();

        //file name start with "FB"
        if (nameArray[0] == 'F' && nameArray[1] == 'B') {
            int monthIndex;
            //day has 1 digit
            if (dateArray[1] == 32) {
                newDate.append(0);
                newDate.append(dateArray[0]);
                monthIndex = 2;
            } else {
                newDate.append(dateArray[0]);
                newDate.append(dateArray[1]);
                monthIndex = 3;
            }
            newDate.append("/");
            //add month
            Parser parser = new Parser(dateArray, monthIndex);
            newDate.append(parser.getMonth());

        }

        //file name starts with "LA"
        if (nameArray[0] == 'L' && nameArray[1] == 'A') {

            //add day
            int i;
            for (i = 0; i < dateArray.length && !isDigit(dateArray[i]); i++) ;
            if(i == dateArray.length)
                return date.toString();
            if (dateArray[i + 1] == 32) {
                newDate.append(0);
                newDate.append(dateArray[i]);
            }
            else {
                newDate.append(dateArray[i]);
                newDate.append(dateArray[i + 1]);
            }

            newDate.append("/");
            //add month
            Parser parser = new Parser(dateArray, 0);
            newDate.append(parser.getMonth());
        }
        newDate.append("/");
        //add year
        newDate.append(dateArray[dateArray.length - 4]);
        newDate.append(dateArray[dateArray.length - 3]);
        newDate.append(dateArray[dateArray.length - 2]);
        newDate.append(dateArray[dateArray.length - 1]);
        return newDate.toString();
    }

    /**
     * read queries from file
     * @param file
     * @return list of queries
     */
    public ArrayList<String> readQueriesFile(String file){
        ArrayList<String> queries = new ArrayList<>();
        try{
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line = br.readLine()) != null){
                if(line.contains("<title>"))
                    queries.add(line.substring(8));
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return queries;

    }

    /**
     * split file into sentences
     * @param file file to split
     * @return list of all sentences in file
     */
    public List<String> getListOfSentencesInFile(File file, String docNo){
        List<String> sentences = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                //looking for the doc
                if (line.contains(docNo)) {
                    while ((line = br.readLine()) != null) {
                        //looking for text
                        if (line.contains("<TEXT>")) {

                            line = br.readLine();

                            //reading all the text
                            while (line != null && !line.contains("</TEXT>")) {

                                text.append(line);
                                text.append(" ");
                                line = br.readLine();
                            }
                            break;
                        }
                    }
                    break;
                }
            }

            char[] textArray = text.toString().toCharArray();
            StringBuilder sentence = new StringBuilder();
            int i = 0;
            while (i < textArray.length) {
                if (i > 0 && !isDigit(textArray[i - 1]) && textArray[i] == '.') {
                    sentences.add(sentence.toString());
                    sentence = new StringBuilder();
                } else
                    sentence.append(textArray[i]);
                i++;
            }

        }
        catch (IOException e){
            e.printStackTrace();
        }
        return sentences;
    }

    public Doc getDoc(String docNo, String path){
        Doc doc = null;
        try{
            BufferedReader br= new BufferedReader(new FileReader(path + "docs.txt"));
            String line;
            while((line = br.readLine()) != null){
                if(line.contains(docNo)){
                    String[] docLine = line.split("\t");
                   // doc = new Doc(docNo,Integer.parseInt(docLine[0]), Integer.parseInt(docLine[1]),docLine[2], docLine[3]);

                }
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return doc;

    }

    public int getNumOfDocs() {
        return numOfDocs;
    }

    @Override
    public String toString() {
        return numOfDocs + "";
    }


    /**
     * check if the char is a digit
     * @param c char to check
     * @return true if the char is digit
     */
    private boolean isDigit(char c){
        return c >= 48 && c <= 57;
    }

    /**
     * reads a term from posting file and returns list of docs for this term
     * @param indexer indexer to use
     * @param term term to read
     * @return list of docs for this term
     * @throws IOException exception
     */
    public ArrayList<Pair<String, TermInDocCache>> getTermDocsFromPosting(Indexer indexer, String term) throws IOException {
        //get posting path
        String postingFilesPath = indexer.getTempPostingFilesPath();
        //get relevant row number
        int rowNum = new Term(indexer.getFinalTermsDictionary().get(term)).getPointerToPostingList();

        //read from posting file until reaching relevant term
        BufferedReader br = new BufferedReader(new FileReader(postingFilesPath + "\\" + term.charAt(0) + ".txt"));
        for (int i = 0; i < rowNum; i++)
            br.readLine();

        //reached relevant line, read term
        String line = br.readLine();

        //split to get data
        String[] docsPerTerm = line.split("\t");

        //create a list of docs
        ArrayList<Pair<String, TermInDocCache>> docsList = new ArrayList<>();
        for (int i = 1; i < docsPerTerm.length; i++){
            char[] docsPerTermArray = docsPerTerm[i].toCharArray();

            //get doc name hash
            String docNameHash = String.valueOf(docsPerTermArray[0]) + docsPerTermArray[1] + docsPerTermArray[2];

            StringBuilder occurrences = new StringBuilder();
            StringBuilder index = new StringBuilder();
            StringBuilder tf = new StringBuilder();

            int j;
            //find numOfOccurrencesInDoc
            for (j = 3; j < docsPerTermArray.length; j++)
                if (docsPerTermArray[j] != '*')
                    occurrences.append(docsPerTermArray[j]);
                else
                    break;

            //find firstIndexOfTermInDoc
            for (j = j + 1; j < docsPerTermArray.length; j++)
                if (docsPerTermArray[j] != '*')
                    index.append(docsPerTermArray[j]);
                else
                    break;

            //find tf
            for (j = j + 1; j < docsPerTermArray.length; j++)
                if (docsPerTermArray[j] != '*')
                    tf.append(docsPerTermArray[j]);
                else
                    break;

            //add
            docsList.add(new Pair<>(docNameHash,
                    new TermInDocCache(Integer.parseInt(
                            occurrences.toString()),
                            Integer.parseInt(index.toString()),
                            Double.parseDouble(tf.toString()))));
        }

        return docsList;
    }

}