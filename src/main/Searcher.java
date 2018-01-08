package main;

import javafx.util.Pair;
import jdk.nashorn.internal.codegen.DumpBytecode;

import java.text.BreakIterator;
import java.util.*;

/**
 * Created by USER on 12/29/2017.
 */
public class Searcher {

    private HashSet<String> stopWords;

    Searcher(HashSet<String> stopWords) {
        this.stopWords = stopWords;
    }

    /**
     * searches for a query given as a string, and returns relevant docs
     *
     * @param query  query as a string
     * @param isStem if to use stemming or not
     * @param path   of working directory
     * @return list of docs num (maximum 50)
     */
    public ArrayList<String> search(String query, boolean isStem, String path) {
        //create parser and parse text
        Parser parser = new Parser(stopWords);
        ArrayList<String> queryTerms = parser.parse(query.toCharArray());

        //stem
        if (isStem) {
            ArrayList<String> queryTermsStemmed = new ArrayList<>();
            Stemmer stemmer = new Stemmer();

            //stem each term in query stem it
            for (int i = 0; i < queryTerms.size(); i++) {
                String s = queryTerms.get(i);
                stemmer.add(s.toCharArray(), s.length());
                stemmer.stem();
                queryTermsStemmed.add(stemmer.toString());
            }

            queryTerms = queryTermsStemmed;
            path += "\\output\\posting_files\\Stemming";
        } else
            path += "\\output\\posting_files\\No_Stemming";

        //ranking method
        Ranker ranker = new Ranker();

        ArrayList<Pair<String, Double>> rankedDocs = ranker.getRankedDocs(queryTerms, path);

        //get 50 first docs
        int rankedDocsSize = rankedDocs.size();
        ArrayList<String> docsToReturn = new ArrayList<>();
        DocNameHash docNameHash = new DocNameHash();
        for (int i = 0; i < 50 && i < rankedDocsSize; i++)
            docsToReturn.add(i + 1 + ".\t" + docNameHash.getDocNoFromHash(rankedDocs.get(i).getKey()) + "\t(Rank: " + rankedDocs.get(i).getValue() + ")");
        return docsToReturn;
    }

    /**
     * searches for multiple queries one after the other and returns relevant docs
     *
     * @param queries list of queries
     * @param isStem  if to use stem or not
     * @return hash map of query and his relevant docs
     */
    public List<Pair<String, List<String>>> search(List<Pair<String,String>> queries, boolean isStem, String path) {
        List<Pair<String, List<String>>> queriesResults = new ArrayList<>();

        //send each query to search function
        for (int i = 0; i < queries.size(); i++)
            queriesResults.add(new Pair<>(queries.get(i).getKey(), search(queries.get(i).getValue(), isStem, path)));

        return queriesResults;
    }


    /**
     * find 5 most important sentences in doc
     *
     * @param docNo doc number
     * @param path  path to docs file
     * @return list of 50 most important sentences in doc
     */
    public ArrayList<Pair<String, Integer>> find5MostImportantSentences(String docNo, String path) {
        
        Indexer indexer = new Indexer();
        DocNameHash docNameHash = new DocNameHash();
        HashMap<String, String> docsDictionary = indexer.getDocsDictionary();
        String docHash = docNameHash.getHashFromDocNo(docNo);
        String fileName = new Doc(docHash, docsDictionary.get(docHash)).getFile();

        //get text from doc
        String docText = new ReadFile().getTextFromFile(fileName, docNo, path + "\\");

        List<Pair<String,Pair<Integer, Double>>> sumTfPerSent = findSentencesImportance(docText);
        //sort list according to sumTf
        sumTfPerSent.sort((o1, o2) -> {
            if (o1.getValue().getValue() > o2.getValue().getValue())
                return -1;
            return 1;
        });

        //add 5 sentences with biggest sumTf to list
        ArrayList<Pair<String, Integer>> mostImportantSentences = new ArrayList<>();
        for (int i = 0; i < 5; i++)
            mostImportantSentences.add(new Pair<>("RANK: " + (i + 1) + "    " + sumTfPerSent.get(i).getKey(), sumTfPerSent.get(i).getValue().getKey()));

        //sort sentences according to line number
        mostImportantSentences.sort((o1, o2) -> {
            if (o1.getValue() < o2.getValue())
                return -1;
            return 1;
        });

        return mostImportantSentences;

    }

