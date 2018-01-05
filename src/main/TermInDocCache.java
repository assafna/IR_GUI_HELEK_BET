package main;

public class TermInDocCache implements Comparable<TermInDocCache> {

    private String docName;
    private int numOfOccurrencesInDoc;
    private double indexOfFirstOccurrence;
    private double tf;


    public TermInDocCache(String docName, int numOfOccurrencesInDoc, double indexOfFirstOccurrence, double tf) {
        this.docName = docName;
        this.numOfOccurrencesInDoc = numOfOccurrencesInDoc;
        this.indexOfFirstOccurrence = indexOfFirstOccurrence;
        this.tf = tf;
    }

    public TermInDocCache(String cacheString) {
        String[] splitString = cacheString.split("\t");
        this.docName = splitString[0];
        this.numOfOccurrencesInDoc = Integer.parseInt(splitString[1]);
        this.indexOfFirstOccurrence = Double.parseDouble(splitString[2]);
        this.tf = Double.parseDouble(splitString[3]);
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

    public double getIndexOfFirstOccurrence() {
        return indexOfFirstOccurrence;
    }

    public void setIndexOfFirstOccurrence(double indexOfFirstOccurrence) {
        this.indexOfFirstOccurrence = indexOfFirstOccurrence;
    }

    public double getTf() {
        return tf;
    }

    public void setTf(double tf) {
        this.tf = tf;
    }


    @Override
    public String toString() {
        return docName + "\t" + numOfOccurrencesInDoc + "\t" + indexOfFirstOccurrence + "\t" + tf + "\t";
    }

    @Override
    public int compareTo(TermInDocCache o) {
        return Integer.compare(numOfOccurrencesInDoc, o.numOfOccurrencesInDoc);
    }
}
