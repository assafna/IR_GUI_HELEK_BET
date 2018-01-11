package main;

import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Used to index
 */
public class Indexer {

    //static data structures for docs and terms
    private static HashMap<String, Pair<Integer, Integer>> termsDictionary; //in start - term, sumTf, numOfDocsForTerm -/- after final posting creation - term, df, pointerToPostingList
    private static HashMap<String, String> finalTermsDictionary;
    private static HashMap<String, String> docsDictionary;
    private static HashMap<String, Integer> mostCommonTermFrequency;
    private static HashMap<String, Double> docsWights;
    private static HashMap<String, Pair<ArrayList<String>, Integer>> cache; //term, list of docs, row num in posting
    private static int docsCounter = 0;
    private static Parser parser;
    private static double avgLengthOfDocs;
    private boolean useThreads = false;

    //constants for class
    private String tempPostingFilesPath;
    private int postingFileIndex = 5000; //starts from 5000 because there will be no more than 5000 files
    private final int termsArraysSize = 500000;
    private final int cacheDocsPerTerm = 200;

    private Stemmer stemmer;
    private HashMap<String, String> stemmedWords;
    private int termIndex;
    //data structures for terms
    private HashMap<String, Integer> tempTermsDictionary;
    private HashMap<String, Pair<Integer, Double>>[] postingListArray;
    private HashMap<String, Double>[] tfPerDoc;
    private BufferedWriter tempPosting;
    private BufferedWriter docWriter;
    private HashSet<String> mostCommonTerms;
    private boolean isStem;