    /**
     * split text into sentences
     * @param text text to split
     * @return list of text sentences
     */
    private List<String> splitTextToSentences(String text){
        List<String> sentences = new ArrayList<>();
        //split text into sentences
        char[] textArray = text.toString().toCharArray();
        StringBuilder sentence = new StringBuilder();
        int i = 0;
        while (i < textArray.length) {
            if (textArray[i] == '.') {
                //check special cases of dot
                if (!(i > 0 && i < textArray.length - 1 && !isDigit(textArray[i - 1]) && isDigit(textArray[i + 1])) && //case of number
                        !(i > 1 && textArray[i - 2] == 'M' && (textArray[i - 1] == 'R' || textArray[i - 1] == 'S')) &&  //case of MR. of MS.
                        !(i > 1 && textArray[i - 2] == 'L' && textArray[i - 1] == 't') && //case of Lt.
                        !(i > 2 && textArray[i - 3] == 'C' && textArray[i - 2] == 'o' && textArray[i - 1] == 'l') && //case of Col.
                        !(i > 0 && textArray[i - 1] == 'U' && i < textArray.length - 2 && textArray[i + 1] == 'S' && textArray[i + 2] == '.')) {                //case of U.S.


                    sentences.add(sentence.toString());
                    sentence = new StringBuilder();
                    i++;
                }
                if (i > 0 && textArray[i - 1] == 'U' && i < textArray.length - 2 && textArray[i + 1] == 'S' && textArray[i + 2] == '.') {
                    sentence.append(textArray[i++]);
                    sentence.append(textArray[i++]);
                    sentence.append(textArray[i++]);
                }


            }
            //case of 3 spaces or tab or '\n'
            else if((textArray[i] == ' ' && textArray.length - 2 > i && textArray[i + 1] == ' ' && textArray[i + 2] == ' ') || textArray[i] == '\t'||
                        textArray[i] == '\n' && textArray.length - 1 > i && textArray[i + 1] == '\n') {
                if (sentence.toString().length() > 0) {
                    sentences.add(sentence.toString());
                    sentence = new StringBuilder();

                }
                //skip all spaces and tabs
                while (i < textArray.length && (textArray[i] == ' ' || textArray[i] == '\t'))
                    i++;
            }
            else if(textArray[i] == ';'){
                if (sentence.toString().length() > 0) {
                    sentences.add(sentence.toString());
                    sentence = new StringBuilder();
                    i++;

                }
            }
            else
                sentence.append(textArray[i++]);

        }


        return sentences;
    }

    /**
     * check if the char is a digit
     *
     * @param c char to check
     * @return true if the char is digit
     */
    private boolean isDigit(char c) {
        return c >= 48 && c <= 57;
    }

    /**
     * find sentences index in text and importance
     * @param docText text to split
     * @return List of index and rank for each sentences
     */
    private List<Pair<String,Pair<Integer, Double>>> findSentencesImportance(String docText){
        HashMap<String, Integer> termFrequency = new HashMap<>();
        List<Pair<String, Pair<Integer, Double>>> sumTfPerSent = new ArrayList<>(); //for each term save the sentence index and sumTf

        //parse per sentence
        Parser parser = new Parser(stopWords);
        List<String> terms = parser.parse(docText.toCharArray());

        //update term frequency
        int termsListSize = terms.size();
        for(int i = 0; i < termsListSize; i++) {
            String term = terms.get(i);
            int freq;
            if (!termFrequency.containsKey(term))
                freq = 1;
            else //first time of term in text
                freq = termFrequency.get(term) + 1;
            termFrequency.put(term, freq);
        }

        //get list of all sentences in doc
        //List<String> sentences = splitTextToSentences(docText);
        List<String> sentences = breakIteratorBreakToSentences(docText);



        //parse each sentence
        int sentencesListSize = sentences.size();
        for(int i = 0; i < sentencesListSize; i++) {
            List<String> termsInSentence = parser.parse(sentences.get(i).toCharArray());
            double sumTf = 0;
            if (termsInSentence.size() > 7) { //only if sentence has more than 7 terms
                for (int j = 0; j < termsInSentence.size(); j++) {
                    String term = termsInSentence.get(j);
                    if (termFrequency.containsKey(term) && termFrequency.get(term) > 4)
                        sumTf += (double) termFrequency.get(term) / terms.size();
                }
                sumTfPerSent.add(new Pair(sentences.get(i), new Pair(i, sumTf / termsInSentence.size())));
            }
        }

        return  sumTfPerSent;

    }

    /**
     * use break iterator to split text to sentences
     * @param text text to split
     * @return list of sentences
     */
    private List<String> breakIteratorBreakToSentences(String text){
        List<String> sentences = new ArrayList<>();
        BreakIterator breakIterator = BreakIterator.getSentenceInstance(Locale.US);
        breakIterator.setText(text);
        int start = breakIterator.first();
        for (int end = breakIterator.next();
             end != BreakIterator.DONE;
             start = end, end = breakIterator.next()) {
            String sentence = text.substring(start,end);
            sentences.addAll(splitTextToSentences(sentence));
        }

        return sentences;
    }





}
