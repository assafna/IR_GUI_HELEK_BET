package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by USER on 12/29/2017.
 */
public class Searcher {

    /**
     * searches for a query given as a string, and returns relevant docs
     *
     * @param query  query as a string
     * @param isStem if to use stemming or not
     * @return list of docs num (maximum 50)
     */
    public List<String> search(String query, boolean isStem) {
        //create parser and parse text
        Parser parser = new Parser();
        List<String> queryTerms = parser.parse(query.toCharArray());

        //stem
        if (isStem) {
            List<String> queryTermsStemmed = new ArrayList<>();
            Stemmer stemmer = new Stemmer();

            //stem each term
            for (String s : queryTerms) {
                stemmer.add(s.toCharArray(), s.length());
                stemmer.stem();
                queryTermsStemmed.add(stemmer.toString());
            }

            queryTerms = queryTermsStemmed;
        }

        //ranking method
        List<String> rankedDocs = new ArrayList<>();
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
        for (String s : queries)
            queriesResults.put(s, search(s, isStem));

        return queriesResults;
    }

}
