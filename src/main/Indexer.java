package main;

import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by USER on 12/4/2017.
 */
public class Indexer {

    //static data structures for docs and terms
    private static HashMap<String, Pair<Integer, Integer>> termsDictionary;
    private static HashMap<String, Pair<Integer, Integer>> finalTermsDictionary;
    private static HashMap<String, Integer> docsDictionary;
    //private static HashMap<String, Pair<ArrayList<TermInDocCache>, Integer>> cache;
    //term, list of (doc, num of occurrences in doc, first index of term in doc), line number in posting
    private static HashMap<String, Pair<ArrayList<Pair<String, Pair<String, String>>>, Integer>> cache;
    private static int docsCounter = 0;
    private static int[] mostCommonTermFrequency;
    private static String[] docsDates;
    private static int[] docsLength;
    private static Parser parser;

    //constants for class
    //private String tempPostingFilesPath = "C:\\corpus\\temp_posting";
    private String tempPostingFilesPath;
    private int postingFileIndex = 5000; //starts from 5000 because there will be no more than 5000 files
    private final int termsArraysSize = 500000;
    private final int cacheDocsPerTerm = 2000;

    private Stemmer stemmer;
    private HashMap<String, String> stemmedWords;
    private int termIndex;
    //data structures for terms
    private HashMap<String, Integer> tempTermsDictionary;
    private HashMap<String, Pair<Integer, Integer>>[] postingListArray;
    private BufferedWriter tempPosting;
    private BufferedWriter docWriter;
    private HashSet<String> mostCommonTerms;
    private boolean isStemm;


