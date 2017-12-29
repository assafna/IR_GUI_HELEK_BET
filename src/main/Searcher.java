package main;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by USER on 12/29/2017.
 */
public class Searcher {

    public List<String> search(String query, boolean isStem){
        Parser parser = new Parser();
        List<String> queryTerms = parser.parse(query.toCharArray());

        //add ranking method
        List<String> rankedDocs = new ArrayList<>();
        return rankedDocs;
    }
}
