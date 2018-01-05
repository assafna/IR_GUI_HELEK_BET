package main;

import javafx.util.Pair;

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
    public HashMap<String, List<String>> search(List<String> queries, boolean isStem, String path) {
        HashMap<String, List<String>> queriesResults = new HashMap<>();

        //send each query to search function
        for (int i = 0; i < queries.size(); i++)
            queriesResults.put(queries.get(i), search(queries.get(i), isStem, path));

        return queriesResults;
    }


    /**
     * find 5 most important sentences in doc
     *
     * @param docNo doc number
     * @param path  path to docs file
     * @return list of 50 most important sentences in doc
     */
    public ArrayList<String> find5MostImportantSentences(String docNo, String path) {
        List<Pair<String, Double>> sumTfPerSent = new ArrayList<>();
        Indexer indexer = new Indexer();
        DocNameHash docNameHash = new DocNameHash();
        HashMap<String, String> docsDictionary = indexer.getDocsDictionary();
        String docHash = docNameHash.getHashFromDocNo(docNo);
        String fileName = new Doc(docHash, docsDictionary.get(docHash)).getFile();
        //get list of all sentences in doc
        List<String> sentences = new ReadFile().getListOfSentencesInFile(fileName, path + "\\" + docNo);

        //parse per sentence
        Parser parser = new Parser(stopWords);
        for (int i = 0; i < sentences.size(); i++) {
            List<String> terms = parser.parse(sentences.get(i).toCharArray());

            //sum tf of all the term in the sentence
            double sumTf = 0;
            for (int j = 0; j < terms.size(); j++)
                // sumTf += getTf(terms.get(j), docNo);
                sumTfPerSent.add(new Pair<>(sentences.get(i), sumTf));
        }
        //sort list according to sumTf
        sumTfPerSent.sort((o1, o2) -> {
            if (o1.getValue() < o2.getValue())
                return -1;
            return 1;
        });

        //add 5 sentences with biggest sumTf to list
        ArrayList<String> mostImportantSentences = new ArrayList<>();
        for (int i = 0; i < 5; i++)
            mostImportantSentences.add(sumTfPerSent.get(i).getKey());

        return mostImportantSentences;

    }


}