    /**
     * initial all data structures and counters
     *
     * @param fileIndex index of the file
     */
    Indexer(int fileIndex, String path, boolean isStemm) {
        this.isStemm = isStemm;

        //initial static data structures
        if (docsCounter == 0) {
            termsDictionary = new HashMap<>();
            docsDictionary = new HashMap<>();
            mostCommonTermFrequency = new int[1000000];
            docsDates = new String[1000000];
            docsLength = new int[1000000];
            cache = new HashMap<>();
            finalTermsDictionary = new HashMap<>();
            parser = new Parser();
        }

        //initial class data structures and fields

        termIndex = 0;
        tempTermsDictionary = new HashMap<>();
        //termsFrequency = new int[termsArraysSize];
        //termsDf = new int[termsArraysSize];
        postingListArray = new HashMap[termsArraysSize];
        stemmedWords = new HashMap<>();
        stemmer = new Stemmer();
        if (isStemm)
            tempPostingFilesPath = path + "\\posting_files\\Stemming";
        else
            tempPostingFilesPath = path + "\\posting_files\\No_Stemming";
        try {
            tempPosting = new BufferedWriter(new FileWriter(tempPostingFilesPath + "\\" + fileIndex + ".txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    Indexer(String path, boolean isStemm) {
        this.isStemm = isStemm;
        if (isStemm)
            tempPostingFilesPath = path + "\\posting_files\\Stemming";
        else
            tempPostingFilesPath = path + "\\posting_files\\No_Stemming";
        try {
            docWriter = new BufferedWriter(new FileWriter(path + "\\doc.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        mostCommonTerms = new HashSet<>();
        // finalTermsDictionary = new HashMap<>();
        cache = new HashMap<>();

    }

    Indexer(HashMap<String, Pair<Integer, Integer>> loadDictionary, HashMap<String, Pair<ArrayList<Pair<String, Pair<String, String>>>, Integer>> loadCache) {
        termsDictionary = loadDictionary;
        cache = loadCache;
    }

    Indexer() {
    }

    /**
     * creates the index for all the terms of a file
     *
     * @param docs list of all docs in file
     */
    public void indexFile(ArrayList<Doc> docs) {
        int docsListLength = docs.size();
        for (int i = 0; i < docsListLength; i++) {
            Doc doc = docs.get(i);
            ArrayList<String> termsInFile = parser.parse(doc.getText().toCharArray());

            //add doc to dictionary
            docsDates[docsCounter] = doc.getDate();
            docsLength[docsCounter] = termsInFile.size();
            docsDictionary.put(doc.getName(), docsCounter);

            //index terms in doc
            indexTerms(termsInFile, isStemm, doc.getName());
            docsCounter++;

        }

        writeTermsDictionaryToFile();
    }

    /**
     * index terms for 1 doc
     *
     * @param stringList list of terms in doc
     * @param isStem     true if need to stem the words
     * @param docName    name of he doc
     */
    private void indexTerms(List<String> stringList, boolean isStem, String docName) {
        int listSize = stringList.size();

        //for each term in the list
        for (int i = 0; i < listSize; i++) {

            String term = stringList.get(i);
            //if stem enabled
            if (isStem) {
                //word not yet stemmed
                if (!stemmedWords.containsKey(term)) {
                    stemmer.add(term.toCharArray(), term.length());
                    stemmer.stem();
                    term = stemmer.toString();
                } else
                    term = stemmedWords.get(term);
            }

            //update term in dictionaries
            if (tempTermsDictionary.containsKey(term)) {
                int index = tempTermsDictionary.get(term);
                updateTermInDictionary(term, docName, index, i);
            } else
                updateTermInDictionary(term, docName, termIndex++, i);

            //update term frequency
            updateTermFrequency(term, docName);
        }
    }

    /**
     * update term in dictionary
     *
     * @param term             term to insert
     * @param docName          doc that contains the term
     * @param index            term index in the terms dictionary
     * @param indexOfTermInDoc term index in the doc
     */
    private void updateTermInDictionary(String term, String docName, int index, int indexOfTermInDoc) {
        //term already in the dictionary
        if (tempTermsDictionary.containsKey(term)) {
            //not the first time of the term in this doc
            if (postingListArray[index].containsKey(docName))
                postingListArray[index].put(docName, new Pair(postingListArray[index].get(docName).getKey() + 1, postingListArray[index].get(docName).getValue()));
            else { //first time of the term in the doc
                postingListArray[index].put(docName, new Pair<Integer, Integer>(1, indexOfTermInDoc));
            }
        } else { //add term to all dictionaries
            tempTermsDictionary.put(term, index);
            postingListArray[index] = new HashMap<>();
            postingListArray[index].put(docName, new Pair<Integer, Integer>(1, indexOfTermInDoc));

        }

    }

    /**
     * update term frequency
     *
     * @param term term to update
     */
    private void updateTermFrequency(String term, String docName) {
        int index = tempTermsDictionary.get(term);

        //update most frequent term
        if (mostCommonTermFrequency[docsCounter] == 0)
            mostCommonTermFrequency[docsCounter] = 1;
        else if (postingListArray[index].get(docName).getKey() > mostCommonTermFrequency[docsCounter])
            mostCommonTermFrequency[docsCounter] = postingListArray[index].get(docName).getKey();


    }

    /**
     * create temp posting for the file
     */
    private void writeTermsDictionaryToFile() {

        //sort terms
        ArrayList<String> termsList = new ArrayList<>();
        termsList.addAll(tempTermsDictionary.keySet());
        Collections.sort(termsList);
        int termsListLength = termsList.size();

        try {
            for (int i = 0; i < termsListLength; i++) {
                String term = termsList.get(i);
                int sumTf = 0;
                int numOfDocsOfTerm = 0;
                //if the term already has tf-idf
                if (termsDictionary.get(term) != null) {
                    sumTf = termsDictionary.get(term).getKey();
                    numOfDocsOfTerm = termsDictionary.get(term).getValue();
                }

                int termIndex = tempTermsDictionary.get(term);
                if (term.length() > 1) {
                    tempPosting.write(term);
                    tempPosting.write('\t');
                    //write the posting list
                    Collection<String> postingDocs = postingListArray[termIndex].keySet();
                    int counter = 0;
                    int postingDocsSize = postingDocs.size();
                    numOfDocsOfTerm += postingDocsSize;
                    for (String doc : postingDocs) {
                        counter++;
                        tempPosting.write(doc);
                        int tf = postingListArray[termIndex].get(doc).getKey();
                        tempPosting.write(tf + "");
                        tempPosting.write('*');
                        int indexInDoc = postingListArray[termIndex].get(doc).getValue();
                        tempPosting.write(indexInDoc + "");
                        if (counter < postingDocsSize)
                            tempPosting.write('\t');
                        //m.lock();
                        //sumTf += (double) tf/docsDictionary.get(doc).getKey() ; //calculate tfIdf
                        sumTf += tf;
                        //m.unlock();
                    }
                    //m.lock();
                    termsDictionary.put(term, new Pair(sumTf, numOfDocsOfTerm));
                    //m.unlock();
                    tempPosting.write('\n');

                }
            }
            tempPosting.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * write docs details to file
     */
    public int writeDocsToFile() {
        int numOfDocs = 0;
        try {
            for (String docName : docsDictionary.keySet()) {
                numOfDocs++;
                int docIndex = docsDictionary.get(docName);
                docWriter.write(docName);
                docWriter.write('\t');
                docWriter.write(docsLength[docIndex] + "");
                docWriter.write('\t');
                docWriter.write(mostCommonTermFrequency[docIndex] + "");
                docWriter.write('\t');
                docWriter.write(docsDates[docIndex]);
                docWriter.write('\n');

            }
            docWriter.close();
          /*  ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("C:\\corpus\\temp_posting_docs\\dictionary.txt"));
            out.writeObject(termsDictionary);
            out.close();*/
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numOfDocs;

    }

    /**
     * find the 10,000 most important terms in dictionary
     */
    public void findMostImportantWords() {
        int counter = 0;
        ArrayList<Term> terms = new ArrayList<>();
        for (String term : termsDictionary.keySet()) {
            Pair termPair = termsDictionary.get(term);
            if (termsDictionary.get(term).getKey() >= 3) {
                terms.add(new Term(term, (int) termPair.getKey()));
                finalTermsDictionary.put(term, termPair);
            }
        }

        Collections.sort(terms);
        //get 10,000 most important terms in the dictionary
        for (int i = 0; i < 10000; i++)
            mostCommonTerms.add(terms.get(i).getTerm());
    }

    /**
     * calculate the logarithmic value of x according to base 2
     *
     * @param x number
     * @return logarithmic value of x
     */
    private double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    /**
     * merges all the posting files into one sorted file using threads
     */
    public void mergeTempPostingFiles() {
        File directory = new File(tempPostingFilesPath);
        //get array of all files
        final File[] files = directory.listFiles();
        int filesLength = files.length;

        //one file is left
        if (filesLength == 1)
            return;

        //if odd, then make it even
        if (filesLength % 2 != 0)
            filesLength--;

        //threads
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        //ExecutorService executorService = Executors.newFixedThreadPool(1);
        Runnable[] runnables = new Runnable[filesLength];

        //set runnables
        int j = 0;
        for (int i = 0; i < filesLength; i = i + 2) {
            final int finalI = i;
            final int finalPostingFileIndex = postingFileIndex++;
            runnables[j++] = () -> {
                try {
                    mergeTwoFiles(files[finalI], files[finalI + 1], finalPostingFileIndex);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
        }

        //run them together
        for (int i = 0; i < j; i++)
            executorService.execute(runnables[i]);

        //wait for them to finish
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //merge again, recursively
        mergeTempPostingFiles();
    }

    /**
     * gets two posting files and index and merges them into a new file based on which line is smaller
     *
     * @param f1       file 1
     * @param f2       file 2
     * @param fileName
     * @throws IOException exception
     */
    private void mergeTwoFiles(File f1, File f2, int fileName) throws IOException {
        //get both files
        BufferedReader br1 = new BufferedReader(new FileReader(f1));
        BufferedReader br2 = new BufferedReader(new FileReader(f2));

        //write to new file
        BufferedWriter bw = new BufferedWriter(new FileWriter(tempPostingFilesPath + "\\" + fileName + ".txt"));

        //first lines
        String line1 = br1.readLine();
        String line2 = br2.readLine();

        //arrays
        String[] line1Split = splitTermLine(line1);
        String[] line2Split = splitTermLine(line2);

        //run through the files line by line
        while (line1 != null && line2 != null) {
            //if term is equal, need to merge
            int compareAnswer = line1Split[0].compareTo(line2Split[0]);
            if (compareAnswer == 0) {
                if (finalTermsDictionary.containsKey(line1Split[0])) {
                    bw.write(line1Split[0] + '\t' + line1Split[1] + '\t' + line2Split[1] + '\n');
                }
                //next lines please
                line1 = br1.readLine();
                line2 = br2.readLine();
                if (line1 != null) line1Split = splitTermLine(line1);
                if (line2 != null) line2Split = splitTermLine(line2);
            }
            //term 1 is smaller
            else if (compareAnswer < 0) {
                if (finalTermsDictionary.containsKey(line1Split[0])) {
                    bw.write(line1 + '\n');
                }
                line1 = br1.readLine();
                //next line please
                if (line1 != null) line1Split = splitTermLine(line1);

            }
            //term 2 is smaller
            else {
                if (finalTermsDictionary.containsKey(line2Split[0])) {
                    bw.write(line2 + '\n');
                }
                line2 = br2.readLine();
                //next line please
                if (line2 != null) line2Split = splitTermLine(line2);
            }
        }


        //check if only one file is left, get him all
        while (line1 != null) {
            bw.write(line1 + '\n');
            line1 = br1.readLine();
        }
        while (line2 != null) {
            bw.write(line2 + '\n');
            line2 = br2.readLine();
        }

        //close connection and delete files
        br1.close();
        f1.delete();
        br2.close();
        f2.delete();
        bw.close();
    }

    /**
     * gets a line of a term and returns a string array divided into relevant information
     *
     * @param line line of term
     * @return string array where index 0 is term and index 1 is the rest of the line
     */
    private String[] splitTermLine(String line) {
        //divide to term and the rest
        StringBuilder term = new StringBuilder();

        //convert to chars
        char[] lineCharsArray = line.toCharArray();
        int lineCharsArrayLength = lineCharsArray.length;

        //get everything
        int i = 0;

        //term
        while (i < lineCharsArrayLength) {
            if (lineCharsArray[i] != '\t') {
                term.append(lineCharsArray[i]);
                i++;
            } else {
                i++;
                break;
            }
        }

        //return, including the rest of the line
        return new String[]{term.toString(), line.substring(i, lineCharsArrayLength)};

    }

    public void splitPostingThreads() throws IOException {
        File directory = new File(tempPostingFilesPath);
        //get posting file (the only one in the folder)
        File postingFile = Objects.requireNonNull(directory.listFiles())[0];
        BufferedReader br = new BufferedReader(new FileReader(postingFile));

        //strings
        ArrayList<ArrayList<String>> strings = new ArrayList<>();

        //run through the posting file line by line
        String line;
        char c = '0';
        ArrayList<String> strings0 = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            if (line.charAt(0) == c)
                strings0.add(line);
            else {
                strings.add(strings0);
                strings0 = new ArrayList<>();
                c = line.charAt(0);
                strings0.add(line);
            }
        }
        br.close();

        //threads
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        //create writers for all the files
        HashMap<Character, BufferedWriter> bufferedWriterHashMap = new HashMap<>();
        HashMap<Character, Integer> bufferedWriterLineNumberHashMap = new HashMap<>();
        char[] abc = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
                'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
        int abcLength = abc.length;
        for (int i = 0; i < abcLength; i++) {
            bufferedWriterHashMap.put(abc[i], new BufferedWriter(new FileWriter(tempPostingFilesPath + "\\" + abc[i] + ".txt")));
            bufferedWriterLineNumberHashMap.put(abc[i], 0);
        }
        Runnable[] runnables = new Runnable[abcLength];

        for (int i = 0; i < abcLength; i++){
            int finalI = i;
            runnables[i] = () -> {
                BufferedWriter bufferedWriter = bufferedWriterHashMap.get(abc[finalI]);
                ArrayList<String> strings1 = strings.get(finalI);
                int size = strings1.size();
                for (int j = 0; j < size; j++) {
                    try {
                        bufferedWriter.write(strings1.get(j) + '\n');
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
        }

        //run them together
        for (int i = 0; i < runnables.length; i++)
            executorService.execute(runnables[i]);

        //wait for them to finish
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        br.close();

    }

    /**
     * gets a posting file directory and splits the file to files from A-Z and 0-9 sorted
     * by terms in doc occurrences, while building the cache and updating the dictionary
     *
     * @throws IOException exception
     */
    public void splitPostingSortCreateCacheAndUpdateDictionary() throws IOException {
        File directory = new File(tempPostingFilesPath);
        //get posting file (the only one in the folder)
        File postingFile = Objects.requireNonNull(directory.listFiles())[0];
        BufferedReader br = new BufferedReader(new FileReader(postingFile));

        //create writers for all the files
        HashMap<Character, BufferedWriter> bufferedWriterHashMap = new HashMap<>();
        HashMap<Character, Integer> bufferedWriterLineNumberHashMap = new HashMap<>();
        char[] abc = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
                'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
        int abcLength = abc.length;
        for (int i = 0; i < abcLength; i++) {
            bufferedWriterHashMap.put(abc[i], new BufferedWriter(new FileWriter(tempPostingFilesPath + "\\" + abc[i] + ".txt")));
            bufferedWriterLineNumberHashMap.put(abc[i], 0);
        }

        //run through the posting file line by line
        String line;
        while ((line = br.readLine()) != null) {
            //split line to get term and the rest
            String[] lineSplit = splitTermLine(line);

            //check if term is not in final dictionary
            if (!finalTermsDictionary.containsKey(lineSplit[0]))
                continue;

            //writer and start of line
            char c = lineSplit[0].charAt(0);
            BufferedWriter bw = bufferedWriterHashMap.get(c);
            if (!bufferedWriterLineNumberHashMap.containsKey(c))
                continue;
            int bwln = bufferedWriterLineNumberHashMap.get(c);

            //if term is in top 10K
            if (mostCommonTerms.contains(lineSplit[0])) {
                //split by tab
                String[] docsInLine = lineSplit[1].split("\t");
                int docsInLineLength = docsInLine.length;

                //if there are more than cacheDocsPerTerm docs in line then need to store some in posting
                if (docsInLineLength > cacheDocsPerTerm) {
                    //write term
                    //bw.write(lineSplit[0] + '\t');

                    //write docs
                    for (int i = cacheDocsPerTerm; i < docsInLineLength; i++)
                        bw.write(docsInLine[i] + '\t');

                    //write df
                    bw.write(docsInLineLength + '\n');

                    //get list of cacheDocsPerTerm terms in docs and insert to cache, reference to posting
                    cache.put(lineSplit[0], new Pair<>(getSortedListOfDocsPerTerm(lineSplit[0], docsInLine, docsInLineLength, cacheDocsPerTerm), bwln));

                    //update line number
                    bufferedWriterLineNumberHashMap.put(c, bwln + 1);
                } else {

                    //get list of cacheDocsPerTerm - 1 or less terms in docs and insert to cache, no reference to posting
                    cache.put(lineSplit[0], new Pair<>(getSortedListOfDocsPerTerm(lineSplit[0], docsInLine, docsInLineLength, cacheDocsPerTerm), -1));
                }

                //add to dictionary
                Pair<Integer, Integer> pair = finalTermsDictionary.get(lineSplit[0]);
                finalTermsDictionary.put(lineSplit[0], new Pair<>(pair.getKey(), -1));
            }
            //term is not in top 10k, need to write again, but now sorted
            else {
                //split by tab
                String[] docsInLine = lineSplit[1].split("\t");
                int docsInLineLength = docsInLine.length;

                //write term
                //bw.write(lineSplit[0]);
                //bw.write('\t');

                //write df
                bw.write("" + docsInLineLength);
                bw.write('\t');

                //get list of all
                ArrayList<Pair<String, Pair<String, String>>> termInDocCaches = getSortedListOfDocsPerTerm(lineSplit[0], docsInLine, docsInLineLength, Integer.MAX_VALUE);
                int termInDocCachesSize = termInDocCaches.size();

                //go through the list and write
                for (int i = 0; i < termInDocCachesSize; i++) {
                    //TermInDocCache termInDocCache = termInDocCaches.get(i);
                    Pair<String, Pair<String, String>> pair = termInDocCaches.get(i);
                    //bw.write(termInDocCache.getDocFile() + termInDocCache.getNumOfOccurrencesInDoc() + '*' + termInDocCache.getFirstIndexOfTermInDoc());
                    bw.write(pair.getKey() + pair.getValue().getKey() + '*' + pair.getValue().getValue());

                    //add tab if not last
                    if (i < termInDocCachesSize - 1)
                        bw.write('\t');
                }

                //new line
                bw.write('\n');

                //add to dictionary
                Pair<Integer, Integer> pair = finalTermsDictionary.get(lineSplit[0]);
                finalTermsDictionary.put(lineSplit[0], new Pair<>(pair.getKey(), bwln));

                //update line number
                bufferedWriterLineNumberHashMap.put(c, bwln + 1);
            }
        }

        //delete posting file
        br.close();
        postingFile.delete();


    }

    /**
     * gets an array of strings containing the docs per term, returns an array list of terms in docs for cache
     *
     * @param term    the term
     * @param docs    array of docs
     * @param length  length of array
     * @param howMany defines how many terms to return
     * @return array list of terms in docs
     */
    private ArrayList<Pair<String, Pair<String, String>>> getSortedListOfDocsPerTerm(String term, String[] docs, int length, int howMany) {
        ArrayList<Pair<String, Pair<String, String>>> termInDocCaches = new ArrayList<>();

        for (int i = 0; i < length && i < howMany; i++) {
            char[] docCharsArray = docs[i].toCharArray();

            //doc name
            StringBuilder docName = new StringBuilder();
            docName.append(docCharsArray[0]);
            docName.append(docCharsArray[1]);
            docName.append(docCharsArray[2]);

            StringBuilder occurrences = new StringBuilder();
            StringBuilder index = new StringBuilder();
            for (int j = 3; j < docCharsArray.length; j++) {
                char c;
                //find numOfOccurrencesInDoc
                if ((c = docCharsArray[j]) != '*') {
                    occurrences.append(c);
                    continue;
                }
                //find firstIndexOfTermInDoc
                for (int k = j + 1; k < docCharsArray.length; k++)
                    index.append(docCharsArray[k]);
                break;
            }

            //write term
            //termInDocCaches.add(new TermInDocCache(docName.toString(), Integer.parseInt(occurrences.toString()), Integer.parseInt(index.toString())));
            termInDocCaches.add(new Pair<>(docName.toString(), new Pair<>(occurrences.toString(), index.toString())));
        }

        //sort terms
        termInDocCaches.sort((o1, o2) -> o2.getValue().getKey().compareTo(o1.getValue().getKey()));

        //return
        return termInDocCaches;
    }

    /**
     * write dictionary to file
     */
    public void writeDictionaryToFile(String path) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(path));
            for (String term : finalTermsDictionary.keySet()) {
                Pair<Integer, Integer> pair = finalTermsDictionary.get(term);
                bw.write(term + '\t' + pair.getKey() + '\t' + pair.getValue() + '\n');
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void writeCacheToFile(String path) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(path));
            for (String term : cache.keySet()) {
                Pair<ArrayList<Pair<String, Pair<String, String>>>, Integer> pair = cache.get(term);
                ArrayList<Pair<String, Pair<String, String>>> docs = pair.getKey();
                bw.write(term + '\t' + pair.getValue() + '\t');
                int docsSize = docs.size();
                for (int i = 0; i < docsSize; i++) {
                    //TermInDocCache doc = docs.get(i);
                    Pair<String, Pair<String, String>> pair1 = docs.get(i);
                    //bw.write(doc.getDocFile() + doc.getNumOfOccurrencesInDoc() + "*" + doc.getFirstIndexOfTermInDoc());
                    bw.write(pair1.getKey() + pair1.getValue().getKey() + '*' + pair1.getValue().getValue());
                    if (i + 1 < docsSize)
                        bw.write('\t');
                }
                bw.write('\n');
            }
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetStaticFields() {
        if (docsCounter > 0) {
            termsDictionary.clear();
            finalTermsDictionary.clear();
            docsDictionary.clear();
            cache.clear();
            docsCounter = 0;
            mostCommonTermFrequency = new int[1000000];
        }
    }

    /**
     * sort the cache by a-z
     *
     * @return array list of terms and information
     */
    public ArrayList<String> getCacheSorted() {
        //array list
        ArrayList<String> strings = new ArrayList<>();

        //hash
        DocNameHash docNameHash = new DocNameHash();

        //run through the cache
        for (String term : cache.keySet()) {
            StringBuilder stringBuilder = new StringBuilder();

            //term
            stringBuilder.append(term + "\t\t\t");

            //ArrayList<TermInDocCache> termInDocCaches = cache.get(term).getKey();
            ArrayList<Pair<String, Pair<String, String>>> termInDocCaches = cache.get(term).getKey();

            //run through all docs
            int termInDocCachesSize = termInDocCaches.size();
            for (int i = 0; i < termInDocCachesSize; i++) {
                //TermInDocCache termInDocCache = termInDocCaches.get(i);
                Pair<String, Pair<String, String>> termInDocCache = termInDocCaches.get(i);
                stringBuilder.append("\t(Doc: " + docNameHash.getDocNoFromHash(termInDocCache.getKey()) +
                        "\tAmount in doc: " + termInDocCache.getValue().getKey() +
                        "\tFirst index of term in doc: " + termInDocCache.getValue().getValue() + "),");
            }

            //add to list
            strings.add(stringBuilder.toString());
        }

        //sort
        Collections.sort(strings);

        return strings;
    }

    /**
     * sort dictionary and return an array to show
     *
     * @return array of dictionary terms and their information
     */
    public ArrayList<String> getDictionarySorted() {
        //array list
        ArrayList<String> strings = new ArrayList<>();

        //run through the dictionary
        for (String term : finalTermsDictionary.keySet())
            //add to list
            strings.add(term + "\t\t\tAmount in corpus: " + finalTermsDictionary.get(term).getKey());

        //sort
        Collections.sort(strings);

        return strings;
    }
}