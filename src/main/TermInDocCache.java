package main;

public class TermInDocCache implements Comparable<TermInDocCache> {

    public int numOfOccurrencesInDoc;
    public int indexOfFirstOccurrence;
    public double tf;

    public TermInDocCache(int numOfOccurrencesInDoc, int indexOfFirstOccurrence, double tf) {
        this.numOfOccurrencesInDoc = numOfOccurrencesInDoc;
        this.indexOfFirstOccurrence = indexOfFirstOccurrence;
        this.tf = tf;
    }

    @Override
    public int compareTo(TermInDocCache o) {
        return Integer.compare(numOfOccurrencesInDoc, o.numOfOccurrencesInDoc);
    }
}
