package main;

import javafx.util.Pair;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.*;


/**
 * Created by USER on 12/30/2017.
 */
public class Ranker {
    final long MAX_DAYS_BETWEEN_DAYS = 373928;

    /**
     * get list of ranked list according to query
     *
     * @param queryTerms
     * @return
     */
    public ArrayList<Pair<String, Double>> getRankedDocs(ArrayList<String> queryTerms, String path) {
        Indexer indexer = new Indexer();

        //for each doc, have an hash map of terms, and for each term have index of first occurrence, tf and bm25
        HashMap<String, HashMap<String, Pair<Double, Pair<Double, Double>>>> termsDetailsPerDoc = new HashMap<>();
        HashMap<String, Pair<Double, String>> docsWeightAndDate; //weight and date of each doc
        HashMap<String, Pair<Term, Double>> termsObjects = new HashMap<>(); //object and bm of each term
        ArrayList<String> docs = new ArrayList<>(); //list of docs names

        //for each term in query, get all docs
        int queryTermsSize = queryTerms.size();
        for (int i = 0; i < queryTermsSize; i++) {
            String queryTerm = queryTerms.get(i);
            if(!indexer.getFinalTermsDictionary().containsKey(queryTerm))
                continue;
            Term term = new Term(indexer.getFinalTermsDictionary().get(queryTerm));

            //get all docs for term
            ArrayList<String> docsForTerm = getDocsForTerm(queryTerm, term, indexer, path);

            //calculate idf for term
            double idf = getIdfForBM(docsForTerm.size(), indexer.getDocsCounter());
            termsObjects.put(queryTerm, new Pair(term, idf));

            //for each doc, add to hash
            int docsForTermSize = docsForTerm.size();
            for (int j = 0; j < docsForTermSize; j++) {
                HashMap<String, Pair<Double, Pair<Double, Double>>> terms;
                //check if hash already exists in hash map
                TermInDocCache termInDocCache = new TermInDocCache(docsForTerm.get(j));
                if (termsDetailsPerDoc.containsKey(termInDocCache.getDocName()))
                    terms = termsDetailsPerDoc.get(termInDocCache.getDocName());
                    //first time this doc is present
                else {
                    terms = new HashMap<>();
                    docs.add(termInDocCache.getDocName());
                }
                //add term
                double bm = calculateBM25PerTerm(termInDocCache, termsObjects.get(queryTerm).getKey().getIdf(), indexer);
                terms.put(queryTerm, new Pair<>(termInDocCache.getIndexOfFirstOccurrence(), new Pair(termInDocCache.getTf(), bm)));
                termsDetailsPerDoc.put(termInDocCache.getDocName(), terms);


            }
        }

        //get date and wight per doc
        docsWeightAndDate = getDocsWightAndDate(docs, indexer);
        //hash map for ratings
        ArrayList<Pair<String, Double>> docsRank = rankDocs(docsWeightAndDate, termsDetailsPerDoc, termsObjects);
        return docsRank;

    }

    /**
     * calculate idf according to BM25
     * @param numOfDocsForTerm number of docs for term
     * @param numOfDocsInCorpus total number of docs in corpus
     * @return idf for term
     */
    private double getIdfForBM(int numOfDocsForTerm, int numOfDocsInCorpus){
        double mone = numOfDocsInCorpus - numOfDocsForTerm + 0.5;
        double mechane = numOfDocsForTerm + 0.5;
        return Math.log(mone / mechane) / Math.log(2);

    }

    /**
     * calculate BM25 for one term in doc
     * @param term term
     * @param idf idf of the term
     * @param indexer indexer
     * @return BM25 of term
     */
    private double calculateBM25PerTerm(TermInDocCache term, double idf, Indexer indexer){

        double k = 1.6, b = 0.6;
        String docName = term.getDocName();
        Doc doc = new Doc(docName, indexer.getDocsDictionary().get(docName));
        int docLength = doc.getLength();
        double mone = term.getNumOfOccurrencesInDoc() * (k + 1);
        double mechane = term.getNumOfOccurrencesInDoc() + (k *(1 - b + (b * docLength / indexer.getAvgLengthOfDocs())));
        return  idf * mone / mechane;

    }