    /**
     * initial all data structures and counters
     *
     * @param fileIndex index of the file
     */
    Indexer(int fileIndex, String path, boolean isStem) {
        this.isStem = isStem;

        //initial static data structures
        if (docsCounter == 0) {
            termsDictionary = new HashMap<>();
            docsDictionary = new HashMap<>();
            cache = new HashMap<>();
            finalTermsDictionary = new HashMap<>();
            mostCommonTermFrequency = new HashMap<>();
            docsWights = new HashMap<>();
            parser = new Parser();
        }

        //initial class data structures and fields
        termIndex = 0;
        tempTermsDictionary = new HashMap<>();
        postingListArray = new HashMap[termsArraysSize];
        tfPerDoc = new HashMap[termsArraysSize];
        stemmedWords = new HashMap<>();
        stemmer = new Stemmer();
        if (isStem)
            tempPostingFilesPath = path + "\\posting_files\\Stemming";
        else
            tempPostingFilesPath = path + "\\posting_files\\No_Stemming";
        try {
            tempPosting = new BufferedWriter(new FileWriter(tempPostingFilesPath + "\\" + fileIndex + ".txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    Indexer(String path, boolean isStem) {
        this.isStem = isStem;
        if (isStem)
            tempPostingFilesPath = path + "\\posting_files\\Stemming";
        else
            tempPostingFilesPath = path + "\\posting_files\\No_Stemming";

        mostCommonTerms = new HashSet<>();
        // finalTermsDictionary = new HashMap<>();
        cache = new HashMap<>();

    }

    Indexer(HashMap<String, String> loadDictionary, HashMap<String, Pair<ArrayList<String>,
            Integer>> loadCache, HashMap<String, String> loadDocs, int numOfDocs, double avgLengthOfDoc) {
        this.finalTermsDictionary = loadDictionary;
        this.cache = loadCache;
        this.docsDictionary = loadDocs;
        docsCounter = numOfDocs;
        avgLengthOfDocs = avgLengthOfDoc;
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
            docsDictionary.put(doc.getCode(), getDocString(doc, termsInFile.size()));

            //index terms in doc
            indexTerms(termsInFile, isStem, doc.getCode());
            calculateTfPerTerm(doc.getCode(), termsInFile);
            docsCounter++;

        }

        writeTermsDictionaryToFile();
    }


    /**
     * create doc string
     *
     * @param doc    doc
     * @param length doc length
     * @return string of the doc
     */
    private String getDocString(Doc doc, int length) {
        StringBuilder sb = new StringBuilder();
        //sb.append(doc.getName() + '\t');
        //sb.append(doc.getCode() + '\t');
        sb.append(length + "" + '\t');
        sb.append(doc.getDate() + '\t');
        sb.append(doc.getFile());

        return sb.toString();
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
        avgLengthOfDocs += listSize;
        //for each term in the list
        for (int i = 0; i < listSize; i++) {

            String term = stringList.get(i);
            //if stem enabled
            if (isStem) {
                //word not yet stemmed
                if (!stemmedWords.containsKey(term)) {
                    stemmer.add(term.toCharArray(), term.length());
                    stemmer.stem();
                    String prevTerm = term;
                    term = stemmer.toString();
                    stemmedWords.put(prevTerm, term);
                } else
                    term = stemmedWords.get(term);
            }

            //update term in dictionaries
            if (tempTermsDictionary.containsKey(term)) {
                int index = tempTermsDictionary.get(term);
                updateTermInDictionary(term, docName, index, (double) (i+1) / listSize);
            } else
                updateTermInDictionary(term, docName, termIndex++, (double) (i+1) / listSize);

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
    private void updateTermInDictionary(String term, String docName, int index, double indexOfTermInDoc) {
        //term already in the dictionary
        if (tempTermsDictionary.containsKey(term)) {
            //not the first time of the term in this doc
            if (postingListArray[index].containsKey(docName))
                postingListArray[index].put(docName, new Pair(postingListArray[index].get(docName).getKey() + 1, postingListArray[index].get(docName).getValue()));
            else { //first time of the term in the doc
                postingListArray[index].put(docName, new Pair(1, indexOfTermInDoc));
            }
        } else { //add term to all dictionaries
            tempTermsDictionary.put(term, index);
            postingListArray[index] = new HashMap<>();
            tfPerDoc[index] = new HashMap<>();
            postingListArray[index].put(docName, new Pair(1, indexOfTermInDoc));

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
        if (!mostCommonTermFrequency.containsKey(docName))
            mostCommonTermFrequency.put(docName, 1);
        else if (postingListArray[index].get(docName).getKey() > mostCommonTermFrequency.get(docName))
            mostCommonTermFrequency.put(docName, postingListArray[index].get(docName).getKey());


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
                //if the term already in dictionary
                if (termsDictionary.get(term) != null) {
                    sumTf = termsDictionary.get(term).getKey();
                    numOfDocsOfTerm = termsDictionary.get(term).getValue();
                }

                int indexTerm = tempTermsDictionary.get(term);
                if (term.length() > 1) {
                    tempPosting.write(term);
                    tempPosting.write('\t');
                    //write the posting list
                    Collection<String> postingDocs = postingListArray[indexTerm].keySet();
                    int counter = 0;
                    int postingDocsSize = postingDocs.size();
                    numOfDocsOfTerm += postingDocsSize;
                    for (String doc : postingDocs) {
                        counter++;
                        tempPosting.write(doc);
                        int tf = postingListArray[indexTerm].get(doc).getKey();
                        tempPosting.write(tf + "*");
                        double indexInDoc = postingListArray[indexTerm].get(doc).getValue();
                        tempPosting.write(indexInDoc + "*");
                        tempPosting.write(tfPerDoc[indexTerm].get(doc) + "");
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
     * find the 10,000 most important terms in dictionary
     */
    public void findMostImportantWords() {
        int counter = 0;
        ArrayList<Term> terms = new ArrayList<>();
        for (String term : termsDictionary.keySet()) {
            int termSumTf = termsDictionary.get(term).getKey();
            if (termsDictionary.get(term).getKey() >= 3) {
                Term t = new Term(term, termSumTf);
                terms.add(t);
                finalTermsDictionary.put(term, t.toString());
            }
        }
        termsDictionary.clear();

        Collections.sort(terms);
        //get 10,000 most important terms in the dictionary
        for (int i = 0; i < 10000 && i < terms.size(); i++)
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
        if (useThreads){
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

        }
        //no threads
        else {
            for (int i = 0; i < filesLength; i = i + 2) {
                final int finalPostingFileIndex = postingFileIndex++;
                try {
                    mergeTwoFiles(files[i], files[i + 1], finalPostingFileIndex);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //merge again, recursively
        mergeTempPostingFiles();
    }

    /**
     * gets two posting files and index and merges them into a new file based on which line is smaller
     *
     * @param f1       file 1
     * @param f2       file 2
     * @param fileName file name decided ahead
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
                //   if (finalTermsDictionary.containsKey(line1Split[0])) {
                //term tab line1 tab line 2
                bw.write(line1Split[0] + '\t' + line1Split[1] + '\t' + line2Split[1] + '\n');
                //    }
                //next lines please
                line1 = br1.readLine();
                line2 = br2.readLine();
                if (line1 != null) line1Split = splitTermLine(line1);
                if (line2 != null) line2Split = splitTermLine(line2);
            }
            //term 1 is smaller
            else if (compareAnswer < 0) {
                //  if (finalTermsDictionary.containsKey(line1Split[0])) {
                bw.write(line1 + '\n');
                //   }
                line1 = br1.readLine();
                //next line please
                if (line1 != null) line1Split = splitTermLine(line1);

            }
            //term 2 is smaller
            else {
                //  if (finalTermsDictionary.containsKey(line2Split[0])) {
                bw.write(line2 + '\n');
                //  }
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

        //previous char
        char prev = '0';
        System.out.println("Started character: 0");

        //run through the posting file line by line
        String line;
        while ((line = br.readLine()) != null) {
            //split line to get term and the rest
            String[] lineSplit = splitTermLine(line);
            String lineSplit0 = lineSplit[0];
            String lineSplit1 = lineSplit[1];

            //check if term is not in final dictionary
            if (!finalTermsDictionary.containsKey(lineSplit0))
                continue;

            //writer and start of line
            char c = lineSplit0.charAt(0);

            if (!bufferedWriterLineNumberHashMap.containsKey(c))
                continue;

            //prev
            if (c != prev) {
                bufferedWriterHashMap.get(prev).close();
                prev = c;
                System.out.println("Started character: " + c);
            }

            BufferedWriter bw = bufferedWriterHashMap.get(c);
            int bwln = bufferedWriterLineNumberHashMap.get(c);

            //if term is in top 10K
            if (mostCommonTerms.contains(lineSplit0)) {
                //split by tab
                String[] docsInLine = lineSplit1.split("\t");
                int docsInLineLength = docsInLine.length;

                //if there are more than cacheDocsPerTerm docs in line then need to store some in posting
                if (docsInLineLength > cacheDocsPerTerm) {
                    //write term
                    //bw.write(lineSplit[0] + '\t');

                    //get list of all sorted
                    ArrayList<String> termInDocCaches = getSortedListOfDocsPerTerm(docsInLine, docsInLineLength, Integer.MAX_VALUE);
                    int termInDocCachesSize = termInDocCaches.size();

                    //go through the list and write
                    for (int i = cacheDocsPerTerm; i < termInDocCachesSize; i++) {
                        TermInDocCache termInDocCache = new TermInDocCache(termInDocCaches.get(i));
                        bw.write(termInDocCache.getDocName() + termInDocCache.getNumOfOccurrencesInDoc() + '*' + termInDocCache.getIndexOfFirstOccurrence() + '*' + termInDocCache.getTf());

                        //add tab if not last
                        if (i < termInDocCachesSize - 1)
                            bw.write('\t');
                    }

                    //write df
                    bw.write("" + docsInLineLength + '\n');

                    //get all the rest to cache
                    ArrayList<String> forCache = new ArrayList<>();
                    for (int i = 0; i < cacheDocsPerTerm; i++)
                        forCache.add(i, termInDocCaches.get(i));

                    //get list of cacheDocsPerTerm terms in docs and insert to cache, reference to posting
                    cache.put(lineSplit0, new Pair<>(forCache, bwln));

                    //update line number
                    bufferedWriterLineNumberHashMap.put(c, bwln + 1);
                } else {
                    //get list of cacheDocsPerTerm - 1 or less terms in docs and insert to cache, no reference to posting
                    cache.put(lineSplit0, new Pair<>(getSortedListOfDocsPerTerm(docsInLine, docsInLineLength, Integer.MAX_VALUE), -1));
                }

                //add to dictionary
                updateTermInFinalDictionary(lineSplit0, docsInLineLength, -1);
            }
            //term is not in top 10k, need to write again, but now sorted
            else {
                //split by tab
                String[] docsInLine = lineSplit1.split("\t");
                int docsInLineLength = docsInLine.length;

                //write term
                //bw.write(lineSplit[0]);
                //bw.write('\t');

                //write df
                bw.write("" + docsInLineLength);
                bw.write('\t');

                //get list of all
                ArrayList<String> termInDocCaches = getSortedListOfDocsPerTerm(docsInLine, docsInLineLength, Integer.MAX_VALUE);
                int termInDocCachesSize = termInDocCaches.size();

                //go through the list and write
                for (int i = 0; i < termInDocCachesSize; i++) {
                    TermInDocCache termInDocCache = new TermInDocCache(termInDocCaches.get(i));
                    bw.write(termInDocCache.getDocName() + termInDocCache.getNumOfOccurrencesInDoc() + '*' + termInDocCache.getIndexOfFirstOccurrence() + '*' + termInDocCache.getTf());

                    //add tab if not last
                    if (i < termInDocCachesSize - 1)
                        bw.write('\t');
                }

                //new line
                bw.write('\n');

                //add to dictionary
                updateTermInFinalDictionary(lineSplit0, docsInLineLength, bwln);

                //update line number
                bufferedWriterLineNumberHashMap.put(c, bwln + 1);
            }
        }

        //delete posting file
        br.close();
        postingFile.delete();

    }

    /**
     * update term in the final dictionary
     *
     * @param termName term name
     * @param df       term df
     * @param pointer  pointer to posting list. -1 if the term in the cache
     */
    private void updateTermInFinalDictionary(String termName, int df, int pointer) {
        Term term = new Term(finalTermsDictionary.get(termName));
        term.setDf(df);
        term.setIdf(log2((double) docsCounter / df));
        term.setPointerToPostingList(pointer);
        finalTermsDictionary.put(termName, term.toString());
    }

    /**
     * Gets an array of strings containing the docs per term, returns an array list of terms in docs for cache.
     * Update sum squared wight per doc.
     *
     * @param docs    array of docs
     * @param length  length of array
     * @param howMany defines how many terms to return
     * @return array list of terms in docs
     */
    private ArrayList<String> getSortedListOfDocsPerTerm(String[] docs, int length, int howMany) {
        ArrayList<Pair<String, Integer>> termInDocCaches = new ArrayList<>();
        double idf = log2((double) docsCounter / length);
        String docName = "";
        for (int i = 0; i < length && i < howMany; i++) {
            char[] docCharsArray = docs[i].toCharArray();

            //doc name
            docName = String.valueOf(docCharsArray[0]) +
                    docCharsArray[1] +
                    docCharsArray[2];

            StringBuilder occurrences = new StringBuilder();
            StringBuilder index = new StringBuilder();
            StringBuilder tf = new StringBuilder();

            int j;
            //find numOfOccurrencesInDoc
            for (j = 3; j < docCharsArray.length; j++)
                if (docCharsArray[j] != '*')
                    occurrences.append(docCharsArray[j]);
                else
                    break;

            //find firstIndexOfTermInDoc
            for (j = j + 1; j < docCharsArray.length; j++)
                if (docCharsArray[j] != '*')
                    index.append(docCharsArray[j]);
                else
                    break;

            //find tf
            for (j = j + 1; j < docCharsArray.length; j++)
                if (docCharsArray[j] != '*')
                    tf.append(docCharsArray[j]);
                else
                    break;

            //write term
            Double Tf = Double.parseDouble(tf.toString());
            termInDocCaches.add(new Pair<>(new TermInDocCache(docName, Integer.parseInt(occurrences.toString()), Double.parseDouble(index.toString()), Tf).toString(), Integer.parseInt(occurrences.toString())));

            //update term wight in doc in doc sum squared wight
            //Pair<Integer, Double> docPair = mostCommonTermFrequencyAndDocWight.get(docName);
            double wight = Math.pow(Tf * idf, 2);
            if (!docsWights.containsKey(docName))
                docsWights.put(docName, wight);
            else
                docsWights.put(docName, (double) docsWights.get(docName) + wight);

        }

        //update doc string in dictionary
        //docsDictionary.put(docName, docsDictionary.get(docName)+ '\t' + mostCommonTermFrequency.get(docName) + '\t' + docsWights.get(docName));

        //sort terms
        termInDocCaches.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        //create new list
        ArrayList<String> strings = new ArrayList<>();
        int termInDocCachesSize = termInDocCaches.size();
        for (int i = 0; i < termInDocCachesSize; i++)
            strings.add(termInDocCaches.get(i).getKey());

        //return
        return strings;
    }


    /**
     * write dictionary to file
     */
    public void writeDictionaryToFile(String path) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(path));
            for (String term : finalTermsDictionary.keySet()) {
                bw.write(finalTermsDictionary.get(term) + '\n');
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * write cache object to file
     *
     * @param path path of the file
     */
    public void writeCacheToFile(String path) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(path));
            for (String term : cache.keySet()) {
                Pair<ArrayList<String>, Integer> pair = cache.get(term);
                ArrayList<String> cacheDocsForTerms = pair.getKey();
                bw.write(term + '\t' + pair.getValue() + '\t');
                int docsSize = cacheDocsForTerms.size();
                for (int i = 0; i < docsSize; i++) {
                    TermInDocCache termInDocCache = new TermInDocCache(cacheDocsForTerms.get(i));
                    bw.write(termInDocCache.getDocName() + termInDocCache.getNumOfOccurrencesInDoc() + '*' + termInDocCache.getIndexOfFirstOccurrence() + '*' + termInDocCache.getTf());
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

    /**
     * write docs details to file
     */
    public int writeDocsToFile(String path) {
        int numOfDocs = 0;
        try {
            docWriter = new BufferedWriter(new FileWriter(path));
            docWriter.write(docsCounter + "\t" + (double)avgLengthOfDocs/docsCounter + "\n");
            for (String doc : docsDictionary.keySet()) {
                numOfDocs++;
                docWriter.write(doc + '\t');
                docWriter.write(docsDictionary.get(doc));
                docWriter.write('\t');
                docWriter.write(mostCommonTermFrequency.get(doc) + "");
                docWriter.write('\t');
                docWriter.write(docsWights.get(doc) + "");
                docWriter.write('\n');
            }

            docWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numOfDocs;

    }

    /**
     * reset dictionaries and static values
     */
    public void resetStaticFields() {
        if (docsCounter > 0) {
            termsDictionary.clear();
            finalTermsDictionary.clear();
            docsDictionary.clear();
            mostCommonTermFrequency.clear();
            docsWights.clear();
            cache.clear();
            docsCounter = 0;
            //mostCommonTermFrequency = new int[1000000];
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
            ArrayList<String> termInDocCaches = cache.get(term).getKey();

            //run through all docs
            int termInDocCachesSize = termInDocCaches.size();
            for (int i = 0; i < termInDocCachesSize && i < 100; i++) {
                TermInDocCache termInDocCache = new TermInDocCache(termInDocCaches.get(i));
                stringBuilder.append("\t(Doc: " + docNameHash.getDocNoFromHash(termInDocCache.getDocName()) +
                        "\tAmount in doc: " + termInDocCache.getNumOfOccurrencesInDoc() +
                        "\tFirst index of term in doc: " + termInDocCache.getIndexOfFirstOccurrence() +
                        "\tTf: " + termInDocCache.getTf() + "),");
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
            strings.add(term + "\t\t\tAmount in corpus: " + new Term(finalTermsDictionary.get(term)).getSumTf());

        //sort
        Collections.sort(strings);

        return strings;
    }

    /**
     * calculate tf for each term in each doc
     *
     * @param docNo       doc name
     * @param termsInFile list of terms in file
     */
    private void calculateTfPerTerm(String docNo, List<String> termsInFile) {
        try {
            int docLength = termsInFile.size();
            HashSet<String> duplicateTerms = new HashSet<>();
            for (int i = 0; i < docLength; i++) {
                String term = stemmedWords.get(termsInFile.get(i));
                if (!duplicateTerms.contains(term)) {
                    duplicateTerms.add(term);
                    int index = tempTermsDictionary.get(term);
                    double tfNormToDocLength = (double) postingListArray[index].get(docNo).getKey() / docLength;
                    tfPerDoc[index].put(docNo, tfNormToDocLength);

                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public HashMap<String, String> getFinalTermsDictionary() {
        return finalTermsDictionary;
    }

    public HashMap<String, String> getDocsDictionary() {
        return docsDictionary;
    }

    public HashMap<String, Pair<ArrayList<String>, Integer>> getCache() {
        return cache;
    }

    public String getTempPostingFilesPath() {
        return tempPostingFilesPath;
    }

    public static int getDocsCounter() {
        return docsCounter;
    }

    public static void setDocsCounter(int docsCounter) {
        Indexer.docsCounter = docsCounter;
    }

    public static double getAvgLengthOfDocs() {
        return avgLengthOfDocs;
    }

    public static void setAvgLengthOfDocs(double avgLengthOfDocs) {
        Indexer.avgLengthOfDocs = avgLengthOfDocs;
    }
}