package main;

import javafx.util.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Used to read the corpus files
 */
public class ReadFile {

    private DocNameHash docNameHash;

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

                        //looking for date
                        if (line.contains("<DATE1>")) {
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

                    //add to pairs
                    if (date.toString().length() == 0)
                        date.append("-");

                    pairs.add(new Doc(docNameHash.getHashFromDocNo(docNo.toString()), date.toString(), text.toString()));
                }

            }


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

    public int getNumOfDocs() {
        return numOfDocs;
    }

    @Override
    public String toString() {
        return numOfDocs + "";
    }

}