    /**
     * get all relevant docs (with details per doc) form cache and posting file for term
     *
     * @param queryTerm term from the query
     * @param term      terms object
     * @param indexer   indexer object
     * @param path      for posting files
     * @return list of all relevant docs for the term
     */
    private ArrayList<String> getDocsForTerm(String queryTerm, Term term, Indexer indexer, String path) {

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
                    docsForTerm.addAll(readFile.getTermDocsFromPosting(indexer, queryTerm, path));
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
                docsForTerm.addAll(readFile.getTermDocsFromPosting(indexer, queryTerm, path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return docsForTerm;
    }

    /**
     * return hash map the squared wight sum and date per doc
     *
     * @param docs    list of docs names
     * @param indexer object of indexer
     * @return hash map od docs details
     */
    private HashMap<String, Pair<Double, String>> getDocsWightAndDate(ArrayList<String> docs, Indexer indexer) {
        HashMap<String, Pair<Double, String>> docsDetails = new HashMap<>();

        int docsSize = docs.size();
        for (int i = 0; i < docsSize; i++) {
            HashMap<String, String> docsDictionary = indexer.getDocsDictionary();
            String docCode = docs.get(i);
            String docString = docsDictionary.get(docCode);
            Doc doc = new Doc(docCode, docString);
            docsDetails.put(docs.get(i), new Pair<>(doc.getSquaredWightSum(), doc.getDate()));
        }

        return docsDetails;
    }

    /**
     * rank docs according to tf-idf, doc date and term first index in doc
     *
     * @param docsDetails        hash map of doc details
     * @param termsDetailsPerDoc hash map of terms details per doc
     * @param termsObject        hash map of terms objects
     * @return list of ranked docs
     */
    private ArrayList<Pair<String, Double>> rankDocs(HashMap<String, Pair<Double, String>> docsDetails, HashMap<String, HashMap<String, Pair<Double, Pair<Double, Double>>>> termsDetailsPerDoc, HashMap<String, Pair<Term, Double>> termsObject) {
        ArrayList<Pair<String, Double>> rankedDocs = new ArrayList<>();

        //for each doc
        for (String doc : termsDetailsPerDoc.keySet()) {

            HashMap<String, Pair<Double, Pair<Double, Double>>> terms = termsDetailsPerDoc.get(doc);
            double sumWeightForDoc = 0;
            double sumBm = 0;

            //for each term in doc
            for (String term : terms.keySet()) {
                Pair<Double, Pair<Double,Double>> termPair = terms.get(term);// normalized index, tf
                //calculate sum wight of query terms
                double idf = termsObject.get(term).getKey().getIdf();
                double tf = termPair.getValue().getKey();
                sumBm += termPair.getValue().getValue();

                sumWeightForDoc += (tf * idf)*(1-termPair.getKey()) /*/ termPair.getKey()*/;
            }
            double denominator = Math.sqrt(docsDetails.get(doc).getKey())/* * getNormalizedDate(docsDetails.get(doc).getValue())*/;
            //double rank = (sumWeightForDoc / denominator) /*+ sumBm*/;
            double cosSin = sumWeightForDoc / denominator;
            double rank =  (sumBm * 0.3) + cosSin;
/*
            //find distance between query terms
            double sumDistanceBetweenTerms = 0;
            if(terms.size() > 1) {
                List<String> queryTerms = new ArrayList<>();
                queryTerms.addAll(terms.keySet());
                for (int i = 0; i < queryTerms.size() - 1; i++) {
                    for (int j = i + 1; j < queryTerms.size(); j++) {
                        sumDistanceBetweenTerms += Math.abs(terms.get(queryTerms.get(i)).getKey() - terms.get(queryTerms.get(j)).getKey());
                    }
                }
                rank += (sumDistanceBetweenTerms/choose(queryTerms.size(), 2));
            }
            //double rank = sumBm;
*/
            rankedDocs.add(new Pair(doc, rank));
        }

        //sort docs according to weight
        rankedDocs.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        //and finally, return
        return rankedDocs;

    }

    /**
     * implement n choose k
     * @param total total size of group
     * @param choose number to choose
     * @return n choose k
     */
    public static long choose(long total, long choose){
        if(total < choose)
            return 0;
        if(choose == 0 || choose == total)
            return 1;
        return choose(total-1,choose-1)+choose(total-1,choose);
    }

    /**
     * get normalize date
     * @param date original date
     * @return normalize date
     */
    public double getNormalizedDate(String date) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date today = new Date();
            if(date.equals("-"))
                return 0.5;
            Date docDate = dateFormat.parse(date);
            long days = ChronoUnit.DAYS.between(docDate.toInstant(), today.toInstant());
            return (double) days/MAX_DAYS_BETWEEN_DAYS;
        } catch (ParseException e) {
            return 0.5;
        }
    }


}

