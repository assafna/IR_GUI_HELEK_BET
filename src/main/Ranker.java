package main;

import javafx.util.Pair;

import java.io.*;
import java.util.*;

/**
 * Created by USER on 12/30/2017.
 */
public class Ranker {

    public ArrayList<Pair<String, Double>> getRankedDocs(ArrayList<String> queryTerms) {

        Indexer indexer = new Indexer();

        //for each doc, have an hash map of terms, and for each term have term in doc
        HashMap<String, HashMap<String, TermInDocCache>> docToTermsToDoc = new HashMap<>();
        ArrayList<String> docs = new ArrayList<>();
        HashMap<String, Term> termsObjects = new HashMap<>();

        //for each term in query, get all docs
        int queryTermsSize = queryTerms.size();
        for (int i = 0; i < queryTermsSize; i++) {
            //current term
            String queryTerm = queryTerms.get(i);
            termsObjects.put(queryTerm, new Term(indexer.getFinalTermsDictionary().get(queryTerm)));

            //get all docs
            ArrayList<Pair<String, TermInDocCache>> cacheDocsForTerm = new ArrayList<>();
            //check if term in cache
            if (termsObjects.get(queryTerm).getPointerToPostingList() == -1) {
                //get all docs relevant for this term from cache
                cacheDocsForTerm.addAll(indexer.getCache().get(queryTerm).getKey());
                //check if there are more docs in posting
                if (indexer.getCache().get(queryTerm).getValue() != -1) {
                    //get all docs relevant for this term from posting
                    ReadFile readFile = new ReadFile();
                    try {
                        cacheDocsForTerm.addAll(readFile.getTermDocsFromPosting(indexer, queryTerm));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //all docs are in posting
            else {
                //get all docs relevant for this term from posting
                ReadFile readFile = new ReadFile();
                try {
                    cacheDocsForTerm.addAll(readFile.getTermDocsFromPosting(indexer, queryTerm));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //for each doc, add to hash
            int cacheDocsForTermSize = cacheDocsForTerm.size();
            for (int j = 0; j < cacheDocsForTermSize; j++) {
                HashMap<String, TermInDocCache> terms;
                //check if hash already exists in hash map
                if (docToTermsToDoc.containsKey(cacheDocsForTerm.get(j).getKey()))
                    terms = docToTermsToDoc.get(cacheDocsForTerm.get(j).getKey());
                //first time this doc is present
                else
                    terms = new HashMap<>();
                //add term
                terms.put(queryTerm, cacheDocsForTerm.get(j).getValue());
                docToTermsToDoc.put(cacheDocsForTerm.get(j).getKey(), terms);

                //add doc to array
                if (!docs.contains(cacheDocsForTerm.get(j).getKey()))
                    docs.add(cacheDocsForTerm.get(j).getKey());
            }
        }

        //hash map for ratings
        ArrayList<Pair<String, Double>> docsRank = new ArrayList<>();

        //calculate for each doc in hash
        int docsSize = docs.size();
        for (int i = 0; i < docsSize; i++){
            //define
            double numerator = 0;
            double denominator = 0;

            //get all terms
            HashMap<String, TermInDocCache> terms = docToTermsToDoc.get(docs.get(i));

            //for each term in this doc (that is also in the query)
            for (int j = 0; j < queryTermsSize; j++){
                //if contains the term
                TermInDocCache termInDocCache;
                if (terms.containsKey(queryTerms.get(j))) {
                    //get term in doc
                    termInDocCache = terms.get(queryTerms.get(j));
                    //calculate wij
                    double weightTermInDoc = termInDocCache.tf * termsObjects.get(queryTerms.get(j)).getIdf();
                    //add
                    numerator += weightTermInDoc;
                    denominator += Math.pow(weightTermInDoc, 2) * termInDocCache.indexOfFirstOccurrence; //NEED TO ADD DATE
                }
            }

            //sqrt denominator
            denominator = Math.sqrt(denominator);

            //rank
            double docRank = numerator / denominator;

            //add doc rank
            docsRank.add(new Pair<>(docs.get(i), docRank));
        }

        //sort
        docsRank.sort(Comparator.comparing(Pair::getValue));

        //and finally, return
        return docsRank;
    }

    //private int getAmountOfDaysBetweenNowAnd(String date){

    //}
}
