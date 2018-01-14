package main;

/**
 * Used to represent a term
 */
public class Term implements Comparable<Term> {

    private String term;
    private int sumTf;
    private int df;
    private double idf;
    private int pointerToPostingList;

    Term(String term, int sumTf) {
        this.term = term;
        this.sumTf = sumTf;
    }

    public Term(String term, int sumTf, int df, double idf, int pointerToPostingList) {
        this.term = term;
        this.sumTf = sumTf;
        this.df = df;
        this.idf = idf;
        this.pointerToPostingList = pointerToPostingList;
    }

    public Term(String term, int df, double idf, int pointerToPostingList) {
        this.term = term;
        this.df = df;
        this.idf = idf;
        this.pointerToPostingList = pointerToPostingList;
    }

    public Term(String termString) {
        String[] splitString = termString.split("\t");
        this.term = splitString[0];
        this.sumTf = Integer.parseInt(splitString[1]);
        if (splitString.length > 2) {
            this.df = Integer.parseInt(splitString[2]);
            this.idf = Double.parseDouble(splitString[3]);
            this.pointerToPostingList = Integer.parseInt(splitString[4]);
        }
    }

    public int getDf() {
        return df;
    }

    public void setDf(int df) {
        this.df = df;
    }

    public double getIdf() {
        return idf;
    }

    public void setIdf(double idf) {
        this.idf = idf;
    }

    public int getPointerToPostingList() {
        return pointerToPostingList;
    }

    public void setPointerToPostingList(int pointerToPostingList) {
        this.pointerToPostingList = pointerToPostingList;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public int getSumTf() {
        return sumTf;
    }

    public void setSumTf(int sumTf) {
        this.sumTf = sumTf;
    }

    @Override
    public String toString() {

        return (term + '\t') +
                sumTf + "" + '\t' +
                df + "" + '\t' +
                idf + "" + '\t' +
                pointerToPostingList + "" + '\t';

    }

    @Override
    public int compareTo(Term o) {
        return Double.compare(o.getSumTf(), sumTf);
    }
}
