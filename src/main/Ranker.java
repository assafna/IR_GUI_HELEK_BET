package main;

import javafx.util.Pair;

import java.io.*;
import java.util.*;

/**
 * Created by USER on 12/30/2017.
 */
public class Ranker {

    /**
     * get list of ranked list according to query
     * @param queryTerms
     * @return
     */
    public ArrayList<Pair<String, Double>> getRankedDocs(ArrayList<String> queryTerms) {
        Indexer indexer = new Indexer();

        //for each doc, have an hash map of terms, and for each term have index of first occurrence anf tf
        HashMap<String, HashMap<String, Pair<Double, Double>>> termsDetailsPerDoc = new HashMap<>();
        HashMap<String, Pair<Double, String>> docsWightAndDate = new HashMap<>(); //wight and date of each doc
        HashMap<String, Term> termsObjects = new HashMap<>(); //object of each term
        ArrayList<String> docs = new ArrayList<>(); //list of docs names

        //for each term in query, get all docs
        int queryTermsSize = queryTerms.size();
        for (int i = 0; i < queryTermsSize; i++) {
            String queryTerm = queryTerms.get(i);
            termsObjects.put(queryTerm, new Term(indexer.getFinalTermsDictionary().get(queryTerm)));

            //get all docs for term
            ArrayList<String> docsForTerm = getDocsForTerm(queryTerm, termsObjects.get(queryTerm), indexer);

            //for each doc, add to hash
            int docsForTermSize = docsForTerm.size();
            for (int j = 0; j < docsForTermSize; j++) {
                HashMap<String, Pair<Double, Double>> terms;
                //check if hash already exists in hash map
                TermInDocCache termInDocCache = new TermInDocCache(docsForTerm.get(j));
                if (termsDetailsPerDoc.containsKey(termInDocCache.getDocName()))
                    terms = termsDetailsPerDoc.get(termInDocCache.getDocName());
                    //first time this doc is present
                else {
                    terms = new HashMap<>();
                    docs.add(termInDocCache.getDocName());
                    //add term
                    terms.put(queryTerm, new Pair<>(termInDocCache.getIndexOfFirstOccurrence(), termInDocCache.getTf()));
                    termsDetailsPerDoc.put(termInDocCache.getDocName(), terms);

                }
            }
        }

        //get date and wight per doc
        docsWightAndDate = getDocsWightAndDate(docs, indexer);
        //hash map for ratings
        ArrayList<Pair<String, Double>> docsRank = rankDocs(docsWightAndDate, termsDetailsPerDoc, termsObjects);
        return docsRank;


    }

    /**
     * get all relevant docs (with details per doc) form cache and posting file for term
     * @param queryTerm term from the query
     * @param term terms object
     * @param indexer indexer object
     * @return list of all relevant docs for the term
     */
    private ArrayList<String> getDocsForTerm(String queryTerm, Term term, Indexer indexer) {

        //get all docs
        ArrayList<String> docsForTerm = new ArrayList<>();
        //check if term in cache
        if (term.getPointerToPostingList() == -1) {
            //get all docs relevant for this term from cache
            docsForTerm.addAll(indexer.getCache().get(queryTerm).getKey());
            //check if there are more docs in posting
            if (indexer.getCache().get(queryTerm).getValue() != -1) {
                //get all docs relevant for this term from posting
                ReadFile readFile = new ReadFile();
                try {
                    docsForTerm.addAll(readFile.getTermDocsFromPosting(indexer, queryTerm));
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
                docsForTerm.addAll(readFile.getTermDocsFromPosting(indexer, queryTerm));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return docsForTerm;
    }

    /**
     * return hash map the squared wight sum and date per doc
     * @param docs list of docs names
     * @param indexer object of indexer
     * @return hash map od docs details
     */
    private HashMap<String, Pair<Double, String>> getDocsWightAndDate(ArrayList<String> docs, Indexer indexer){
        HashMap<String, Pair<Double, String>> docsDetails = new HashMap<>();

        int docsSize = docs.size();
        for(int i = 0; i < docsSize; i++){
            String docString = indexer.getDocsDictionary().get(docs.get(i));
            Doc doc = new Doc(docString);
            docsDetails.put(docs.get(i), new Pair<>(doc.getSquaredWightSum(), doc.getDate()));
        }

        return docsDetails;
    }

    /**
     * rank docs according to tf-idf, doc date and term first index in doc
     * @param docsDetails hash map of doc details
     * @param termsDetailsPerDoc hash map of terms details per doc
     * @param termsObject hash map of terms objects
     * @return list of ranked docs
     */
    private ArrayList<Pair<String, Double>> rankDocs(HashMap<String, Pair<Double, String>> docsDetails, HashMap<String, HashMap<String, Pair<Double, Double>>> termsDetailsPerDoc, HashMap<String, Term> termsObject){
        ArrayList<Pair<String, Double>> rankedDocs = new ArrayList<>();

        //for each doc
        for(String doc : termsDetailsPerDoc.keySet()){
            HashMap<String, Pair<Double, Double >> terms = termsDetailsPerDoc.get(doc);
            double sumWightForDoc = 0;

            //for each term in doc
            for(String term : terms.keySet()){
                Pair<Double, Double> termPair = terms.get(term);// normalized index, tf
                //calculate sum wight of query terms
                double idf = termsObject.get(term).getIdf();
                sumWightForDoc += (termPair.getValue()* idf)/* / termPair.getKey()*/;
            }
            double denominator = Math.sqrt(docsDetails.get(doc).getKey()) * getNormalizedDate(docsDetails.get(doc).getValue());
            rankedDocs.add(new Pair(doc, sumWightForDoc/denominator));
        }

        //sort docs according to wight
        rankedDocs.sort(Comparator.comparing(Pair::getValue));

        //and finally, return
        return rankedDocs;

    }

    private double getNormalizedDate(String date){
        return 1;
    }
/*
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
               // cacheDocsForTerm.addAll(indexer.getCache().get(queryTerm).getKey());
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
    */

    //private int getAmountOfDaysBetweenNowAnd(String date){

    //}


    }

