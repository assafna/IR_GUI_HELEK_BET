package main;

/**
 * Used to represent a term in doc for the cache
 */
public class TermInDocCache implements Comparable<TermInDocCache> {

    private String docName;
    private int numOfOccurrencesInDoc;
    private int firstIndexOfTermInDoc;

    TermInDocCache(String docName, int numOfOccurrencesInDoc, int firstIndexOfTermInDoc) {
        this.docName = docName;
        this.numOfOccurrencesInDoc = numOfOccurrencesInDoc;
        this.firstIndexOfTermInDoc = firstIndexOfTermInDoc;
    }


    public String getDocFile() {
        return docName;
    }

    public void setDocFile(String docFile) {
        this.docName = docFile;
    }

    public int getNumOfOccurrencesInDoc() {
        return numOfOccurrencesInDoc;
    }

    public void setNamOfOccurrencesInDoc(int numOfOccurrencesInDoc) {
        this.numOfOccurrencesInDoc = numOfOccurrencesInDoc;
    }

    public int getFirstIndexOfTermInDoc() {
        return firstIndexOfTermInDoc;
    }

    public void setFirstIndexOfTermInDoc(int firstIndexOfTermInDoc) {
        this.firstIndexOfTermInDoc = firstIndexOfTermInDoc;
    }

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public void setNumOfOccurrencesInDoc(int numOfOccurrencesInDoc) {
        this.numOfOccurrencesInDoc = numOfOccurrencesInDoc;
    }


    @Override
    public int compareTo(TermInDocCache o) {
        return o.getNumOfOccurrencesInDoc() - numOfOccurrencesInDoc;
    }
}