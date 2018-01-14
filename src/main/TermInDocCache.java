package main;

/**
 * represents a term in doc for the cache
 */
public class TermInDocCache implements Comparable<TermInDocCache> {

    private String docName;
    private int numOfOccurrencesInDoc;
    private int indexOfFirstOccurrence;


    public TermInDocCache(String docName, int numOfOccurrencesInDoc, int indexOfFirstOccurrence) {
        this.docName = docName;
        this.numOfOccurrencesInDoc = numOfOccurrencesInDoc;
        this.indexOfFirstOccurrence = indexOfFirstOccurrence;
    }

    public TermInDocCache(String cacheString) {
        String[] splitString = cacheString.split("\t");
        this.docName = splitString[0];
        this.numOfOccurrencesInDoc = Integer.parseInt(splitString[1]);
        this.indexOfFirstOccurrence = Integer.parseInt(splitString[2]);
    }

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public int getNumOfOccurrencesInDoc() {
        return numOfOccurrencesInDoc;
    }

    public void setNumOfOccurrencesInDoc(int numOfOccurrencesInDoc) {
        this.numOfOccurrencesInDoc = numOfOccurrencesInDoc;
    }

    public int getIndexOfFirstOccurrence() {
        return indexOfFirstOccurrence;
    }

    public void setIndexOfFirstOccurrence(int indexOfFirstOccurrence) {
        this.indexOfFirstOccurrence = indexOfFirstOccurrence;
    }


    @Override
    public String toString() {
        return docName + "\t" + numOfOccurrencesInDoc + "\t" + indexOfFirstOccurrence + "\t";
    }

    @Override
    public int compareTo(TermInDocCache o) {
        return Integer.compare(numOfOccurrencesInDoc, o.numOfOccurrencesInDoc);
    }
}
