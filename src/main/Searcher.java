package main;

import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by USER on 12/29/2017.
 */
public class Searcher {

    private HashSet<String> stopWords = new HashSet();

    public Searcher(HashSet<String> stopWords){
        this.stopWords = stopWords;
    }

    /**
     * searches for a query given as a string, and returns relevant docs
     *
     * @param query  query as a string
     * @param isStem if to use stemming or not
     * @return list of docs num (maximum 50)
     */
    public List<String> search(String query, boolean isStem) {
        //create parser and parse text
        Parser parser = new Parser(stopWords);
        List<String> queryTerms = parser.parse(query.toCharArray());

        //stem
        if (isStem) {
            List<String> queryTermsStemmed = new ArrayList<>();
            Stemmer stemmer = new Stemmer();

            //stem each term
            for (int i = 0; i < queryTerms.size(); i++) {
                String s = queryTerms.get(i);
                stemmer.add(s.toCharArray(), s.length());
                stemmer.stem();
                queryTermsStemmed.add(stemmer.toString());
            }

            queryTerms = queryTermsStemmed;
        }

        //ranking method
        Ranker ranker = new Ranker();
        List<String> rankedDocs = new ArrayList<>();
        for (int i = 0; i < rankedDocs.size(); i++)
            rankedDocs.addAll(ranker.getRankedDocs(rankedDocs.get(i)));
        return rankedDocs;
    }

    /**
     * searches for multiple queries one after the other and returns relevant docs
     *
     * @param queries list of queries
     * @param isStem  if to use stem or not
     * @return hash map of query and his relevant docs
     */
    public HashMap<String, List<String>> search(List<String> queries, boolean isStem) {
        HashMap<String, List<String>> queriesResults = new HashMap<>();

        //send each query to search function
        for(int i = 0; i < queries.size(); i++)
            queriesResults.put(queries.get(i), search(queries.get(i), isStem));

        return queriesResults;
    }

    /**
     * find 5 most important sentences in doc
     * @param docNo doc number
     * @param path path to docs file
     * @return list of 5 most important sentences in doc
     */
    public List<String> find5MostImportantSentences(String docNo, String path) {
        List<Pair<String, Double>> sumTfPerSent = new ArrayList<>();
        String fileName = new ReadFile().getDoc(docNo, path).getFile();
        File file;
        file = new File(fileName);
        //get list of all sentences in doc
        List<String> sentences = new ReadFile().getListOfSentencesInFile(file, docNo);

        //parse per sentence
        Parser parser = new Parser(stopWords);
        for (int i = 0; i < sentences.size(); i++) {
            List<String> terms = parser.parse(sentences.get(i).toCharArray());

            //sum tf of all the term in the sentence
            double sumTf = 0;
            for (int j = 0; j < terms.size(); j++)
                sumTf += getTf(terms.get(j), docNo);
            sumTfPerSent.add(new Pair<>(sentences.get(i), sumTf));
        }
        //sort list according to sumTf
        Collections.sort(sumTfPerSent, new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                if (o1.getValue() < o2.getValue())
                    return -1;
                return 1;
            }
        });

        //add 5 sentences with biggest sumTf to list
        List<String> mostImportantSentences = new ArrayList<>();
        for(int i = 0; i < 5; i++)
            mostImportantSentences.add(sumTfPerSent.get(i).getKey());

        return mostImportantSentences;

    }

    private double getTf(String term, String doc){
        //TODO: implement
        return 0;
    }





}
